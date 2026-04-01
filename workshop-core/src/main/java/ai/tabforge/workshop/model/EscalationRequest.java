package ai.tabforge.workshop.model;

import java.util.List;

/**
 * Sent to Claude Desktop when a CRITICAL finding requires a human decision.
 *
 * <p>Analogy: like a circuit breaker tripping — when current (risk) exceeds
 * the safe threshold, the breaker opens (review pauses) and a human must
 * manually reset it. {@code EscalationRequest} is the message the breaker
 * sends to the electrician: "I tripped on this specific problem, here are
 * your options, please decide."</p>
 *
 * <p>Flow:
 * <ol>
 *   <li>{@code EscalationHandler} detects CRITICAL finding with confidence ≥ 0.70</li>
 *   <li>Creates {@code EscalationRequest} and returns it to {@code OrchestratorAgent}</li>
 *   <li>Orchestrator pauses the review, stores the request under {@code reviewId}</li>
 *   <li>{@code GetReportTool} returns {@code status: AWAITING_HUMAN} with this request</li>
 *   <li>Claude Desktop presents the finding to the developer</li>
 *   <li>Developer calls {@code RespondToEscalationTool} with their decision</li>
 *   <li>{@code OrchestratorAgent.resumeAfterEscalation()} is called → review continues</li>
 * </ol>
 * </p>
 *
 * <p>CERTIFICATION NOTE — Context Management & Reliability (15% of exam):
 * This class represents the "pause point" in an agentic loop — the moment
 * when the system decides it cannot continue autonomously and must involve
 * a human. The exam tests: when should an agent pause? What information
 * must it surface? How does it resume? This record answers all three.</p>
 *
 * @param reviewId          the paused review session
 * @param triggeringFinding the CRITICAL finding that caused the pause
 * @param question          the specific question the developer must answer
 * @param availableOptions  the decision types the developer can choose from
 * @param escalatedAt       ISO-8601 timestamp when the escalation was created
 * @param confidenceNote    optional explanation if confidence affects the severity
 */
public record EscalationRequest(
        String reviewId,
        Finding triggeringFinding,
        String question,
        List<HumanDecision.DecisionType> availableOptions,
        String escalatedAt,
        String confidenceNote
) {

    /**
     * Standard escalation question for a CRITICAL finding.
     * Used when confidence ≥ 0.85 (high certainty).
     */
    public static String standardQuestion(Finding finding) {
        return String.format(
            "A CRITICAL security/quality issue was found in %s (line %d). " +
            "Rule: %s. How would you like to proceed?",
            finding.filePath(), finding.lineNumber(), finding.ruleId());
    }

    /**
     * Uncertainty escalation question for a CRITICAL finding with 0.70–0.84 confidence.
     * Includes a note that the agent is not fully certain.
     */
    public static String uncertaintyQuestion(Finding finding) {
        return String.format(
            "A potential CRITICAL issue was found in %s (line %d). " +
            "Rule: %s. Confidence: %.0f%% — this may be a false positive. " +
            "How would you like to proceed?",
            finding.filePath(), finding.lineNumber(), finding.ruleId(),
            finding.confidence() * 100);
    }
}
