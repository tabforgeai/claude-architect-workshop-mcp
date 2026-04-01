package ai.tabforge.workshop.model;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The final aggregated output of a complete Workshop review.
 *
 * <p>Analogy: like a Maven build report that aggregates results from all
 * plugins (Checkstyle, SpotBugs, Surefire) into one summary. Each plugin
 * is a sub-agent; this class is the combined result that the developer
 * actually reads in Claude Desktop.</p>
 *
 * <p>Serialized as JSON by {@code GetReportTool} and returned to Claude,
 * which then presents it to the developer in natural language.</p>
 *
 * <p>CERTIFICATION NOTE — Prompt Engineering & Structured Output (20% of exam):
 * The JSON produced from this record is what Claude receives from the MCP tool
 * and then summarizes for the developer. Designing the output schema so that
 * Claude can reason about it effectively (clear field names, sorted findings,
 * explicit safeToMerge flag) is part of the "structured output" exam domain.</p>
 *
 * @param reviewId         unique UUID for this review session
 * @param projectPath      root path of the reviewed project
 * @param startedAt        ISO-8601 timestamp when the review was initiated
 * @param completedAt      ISO-8601 timestamp when the review finished;
 *                         null if still running or awaiting human input
 * @param status           current state of the review
 * @param overallRisk      worst severity found across all agents
 * @param safeToMerge      false if any unresolved CRITICAL findings remain
 * @param findings         all findings sorted by severity descending
 * @param agentSummaries   per-agent stats keyed by agent name
 * @param humanDecisions   all escalations and the developer's responses
 */
public record ReviewReport(
        String reviewId,
        String projectPath,
        String startedAt,
        String completedAt,
        ReviewStatus status,
        Severity overallRisk,
        boolean safeToMerge,
        List<Finding> findings,
        Map<String, AgentSummary> agentSummaries,
        List<HumanDecision> humanDecisions
) {

    /**
     * Current lifecycle state of the review session.
     *
     * <p>CERTIFICATION NOTE — Agentic Architecture & Orchestration (27% of exam):
     * These states map to the agentic loop stages in {@code OrchestratorAgent}.
     * {@code GetReportTool} returns different JSON based on which state
     * the review is in — running progress vs. final report vs. escalation request.
     * Understanding state management in long-running agentic workflows is exam material.</p>
     */
    public enum ReviewStatus {
        /** Review is currently running — agents are executing. */
        RUNNING,
        /** Review is paused — a CRITICAL finding requires human input. */
        AWAITING_HUMAN,
        /** Review completed successfully — report is final. */
        COMPLETED,
        /** Review was cancelled by the developer. */
        CANCELLED,
        /** Review failed due to an unrecoverable error. */
        FAILED
    }

    /** Returns only CRITICAL findings — used by {@code EscalationHandler}. */
    public List<Finding> criticalFindings() {
        return findings.stream()
                .filter(f -> f.severity() == Severity.CRITICAL)
                .toList();
    }

    /** Returns findings sorted by severity (CRITICAL first) then by confidence descending. */
    public List<Finding> findingsByPriority() {
        return findings.stream()
                .sorted(Comparator
                        .comparingInt((Finding f) -> f.severity().ordinal())
                        .thenComparingDouble(Finding::confidence).reversed())
                .toList();
    }

    /** Total tokens consumed across all agents in this review. */
    public int totalTokensUsed() {
        return agentSummaries.values().stream()
                .mapToInt(s -> s.inputTokens() + s.outputTokens())
                .sum();
    }

    /** Estimated total cost of this review in USD. */
    public double estimatedTotalCostUsd() {
        return agentSummaries.values().stream()
                .mapToDouble(AgentSummary::estimatedCostUsd)
                .sum();
    }
}
