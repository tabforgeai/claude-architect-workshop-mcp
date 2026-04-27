package ai.tabforge.workshop.agents;

import com.anthropic.client.AnthropicClient;

import ai.tabforge.workshop.agent.AgentContext;
import ai.tabforge.workshop.agent.ProgressReporter;

public class ArchitectureCheckerAgent extends DefaultSubAgent {

	public ArchitectureCheckerAgent(ProgressReporter orchestrator, AnthropicClient client) {
		super(orchestrator, client);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected String buildPrompt(AgentContext context) {
		  return """
			      You are an architecture checker specialized in Jakarta EE applications.
			      Analyze the provided Java code for layer violations and architectural issues.
			      Focus on: direct repository calls from REST resources, business logic in
			      entities, and circular dependencies between layers.

			      Respond ONLY in this JSON format:
			      [
			        {
			          "ruleId": "ARCH-001",
			          "severity": "WARNING",
			          "lineNumber": 42,
			          "message": "Direct repository call from REST resource",
			          "suggestion": "Introduce a service layer between resource and repository"
			        }
			      ]

			      If no issues found, respond with empty array: []

			      Rule IDs to use:
			      ARCH-001: Layer violation
			      ARCH-002: Business logic in entity
			      ARCH-003: Circular dependency
			      ARCH-004: Missing service layer
			      """;		
	}

	@Override
	protected String getAgentName() {
		return "ArchitectureCheckerAgent";
	}

}
