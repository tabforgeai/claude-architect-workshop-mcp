package ai.tabforge.workshop.model;

import java.util.List;

/**
 * What a sub-agent returns after executing its analysis pass.
 *
 * <p>Analogy: like a {@code Future<T>} result — the orchestrator submits
 * work to an agent and eventually receives this record back. It contains
 * not just the findings but also execution metadata (tokens used, time taken)
 * so the {@code ContextWindowManager} can track budget consumption across
 * all agents in the review session.</p>
 *
 * <p>Flow: {@code SubAgent.execute()} → {@code AgentResult} →
 * {@code OrchestratorAgent#escalate()} (checks for CRITICAL, pauses if needed) →
 * {@code AgentResultAggregator} (merges into {@code ReviewReport})</p>
 *
 * <p>CERTIFICATION NOTE — Agentic Architecture & Orchestration (27% of exam):
 * {@code AgentResult} is the handoff object between the agentic loop stages.
 * Every stage in the orchestrator (COLLECT → EVALUATE → ESCALATE → AGGREGATE)
 * operates on this record. Understanding what travels between stages
 * is fundamental to designing multi-agent systems.</p>
 *
 * @param agentName       name of the sub-agent that produced this result
 * @param findings        all findings from this agent's analysis pass
 * @param inputTokens     tokens consumed by the prompt sent to Claude API
 * @param outputTokens    tokens consumed by Claude's response
 * @param durationMs      wall-clock time for the API call in milliseconds
 * @param errorMessage    non-null if the agent failed after max retries;
 *                        null on success
 * @param retryCount      number of retry attempts before success or failure
 *                        (0 = first attempt succeeded)
 */
public record AgentResult(
        String agentName,
        List<Finding> findings,
        int inputTokens,
        int outputTokens,
        long durationMs,
        String errorMessage,
        int retryCount
) {

    /** Returns true if the agent completed without error. */
    public boolean isSuccess() {
        return errorMessage == null;
    }

    /**
     * Returns total token usage for budget tracking.
     *
     * <p>CERTIFICATION NOTE — Domain 5: Context Management & Reliability (15%):
     * {@code ContextWindowManager} calls this method after every agent completes
     * to track cumulative token consumption across the entire review session.
     * If the running total approaches the session budget, the manager splits
     * remaining files into smaller batches before the next agent call.</p>
     */
    public int totalTokens() {
        return inputTokens + outputTokens;
    }

    /**
     * Returns a failed result with a single ERROR finding.
     *
     * <p>Used by {@code SubAgent.execute()} after all retry attempts are
     * exhausted. Returning a result (rather than throwing) lets the
     * orchestrator continue with the remaining agents and include the
     * failure in the final report.</p>
     */
    public static AgentResult failed(String agentName, String errorMessage, int retryCount) {
        Finding errorFinding = new Finding(
            Severity.WARNING,
            agentName,
            "(none)",
            -1,
            "Agent failed after " + retryCount + " retries: " + errorMessage,
            "Check server logs for details. Re-run the review to retry.",
            1.0,
            "AGENT-ERROR"
        );
        return new AgentResult(agentName, List.of(errorFinding), 0, 0, 0, errorMessage, retryCount);
    }
}
