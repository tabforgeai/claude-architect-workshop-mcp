package ai.tabforge.workshop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.tabforge.workshop.model.Finding;
import ai.tabforge.workshop.orchestrator.OrchestratorAgent;
import ai.tabforge.workshop.orchestrator.ReviewSession;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

/**
 * MCP Tool: {@code get_report}
 *
 * <p>Polling tool that Claude calls repeatedly after {@code start_review} to check
 * the current state of a review session. Returns different JSON depending on status —
 * Claude reads the {@code status} field and decides what to do next based on
 * the {@code DESCRIPTION} instructions.</p>
 *
 * <p>Flow:
 * <pre><code>
 * Claude AI → calls get_report(reviewId)
 *     ↓
 * GetReportTool.execute() → reads ReviewSession from OrchestratorAgent
 *     ↓
 * RUNNING        → { status, progress }        → Claude waits and calls again
 * AWAITING_HUMAN → { status, question, ... }   → Claude presents question to developer
 * COMPLETED      → { status, report }          → Claude presents findings
 * CANCELLED      → { status, message }         → Claude informs developer
 * FAILED         → { status, message }         → Claude suggests checking logs
 * </code></pre>
 * </p>
 *
 * <p>Created by: {@code WorkshopServer.start()} at server startup.
 * Receives {@link OrchestratorAgent} via constructor injection.</p>
 *
 * <p>CERTIFICATION NOTE — covers two exam domains:
 * <ul>
 *   <li><b>Domain 4 — Tool Design &amp; MCP Integration (18%):</b>
 *       Demonstrates that a single tool can return different JSON schemas depending
 *       on state. The {@code DESCRIPTION} field teaches Claude how to interpret
 *       each possible response — this is Task Statement 4.1: "Design MCP tool
 *       interfaces with clear descriptions and input schemas."</li>
 *   <li><b>Domain 1 — Agentic Architecture &amp; Orchestration (27%):</b>
 *       This tool implements the polling side of the async pattern started by
 *       {@code StartReviewTool}. Claude polls this tool periodically — not because
 *       we programmed a loop, but because the {@code DESCRIPTION} tells it to.
 *       This is Task Statement 1.7: "Manage session state, resumption, and forking."</li>
 * </ul>
 * </p>
 */
public class GetReportTool {
	private static final Logger logger = LoggerFactory.getLogger(GetReportTool.class);

	
	  private static final String TOOL_NAME = "get_report";

	  /*
	   * INPUT_SCHEMA defines what Claude must send when calling this tool.
	   * For this tool,  Claude only needs the reviewId he got from StartReviewTool.
	   * Claude reads this schema and knows: "I have to send a reviewId". He already got it from start_review — now he's just forwarding it back.
	   */
	  private static final String INPUT_SCHEMA = """
		      {
		        "type": "object",
		        "properties": {
		          "reviewId": {
		            "type": "string",
		            "description": "The review session ID returned by start_review"
		          }
		        },
		        "required": ["reviewId"]
		      }
		      """;	  
	  private  static final String DESCRIPTION = """
		      Retrieves the current status or completed report of a review session.
		      Call this after start_review() to check progress.
		      If status is RUNNING, call again after a few seconds.
		      If status is AWAITING_HUMAN,return:
                  {
                    status: 'AWAITING_HUMAN',
                    question: <string>,
                    severity: <string>,
                    filePath: <string>,
                    lineNumber: <number>
                    }
		      If status is COMPLETED, present the findings to the developer.
		      If status is CANCELED,  inform the developer that the review  was cancelled and ask if they want to start a new one..
		      If status is FAILED, inform the developer that the review failed  due to an unexpected error, suggest checking server logs...
		      """;
	
	  private final OrchestratorAgent orchestrator;
	  private final ObjectMapper jackson = new ObjectMapper();
	  
	  public GetReportTool(OrchestratorAgent orchestrator) {
	      this.orchestrator = orchestrator;
	  }
	  
	   public McpServerFeatures.AsyncToolSpecification toolSpecification(McpJsonMapper jsonMapper){
	        McpSchema.Tool tool = McpSchema.Tool.builder()
	                .name(TOOL_NAME)
	                .description(DESCRIPTION)
	                .inputSchema(jsonMapper, INPUT_SCHEMA)
	                .build();

	            /*
	             * (exchange, request) -> {...} : lambda, i.e. handler that is called when Claude actually calls the tool
	             *    Analogy: the receptionist who receives the call and delegates to the kitchen (execute())
	             *    
	             * exchange — communication channel with the MCP client (Claude Desktop). In our case, we don't use it directly — we just read the request and return the result.
	             * request — contains arguments sent by Claude, eg: { "reviewId": "abc123..."}
	             */
	            return new McpServerFeatures.AsyncToolSpecification(tool, (exchange, request) -> {
	            	  String reviewId = (String) request.arguments().get("reviewId");
	            	  logger.info("Tool {}: reviewId={}", TOOL_NAME, reviewId);
	            	  return Mono.fromCallable(() -> execute(reviewId))
	            	      .map(text -> McpSchema.CallToolResult.builder().addTextContent(text).build());	            });		   
	   }

	   /**
	    * 
	    * @param reviewId
	    * @return execute() should return different JSON depending on the status.
	    * @throws Exception
	    */
	   private String execute(String reviewId) throws Exception {
		      ReviewSession session = orchestrator.getStatus(reviewId);

		      ObjectNode result = jackson.createObjectNode();
		      result.put("status", session.getStatus().name());

		      switch (session.getStatus()) {
		          case RUNNING -> result.put("progress", session.getProgress());
		          case AWAITING_HUMAN -> {
		              Finding f = session.getPendingEscalation().triggeringFinding();
		              result.put("question", session.getPendingEscalation().question());
		              result.put("severity", f.severity().name());
		              result.put("filePath", f.filePath());
		              result.put("lineNumber", f.lineNumber());
		          }
		          case COMPLETED -> result.set("report", jackson.valueToTree(session.getReport()));
		          case CANCELLED -> result.put("message", "Review was cancelled.");
		          case FAILED -> result.put("message", "Review failed. Check server logs.");
		      }

		      return jackson.writerWithDefaultPrettyPrinter().writeValueAsString(result);
		  }

}
