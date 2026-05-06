package ai.tabforge.workshop.orchestrator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ai.tabforge.workshop.model.AgentResult;
import ai.tabforge.workshop.model.Finding;

/**
 * Merges findings from all sub-agents into a single, deduplicated list.
 *
 * <p>Why deduplication is necessary: multiple specialist agents review
 * overlapping file sets. A security vulnerability in {@code PaymentService.java}
 * might be found by both {@code SecurityAuditorAgent} (SQL injection angle)
 * and {@code TransactionAnalystAgent} (missing rollback angle) — but if they
 * report the same line with the same ruleId, the developer should see it once,
 * not twice.</p>
 *
 * <p>Deduplication key: {@code ruleId + filePath + lineNumber}.
 * These three fields together uniquely identify a finding.
 * When two agents report the same key, the one with the higher severity
 * is kept; if severity is equal, the one with the higher confidence wins.</p>
 *
 * <p>Output order: CRITICAL → WARNING → INFO, then by filePath, then
 * by lineNumber. This matches the order a developer reads a report:
 * most urgent findings first.</p>
 *
 * <p>CERTIFICATION NOTE — Agentic Architecture &amp; Orchestration (27% of exam):
 * {@code AgentResultAggregator} implements the AGGREGATE step of the agentic loop:
 * <pre>
 *   INIT → DECOMPOSE → DISPATCH → COLLECT → EVALUATE → ESCALATE → AGGREGATE → COMPLETE
 * </pre>
 * In a multi-agent system, the orchestrator cannot hand raw per-agent output
 * directly to the user — it must merge, deduplicate, and rank before presenting.
 * This class is that merge step. The exam tests whether you know that aggregation
 * is the orchestrator's responsibility, not the sub-agent's.</p>
 */
public class AgentResultAggregator {

    /**
     * Merges all findings from every agent result into one deduplicated,
     * sorted list ready to be stored in a {@code ReviewReport}.
     *
     * <p>Steps:
     * <ol>
     *   <li>Flatten: collect every {@code Finding} from every {@code AgentResult}.</li>
     *   <li>Deduplicate: for each unique {@code (ruleId, filePath, lineNumber)},
     *       keep the finding with the highest severity; if tied, keep the
     *       highest confidence.</li>
     *   <li>Sort: CRITICAL first, then WARNING, then INFO; within a severity
     *       group, sort alphabetically by filePath then numerically by
     *       lineNumber — so the report reads top-to-bottom through the codebase.</li>
     * </ol>
     *
     * @param results one {@code AgentResult} per sub-agent that completed
     * @return deduplicated, sorted list of findings; empty list if no issues found
     */
    public List<Finding> aggregate(List<AgentResult> results) {

        // Step 1 — flatten: one stream of findings across all agents.
        // LinkedHashMap preserves insertion order during the dedup pass.
        Map<String, Finding> deduped = new LinkedHashMap<>();

        for (AgentResult result : results) {
            for (Finding finding : result.findings()) {

                // Step 2 — deduplication key: same rule + same file + same line
                String key = finding.ruleId() + "|" + finding.filePath() + "|" + finding.lineNumber();

                if (!deduped.containsKey(key)) {
                    // first time we see this finding — store it
                    deduped.put(key, finding);
                } else {
                    // duplicate — keep whichever is more severe (or more confident if tied)
                    Finding existing = deduped.get(key);
                    if (isMoreSevere(finding, existing)) {
                        deduped.put(key, finding);
                    }
                }
            }
        }

        // Step 3 — sort: CRITICAL → WARNING → INFO, then filePath, then lineNumber.
        // Severity.ordinal(): CRITICAL=0, WARNING=1, INFO=2 — ascending sort puts CRITICAL first.
        List<Finding> sorted = new ArrayList<>(deduped.values());
        sorted.sort(Comparator
                .comparingInt((Finding f) -> f.severity().ordinal())
                .thenComparing(Finding::filePath)
                .thenComparingInt(Finding::lineNumber));

        return sorted;
    }

    /**
     * Returns true if {@code candidate} should replace {@code current}.
     *
     * <p>Replacement rule:
     * <ul>
     *   <li>Higher severity always wins (CRITICAL beats WARNING beats INFO).</li>
     *   <li>If severity is equal, higher confidence wins — the more certain
     *       agent's description is kept.</li>
     * </ul>
     */
    private boolean isMoreSevere(Finding candidate, Finding current) {
        int severityCmp = Integer.compare(
                candidate.severity().ordinal(),  // lower ordinal = more severe
                current.severity().ordinal());

        if (severityCmp < 0) return true;   // candidate is more severe
        if (severityCmp > 0) return false;  // current is more severe

        // same severity — prefer higher confidence
        return candidate.confidence() > current.confidence();
    }
}
