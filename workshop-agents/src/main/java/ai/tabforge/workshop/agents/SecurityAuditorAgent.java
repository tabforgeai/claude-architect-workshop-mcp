package ai.tabforge.workshop.agents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anthropic.client.AnthropicClient;

import ai.tabforge.workshop.agent.AgentContext;
import ai.tabforge.workshop.agent.ProgressReporter;


/**
 * Specialist sub-agent that analyzes Jakarta EE source files for security vulnerabilities.
 * Extends {@link DefaultSubAgent} — inherits the Anthropic SDK call and JSON parsing;
 * only overrides {@code buildPrompt()} to define the security-specific system prompt.
 *
 * <p>CERTIFICATION NOTE — covers three exam domains:</p>
 * <ul>
 *   <li><b>Domain 1 — Agentic Architecture &amp; Orchestration (27%):</b>
 *       This class IS a sub-agent in the coordinator-subagent pattern.
 *       It receives an {@link AgentContext} work order from {@code OrchestratorAgent}
 *       and reports progress back via {@link ProgressReporter}.</li>
 *   <li><b>Domain 2 — Prompt Engineering &amp; Structured Output (20%):</b>
 *       {@code buildPrompt()} demonstrates role prompting ("You are a security auditor..."),
 *       constrained output ("Respond ONLY in this JSON format"), and rule ID anchoring
 *       (SEC-001..SEC-005) — all techniques tested in Domain 2.</li>
 *   <li><b>Domain 4 — Tool Design &amp; MCP Integration (18%):</b>
 *       The JSON schema defined in the system prompt IS the output contract.
 *       {@code DefaultSubAgent.analyzeFile()} enforces it by parsing the response
 *       with Jackson — if Claude deviates, parsing fails and a safe {@code AgentResult.failed()}
 *       is returned.</li>
 * </ul>
 */
public class SecurityAuditorAgent extends DefaultSubAgent {


	private static final Logger logger = LoggerFactory.getLogger(SecurityAuditorAgent.class);

	public SecurityAuditorAgent(ProgressReporter orchestrator, AnthropicClient client) {
		super(orchestrator, client);
		// TODO Auto-generated constructor stub
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
	protected String getAgentName() {
		return "SecurityAuditorAgent";
	}
	

}
