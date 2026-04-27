package ai.tabforge.workshop.agents;

import com.anthropic.client.AnthropicClient;

import ai.tabforge.workshop.agent.AgentContext;
import ai.tabforge.workshop.agent.ProgressReporter;

public class TransactionAnalystAgent extends DefaultSubAgent {

	public TransactionAnalystAgent(ProgressReporter orchestrator, AnthropicClient client) {
		super(orchestrator, client);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected String buildPrompt(AgentContext context) {
		  return """
			      You are a transaction analyst specialized in Jakarta EE applications.
			      Analyze the provided Java code for transaction management issues.
			      Focus on: missing @Transactional, wrong transaction propagation,
			      transaction boundaries crossing remote calls, and rollback misconfiguration.

			      Respond ONLY in this JSON format:
			      [
			        {
			          "ruleId": "TXN-001",
			          "severity": "CRITICAL",
			          "lineNumber": 42,
			          "message": "Missing @Transactional on service method",
			          "suggestion": "Add @Transactional(rollbackOn = Exception.class)"
			        }
			      ]

			      If no issues found, respond with empty array: []

			      Rule IDs to use:
			      TXN-001: Missing @Transactional
			      TXN-002: Wrong transaction propagation
			      TXN-003: Transaction boundary violation
			      TXN-004: Rollback misconfiguration
			      """;		
	}

	@Override
	protected String getAgentName() {
		return "TransactionAnalystAgent";
	}

}
