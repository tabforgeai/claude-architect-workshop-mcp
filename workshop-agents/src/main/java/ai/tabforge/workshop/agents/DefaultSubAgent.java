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

import ai.tabforge.workshop.agent.ProgressReporter;
import ai.tabforge.workshop.agent.SubAgent;
import ai.tabforge.workshop.model.AgentResult;
import ai.tabforge.workshop.model.Finding;
/**
 * Base class for all agents.
 * Each agent will have the same analyzeFile() method. This class exists just for that.
 */
public abstract class DefaultSubAgent extends SubAgent {
	private static final Logger logger = LoggerFactory.getLogger(DefaultSubAgent.class);

	/**
	 * AnthropicClient is the gateway to the Claude API — everything you can do with the Claude API, 
	 * you do through it
	 */
    private final AnthropicClient client;
	private final ObjectMapper objectMapper = new ObjectMapper();
	



    /**
     * 
     * @param orchestrator
     * @param client:  AnthropicClient is thread-safe — a single instance can be shared between all agents. That's why OrchestratorAgent makes 
                       one instance and injects it everywhere. There is no need to create a new client for each agent.
     */
	protected DefaultSubAgent(ProgressReporter orchestrator, AnthropicClient client) {
		super(orchestrator);
		this.client = client;
		// TODO Auto-generated constructor stub
	}

	  /**
	   * What is MessageCreateParams: 
         It is a builder that builds the HTTP request body. 
         Everything you put in the builder goes into JSON that is sent to the API: 
<pre><code>
{ 
"model": "claude-haiku-4-5-...", 
"max_tokens": 4096, 
"system": "You are a security auditor...", 
"messages": [{ "role": "user", "content": "...Java code..." }] 
}
</code></pre>
	   */
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


}
