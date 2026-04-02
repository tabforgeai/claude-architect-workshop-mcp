package ai.tabforge.workshop.model;

/**
 * Per-agent execution statistics included in the final {@code ReviewReport}.
 *
 * <p>Analogy: like Maven's build time summary per module —
 * {@code [INFO] workshop-core ... SUCCESS [0.780 s]}.
 * Each agent gets its own summary line so the developer can see
 * which specialist took the longest and how many issues it found.</p>
 *
 * <p>CERTIFICATION NOTE — Domain 1: Agentic Architecture & Orchestration (27%):
 * {@code AgentSummary} is what the {@code AgentResultAggregator} produces for
 * each sub-agent after the review completes. It lets the orchestrator report
 * not just what was found, but how each agent performed — a production
 * multi-agent system must be observable, not just functional.</p>
 *
 * <p>CERTIFICATION NOTE — Domain 5: Context Management & Reliability (15%):
 * The {@code inputTokens} and {@code outputTokens} fields feed into
 * {@code ReviewReport.totalTokensUsed()} and {@code estimatedTotalCostUsd()}.
 * Tracking per-agent token consumption is how the system surfaces cost
 * to the developer and how future runs can be optimized.</p>
 *
 * @param agentName      name of the sub-agent (e.g. "SecurityAuditorAgent")
 * @param filesScanned   number of Java source files this agent analyzed
 * @param findingCount   total findings this agent produced (all severities)
 * @param criticalCount  number of CRITICAL findings from this agent
 * @param inputTokens    tokens consumed by prompts sent to Claude API
 * @param outputTokens   tokens consumed by Claude's responses
 * @param durationMs     total wall-clock time for this agent's work
 * @param retryCount     number of API retries required (0 = no retries)
 */
public record AgentSummary(
        String agentName,
        int filesScanned,
        int findingCount,
        int criticalCount,
        int inputTokens,
        int outputTokens,
        long durationMs,
        int retryCount
) {

    /** Estimated USD cost using Claude Sonnet pricing ($3/M input, $15/M output). */
    public double estimatedCostUsd() {
        return (inputTokens / 1_000_000.0 * 3.0) + (outputTokens / 1_000_000.0 * 15.0);
    }
}
