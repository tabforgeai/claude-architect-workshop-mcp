package ai.tabforge.workshop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.tabforge.workshop.model.ReviewReport;
import ai.tabforge.workshop.orchestrator.OrchestratorAgent;
import ai.tabforge.workshop.orchestrator.ReviewSession;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

public class CancelReviewTool {
	private static final Logger logger = LoggerFactory.getLogger(CancelReviewTool.class);
	private static final String TOOL_NAME = "cancel_review";

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
                  "description": "The review session ID to cancel"
                }
              },
             "required": ["reviewId"]
             }
		      """;	
	  
	  /*
	   * Description must tell Claude three things:
	   * 
	   * when to call him,
         what to send him,
         and what to do next.
	   */
	  private  static final String DESCRIPTION = """
       Use this tool when the developer wants to stop an active review.
       Parameters:
       - reviewId: the review ID from the active session
       After calling this tool: inform the developer that the review has been cancelled.
       """;

	  private final OrchestratorAgent orchestrator;
	  private final ObjectMapper jackson = new ObjectMapper();
	  
	  public CancelReviewTool(OrchestratorAgent orchestrator) {
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
	            	  logger.info("Tool {}: reviewId={}", TOOL_NAME, reviewId );
	            	  return Mono.fromCallable(() -> execute(reviewId))
	            	      .map(text -> McpSchema.CallToolResult.builder().addTextContent(text).build());	            	
	            	  });		   
	   }

       /**
        * if review was in AWAITING_HUMAN when developer calls cancel_review directly (bypassing RespondToEscalationTool) — agent thread remains blocked on latch.await(). Need to add
        *  countDown()
        * @param reviewId
        * @return
        * @throws Exception
        */
	   private String execute(String reviewId) throws Exception {
		      ReviewSession session = orchestrator.getStatus(reviewId);
		      session.setStatus(ReviewReport.ReviewStatus.CANCELLED);

		      ObjectNode result = jackson.createObjectNode();
		      result.put("status", "CANCELLED");
		      
		      if (session.getLatch() != null) {
		          session.getLatch().countDown();
		      }
		      return jackson.writerWithDefaultPrettyPrinter().writeValueAsString(result);
		  }	   

}
