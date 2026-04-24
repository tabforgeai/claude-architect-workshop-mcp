package ai.tabforge.workshop.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.tabforge.workshop.model.AgentResult;
import ai.tabforge.workshop.model.Finding;



public abstract class SubAgent {
	private static final Logger logger = LoggerFactory.getLogger(SubAgent.class);

    private ProgressReporter orchestrator; // OchestratorAgent implements this interface

	protected SubAgent(ProgressReporter orchestrator) {
	      this.orchestrator = orchestrator;
	  }

    /**
     * 
     * @param context - created and passed to this method by {@link OrchestratorAgent#startReview()}
     * @return
     */
    public final AgentResult execute(AgentContext context) {
        // 1. pripremi se (logovanje, inicijalizacija)
    	logger.info("[{}] starting, reviewId={}, files={}",
    		      getAgentName(), context.getReviewId(), context.getFileList().size());
 		long startAt = System.currentTimeMillis();
 		// all findings from this agent's analysis pass. Needed for AgentResult returned from this method
 	    List<Finding> allFindings = new ArrayList<>();
        int totalInputTokens = 0;
        int totalOutputTokens = 0;
 		
 	   for (Path file : context.getFileList()) {
  	      // defines the role and behavior of the agent:
          String prompt = buildPrompt(context);
          // read file content:
          try {
              String fileContent = Files.readString(file);
     	      //  Claude API call:
              AgentResult result = analyzeFile(prompt, fileContent);
              allFindings.addAll(result.findings());
              totalInputTokens += result.inputTokens();
              totalOutputTokens += result.outputTokens();
              // prepare data for GetReportTool, so GetReportTool can always return current state  without polling the agent:
              orchestrator.updateProgress(context.getReviewId(), file.toString());
          } catch (IOException e) {
              logger.error("[{}] failed to read file={}", getAgentName(), file, e);
              return AgentResult.failed(getAgentName(), e.getMessage(), 0); // retryCount = 0 means agent did not tried at all
          }
 	  }
        // make and return AgentResult:
 	  long durationMs = System.currentTimeMillis() - startAt;
 	 return new AgentResult(getAgentName(), allFindings, totalInputTokens, totalOutputTokens, durationMs, null, 0);    	
    }

    /**
     * A string that defines the role and behavior of the agent. For example for SecurityAuditorAgent: 
       <pre><code>
       "You are a security auditor specialized in Jakarta EE applications. 
        Analyze the provided Java code for security vulnerabilities. 
       Focus on: SQL injection, hardcoded secrets, missing authorization... 

       Respond ONLY in this JSON format: 
       [{ "ruleId": "SEC-001", "severity": "CRITICAL", ... }]"
       </code></pre>
       
       That string goes to the Claude API call as a system parameter.
       
     * @param context - Some agents may use this parameter, SecurityAuditorAgent ignores it.
     * @return
     */
    protected abstract String buildPrompt(AgentContext context);
    /**
     * Actual Claude API call
     * @param prompt
     * @param fileContent
     * @return
     */
    protected abstract AgentResult analyzeFile(String prompt, String fileContent);
    protected abstract String getAgentName();
}