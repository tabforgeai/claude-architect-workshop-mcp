package ai.tabforge.workshop;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.tabforge.workshop.model.HumanDecision;
import ai.tabforge.workshop.orchestrator.OrchestratorAgent;
import ai.tabforge.workshop.orchestrator.ReviewSession;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

/**
 * MCP Tool: {@code respond_to_escalation}
 *
 * <p>Human-in-the-loop re-entry point. Claude calls this tool after the developer
 * has responded to an escalation question presented by {@code GetReportTool}.
 * Passes the developer's decision to {@link OrchestratorAgent#resumeAfterEscalation()}
 * which unblocks the paused agent thread and continues the review.</p>
 *
 * <p>Flow:
 * <pre><code>
 * GetReportTool returns { status: "AWAITING_HUMAN", question: "..." }
 *     ↓
 * Claude presents question to developer
 *     ↓
 * Developer responds: "Accept the fix"
 *     ↓
 * Claude → calls respond_to_escalation(reviewId, decision: "ACCEPT_FIX")
 *     ↓
 * RespondToEscalationTool → creates HumanDecision
 *                         → calls OrchestratorAgent.resumeAfterEscalation()
 *                         → agent thread unblocks, review continues
 *     ↓
 * Claude → calls GetReportTool periodically to track progress
 * </code></pre>
 * </p>
 *
 * <p>Created by: {@code WorkshopServer.start()} at server startup.
 * Receives {@link OrchestratorAgent} via constructor injection.</p>
 *
 * <p>CERTIFICATION NOTE — covers two exam domains:
 * <ul>
 *   <li><b>Domain 4 — Tool Design &amp; MCP Integration (18%):</b>
 *       The {@code DESCRIPTION} field defines all valid decision values and
 *       tells Claude exactly what to do after calling this tool — resume polling
 *       or inform the developer of cancellation. This is Task Statement 4.1:
 *       "Design MCP tool interfaces with clear descriptions and input schemas."</li>
 *   <li><b>Domain 1 — Agentic Architecture &amp; Orchestration (27%):</b>
 *       This tool implements the human-in-the-loop pattern — the moment where
 *       an autonomous agentic loop pauses and yields control to a human, then
 *       resumes based on the human's verdict. This is one of the most tested
 *       patterns in the exam. Task Statement 1.7: "Manage session state,
 *       resumption, and forking."</li>
 * </ul>
 * </p>
 */
public class RespondToEscalationTool {
	private static final Logger logger = LoggerFactory.getLogger(RespondToEscalationTool.class);
	private static final String TOOL_NAME = "respond_to_escalation";
	
	  /*
	   * What is INPUT_SCHEMA?
	   * INPUT_SCHEMA defines what Claude must send when calling this tool
	   * 
	   * in this case, we need the reviewId and developer decision
	   */

	  private static final String INPUT_SCHEMA = """
		      {
		        "type": "object",
		        "properties": {
		          "reviewId": {
		            "type": "string",
		            "description": "The review session ID returned by start_review"
		          },
		          "decision": {
		            "type": "string",
		            "enum": ["ACCEPT_FIX", "REJECT_FINDING", "OVERRIDE_CONTINUE", "CANCEL"],
		            "description": "The developer's response to the escalation question"
		          }
		        },
		        "required": ["reviewId", "decision"]
		      }
		      """;	
	  /*
	   * RespondToEscalationTool description must tell Claude three things:
	   * 
	   * when to call him,
         what to send him,
         and what to do next.
	   */
	  private  static final String DESCRIPTION = """
	  		Use this tool when the developer has responded to an escalation question. 

          Parameters: 
           - reviewId: the review ID from the active session 
           - decision: one of: 
                 ACCEPT_FIX — the developer accepts the suggested fix 
                 REJECT_FINDING — developer disagrees, finding should be ignored 
                 OVERRIDE_CONTINUE — developer acknowledges but wants review to continue as-is 
                 CANCEL — developer wants to stop the review entirely 

         After calling this tool: 
            - If decision is ACCEPT_FIX, REJECT_FINDING, or OVERRIDE_CONTINUE: 
               confirm to the developer that review is resuming, then call GetReportTool periodically to track progress. 
             - If decision is CANCEL:  inform the developer that the review has been cancelled.
		      """;
	
	  private final OrchestratorAgent orchestrator;
	  private final ObjectMapper jackson = new ObjectMapper();
	  
	  public RespondToEscalationTool(OrchestratorAgent orchestrator) {
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
	             *    
	             * exchange — communication channel with the MCP client (Claude Desktop). In our case, we don't use it directly — we just read the request and return the result.
	             * request — contains arguments sent by Claude, eg: { "reviewId": "abc123..." "decision" : ...}
	             */
	            return new McpServerFeatures.AsyncToolSpecification(tool, (exchange, request) -> {
	            	 String reviewId = (String) request.arguments().get("reviewId");
	            	  String decision = (String) request.arguments().get("decision");
	            	  logger.info("Tool {}: reviewId={}, decision={}", TOOL_NAME, reviewId, decision);
	            	  return Mono.fromCallable(() -> execute(reviewId, decision))
	            	      .map(text -> McpSchema.CallToolResult.builder().addTextContent(text).build());	            	
	            	  });		   
	   }
	   
	   private String execute(String reviewId, String decision) throws Exception {
		      ReviewSession session = orchestrator.getStatus(reviewId);
		      String findingRuleId = session.getPendingEscalation().triggeringFinding().ruleId();

		      HumanDecision humanDecision = new HumanDecision(
		          reviewId,
		          findingRuleId,
		          HumanDecision.DecisionType.valueOf(decision),
		          null,
		          Instant.now().toString()
		      );

		      orchestrator.resumeAfterEscalation(reviewId, humanDecision);

		      ObjectNode result = jackson.createObjectNode();
		      result.put("status", "OK");
		      result.put("decision", decision);
		      return jackson.writerWithDefaultPrettyPrinter().writeValueAsString(result);
		  }


	
}
