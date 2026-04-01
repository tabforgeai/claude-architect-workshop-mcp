package ai.tabforge.workshop.model;

/**
 * A single issue discovered by a sub-agent during code review.
 *
 * <p>Analogy: like a compiler diagnostic — a file path, a line number,
 * a severity level, and a human-readable message. A Java compiler produces
 * syntax errors; the Workshop produces architectural, security, and
 * quality findings. Same structure, different domain.</p>
 *
 * <p>Created by: each {@code SubAgent} implementation after parsing the
 * Claude API JSON response. Collected by {@code AgentResultAggregator}
 * into the final {@code ReviewReport}.</p>
 *
 * <p>CERTIFICATION NOTE — Prompt Engineering & Structured Output (20% of exam):
 * This record IS the output contract for every sub-agent API call.
 * The JSON schema enforced via {@code response_format} in each Anthropic API
 * call is derived from the fields of this record. If Claude's response does
 * not match this structure, the retry loop fires. The schema is the contract;
 * this record is its Java representation.</p>
 *
 * @param severity    CRITICAL pauses the review and escalates to the human;
 *                    WARNING is included in the report without interruption;
 *                    INFO is advisory and never blocks the review
 * @param agentName   which sub-agent discovered this (e.g. "SecurityAuditorAgent")
 * @param filePath    source file path relative to project root
 * @param lineNumber  line in the file; -1 if the finding is file-level
 * @param message     human-readable description of the problem
 * @param suggestion  concrete fix recommendation the developer can act on
 * @param confidence  0.0–1.0 confidence score; below 0.70 is auto-downgraded
 *                    from WARNING to INFO by {@code EscalationHandler}
 * @param ruleId      machine-readable identifier, e.g. "SEC-001", "TXN-003"
 */
public record Finding(
        Severity severity,
        String agentName,
        String filePath,
        int lineNumber,
        String message,
        String suggestion,
        double confidence,
        String ruleId
) {

    /**
     * Validates invariants at construction time.
     *
     * <p>Confidence must be in [0.0, 1.0]. A value outside this range
     * indicates a bug in the agent's JSON parsing logic.</p>
     */
    public Finding {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException(
                "confidence must be between 0.0 and 1.0, got: " + confidence);
        }
        if (agentName == null || agentName.isBlank()) {
            throw new IllegalArgumentException("agentName must not be blank");
        }
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId must not be blank");
        }
    }

    /**
     * Returns true if this finding is confident enough to trigger escalation.
     *
     * <p>CERTIFICATION NOTE — Context Management & Reliability (15% of exam):
     * The 0.70 threshold is the confidence calibration boundary used by
     * {@code EscalationHandler}. Findings below this threshold are
     * downgraded to INFO — the agent signals uncertainty rather than
     * asserting a risk it cannot substantiate.</p>
     */
    public boolean isBelowConfidenceThreshold() {
        return confidence < 0.70;
    }
}
