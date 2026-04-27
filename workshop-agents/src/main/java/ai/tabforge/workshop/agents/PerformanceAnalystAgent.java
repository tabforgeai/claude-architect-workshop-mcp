package ai.tabforge.workshop.agents;

import com.anthropic.client.AnthropicClient;

import ai.tabforge.workshop.agent.AgentContext;
import ai.tabforge.workshop.agent.ProgressReporter;

public class PerformanceAnalystAgent extends DefaultSubAgent {

	public PerformanceAnalystAgent(ProgressReporter orchestrator, AnthropicClient client) {
		super(orchestrator, client);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected String buildPrompt(AgentContext context) {
		  return """
			      You are a performance analyst specialized in Jakarta EE applications.
			      Analyze the provided Java code for performance issues.
			      Focus on: JPA N+1 queries, missing fetch joins, EJB lifecycle misuse,
			      and unnecessary eager loading.

			      Respond ONLY in this JSON format:
			      [
			        {
			          "ruleId": "PERF-001",
			          "severity": "WARNING",
			          "lineNumber": 42,
			          "message": "N+1 query detected in loop",
			          "suggestion": "Use JOIN FETCH in JPQL query"
			        }
			      ]

			      If no issues found, respond with empty array: []

			      Rule IDs to use:
			      PERF-001: N+1 query
			      PERF-002: Missing fetch join
			      PERF-003: EJB lifecycle misuse
			      PERF-004: Unnecessary eager loading
			      """;		
	}

	@Override
	protected String getAgentName() {
		return "PerformanceAnalystAgent";
	}

}
