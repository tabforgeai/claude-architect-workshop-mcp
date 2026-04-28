package ai.tabforge.workshop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.tabforge.workshop.model.ReviewScope;
import ai.tabforge.workshop.orchestrator.OrchestratorAgent;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;



/**
 * MCP Tool: {@code start_review}
 *
 * <p>Entry point for the entire review workflow. Claude calls this tool when the developer
 * asks to review their code, e.g. "Review my changes before PR" or "Scan the whole project".
 * The tool creates a {@link ai.tabforge.workshop.orchestrator.ReviewSession}, launches
 * all sub-agents in a background thread, and immediately returns a {@code reviewId} —
 * without waiting for the review to complete.</p>
 *
 * <p>Flow:
 * <pre><code>
 * Developer: "Review my changes"
 *     ↓
 * Claude AI → calls start_review(project_path, scope)
 *     ↓
 * StartReviewTool.execute() → creates ReviewScope
 *                           → calls OrchestratorAgent.startReview()
 *                           → returns { reviewId: "abc-123", status: "RUNNING" }
 *     ↓
 * Claude AI → calls GetReportTool(reviewId) periodically to track progress
 * </code></pre>
 * </p>
 *
 * <p>Created by: {@code WorkshopServer.start()} at server startup.
 * Receives {@link OrchestratorAgent} via constructor injection.</p>
 *
 * <p>CERTIFICATION NOTE — covers two exam domains:
 * <ul>
 *   <li><b>Domain 4 — Tool Design &amp; MCP Integration (18%):</b>
 *       This class IS an MCP tool. The {@code DESCRIPTION} field is the instruction
 *       to Claude AI about when and how to call this tool. The {@code INPUT_SCHEMA}
 *       defines the JSON contract Claude must follow. Together they demonstrate
 *       Task Statement 4.1: "Design MCP tool interfaces with clear descriptions
 *       and input schemas."</li>
 *   <li><b>Domain 1 — Agentic Architecture &amp; Orchestration (27%):</b>
 *       This tool is the trigger that starts the agentic loop. It demonstrates
 *       the async fire-and-forget pattern: return immediately with a handle (reviewId),
 *       let the orchestrator run agents in the background. This is Task Statement 1.7:
 *       "Manage session state, resumption, and forking."</li>
 * </ul>
 * </p>
 */
public class StartReviewTool {
	private static final Logger logger = LoggerFactory.getLogger(StartReviewTool.class);

	
	  private static final String TOOL_NAME = "start_review";

	  /*
	   * What is INPUT_SCHEMA?
	   * INPUT_SCHEMA defines what Claude must send when calling this tool
	   */

	  private static final String INPUT_SCHEMA = """
	      {
	        "type": "object",
	        "properties": {
	          "project_path": {
	            "type": "string",
	            "description": "Absolute path to the project root"
	          },
	          "scope": {
	            "type": "string",
	            "enum": ["full_project", "changed_files"],
	            "description": "Which files to review"
	          }
	        },
	        "required": ["project_path", "scope"]
	      }
	      """;
	  
	  private  static final String DESCRIPTION =   "Start a code review for a Jakarta EE project. " +
	   "Call this when the developer asks to review their code or changes before a PR. " +
	   "Returns a reviewId — use it in all subsequent GetReportTool calls to track progress.";
	  
	  
	  private final OrchestratorAgent orchestrator;
	  private final ObjectMapper jackson = new ObjectMapper();
	  
	  public StartReviewTool(OrchestratorAgent orchestrator) {
	      this.orchestrator = orchestrator;
	  }
	  /**
          
      * Builds the MCP tool descriptor and attaches the async execution handler.
     *
     * <p>The tool descriptor contains the tool name, natural-language description (shown to the AI
     * model), and the JSON Schema for the input parameters. The execution handler receives the
     * parsed arguments and delegates to {@link #execute(String)}.</p>
     *
     * <p>Called from: start() method of the main MCP server class once,  during server initialization.</p>
     * 
     * Analogy: toolSpecification() is like registering a service at the hotel reception — you say: "We offer room service, you call us at the number 
       101, and you need to tell us the room number and what you want." The MCP server remembers that and waits for calls.
     *
     * @param jsonMapper MCP JSON mapper used to parse the {@code INPUT_SCHEMA} string into a schema object understood by the MCP framework
     *                  Its like a  translator between the Java and JSON worlds within the MCP framework. So:
     *                  INPUT_SCHEMA (String) → jsonMapper → McpSchema object understood by the MCP framework. 
     * @return a fully configured async tool specification ready to be registered with the MCP server          
          
	   * @param jsonMapper
	   * @return
	   */
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
	             * request — contains arguments sent by Claude, eg: { "project_path": "C:/myproject", "scope": "changed_files" }
	             */
	            return new McpServerFeatures.AsyncToolSpecification(tool, (exchange, request) -> {
	            	 String projectPath = (String) request.arguments().get("project_path");
	            	  String scope = (String) request.arguments().get("scope");
	            	  logger.info("Tool {}: project_path={}, scope={}", TOOL_NAME, projectPath, scope);
	            	  /*
	            	   * execute this blocking code (execute()) and wrap the result in Mono
	            	   * .map(text -> ...) — when execute() returns a String, wrap it in a CallToolResult that MCP understands.
	            	   */
	            	  return Mono.fromCallable(() -> execute(projectPath, scope))
	            	      .map(text -> McpSchema.CallToolResult.builder().addTextContent(text).build());	            	
	            });		   
	   }

	   /**
	    *  Analogy: execute() is the actual delivery of food — when a guest calls, the front desk forwards the request to the kitchen, which executes it.
	    *  
	    * @param projectPath
	    * @param scope
	    * @return
	    * @throws Exception
	    */
	   private String execute(String projectPath, String scope) throws Exception {
		      ReviewScope reviewScope = ReviewScope.ScopeType.FULL_PROJECT.name().equalsIgnoreCase(scope)
		          ? ReviewScope.fullScan(projectPath)
		          : ReviewScope.changedFiles(projectPath);

		      String reviewId = orchestrator.startReview(reviewScope);

		      ObjectNode result = jackson.createObjectNode();
		      result.put("reviewId", reviewId);
		      result.put("status", "RUNNING");
		      return jackson.writerWithDefaultPrettyPrinter().writeValueAsString(result);
	   }      
}
