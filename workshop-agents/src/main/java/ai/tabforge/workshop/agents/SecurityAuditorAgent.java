package ai.tabforge.workshop.agents;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.tabforge.workshop.agent.AgentContext;
import ai.tabforge.workshop.agent.ProgressReporter;
import ai.tabforge.workshop.agent.SubAgent;
import ai.tabforge.workshop.model.AgentResult;
import ai.tabforge.workshop.model.Finding;


public class SecurityAuditorAgent extends SubAgent {
	private static final Logger logger = LoggerFactory.getLogger(SecurityAuditorAgent.class);


    private final AnthropicClient client;
	private final ObjectMapper objectMapper = new ObjectMapper();
	/**
	 * 
	 * @param orchestrator
	 * @param client
	 */
	 public SecurityAuditorAgent(ProgressReporter orchestrator, AnthropicClient client) {
	      super(orchestrator);
	      this.client = client;
	  }

	  @Override
	  protected String buildPrompt(AgentContext context) {
	      return """
	          You are a security auditor specialized in Jakarta EE applications.
	          Analyze the provided Java code for security vulnerabilities.
	          Focus on: SQL injection, hardcoded secrets, missing authorization,
	          insecure deserialization, and improper input validation.

	          Respond ONLY in this JSON format:
	          [
	            {
	              "ruleId": "SEC-001",
	              "severity": "CRITICAL",
	              "lineNumber": 42,
	              "message": "SQL injection vulnerability detected",
	              "suggestion": "Replace string concatenation with PreparedStatement"
	            }
	          ]

	          If no issues found, respond with empty array: []

	          Rule IDs to use:
	          SEC-001: SQL injection
	          SEC-002: Hardcoded secret
	          SEC-003: Missing authorization
	          SEC-004: Insecure deserialization
	          SEC-005: Improper input validation
	          """;
	  }

	  @Override
	  protected AgentResult analyzeFile(String prompt, String fileContent) {
	      Message response = client.messages().create(
	          MessageCreateParams.builder()
	              .model(Model.CLAUDE_HAIKU_4_5_20251001)
	              .maxTokens(4096)
	              .system(prompt)
	              .addUserMessage(fileContent)
	              .build()
	      );
	      String rawJson = response.content().get(0).asText().text();
          try {	      
        	  List<Finding> findings = objectMapper.readValue(rawJson,
	    	                                    objectMapper.getTypeFactory().constructCollectionType(List.class, Finding.class)
	    	  );
              return new AgentResult(
            	      getAgentName(),
            	      findings,
            	      (int)response.usage().inputTokens(),
            	      (int)response.usage().outputTokens(),
            	      0,   // durationMs — SubAgent.execute() meri ukupno vreme
            	      null, // errorMessage — null znači uspeh
            	      0     // retryCount
            	  );
        	  
    	  } catch (JsonProcessingException e) {
    	      logger.error("[{}] failed to parse Claude response", getAgentName(), e);
    	      return AgentResult.failed(getAgentName(), e.getMessage(), 0);
    	  }
	  }	  
	  
	  
	@Override
	protected String getAgentName() {
		return "SecurityAuditorAgent";
	}
	

}
