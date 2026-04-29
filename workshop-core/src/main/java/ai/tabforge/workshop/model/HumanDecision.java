package ai.tabforge.workshop.model;

/**
 * The developer's response to an escalation — recorded in the audit trail.
 *
 * <p>Analogy: like an approval record in a code review system —
 * when a reviewer clicks "Approve" or "Request Changes" on a GitHub PR,
 * that decision is stamped with who decided, what they decided, and when.
 * {@code HumanDecision} is the Workshop's equivalent of that approval stamp.</p>
 *
 * <p>Created by: {@code RespondToEscalationTool} when the developer responds
 * to an escalation in Claude Desktop. Stored in {@code ReviewReport.humanDecisions}
 * as a permanent audit trail of every human intervention in the review.</p>
 *
 * <p>CERTIFICATION NOTE — Domain 1: Agentic Architecture & Orchestration (27%):
 * {@code HumanDecision} is the re-entry point into the agentic loop after a pause.
 * When {@code OrchestratorAgent.resumeAfterEscalation()} receives this object,
 * it uses {@code decisionType} to determine the next stage: update the finding,
 * recalculate {@code safeToMerge}, and continue dispatching remaining agents.</p>
 *
 * <p>CERTIFICATION NOTE — Domain 5: Context Management & Reliability (15%):
 * Human-in-the-loop is the pattern where an autonomous agent pauses
 * and yields control to a human for a specific decision, then resumes.
 * {@code HumanDecision} is the object that carries the human's verdict
 * back into the agentic loop. This is one of the most tested patterns
 * in the exam — understand what triggers it and what resumes after it.</p>
 *
 * @param reviewId         the review session this decision belongs to
 * @param findingRuleId    the {@code Finding.ruleId} that triggered the escalation
 * @param decisionType     what the developer chose to do
 * @param comment          optional developer note (free text, may be null)
 * @param decidedAt        ISO-8601 timestamp of the decision
 */
public record HumanDecision(
        String reviewId,
        String findingRuleId,
        DecisionType decisionType,
        String comment,
        String decidedAt
) {

    /**
     * The three choices available to the developer when an escalation occurs.
     *
     * <p>Presented to the developer in Claude Desktop as:
     * "A CRITICAL finding was detected. How would you like to proceed?"</p>
     */
    public enum DecisionType {

        /**
         * Developer acknowledges the finding and commits to fixing it.
         * The review continues; the finding stays CRITICAL in the report.
         * {@code ReviewReport.safeToMerge} remains false until the fix is committed.
         */
        ACCEPT_FIX,

        /**
         * Developer disagrees — this is a false positive.
         * The finding is downgraded to INFO in the final report.
         * {@code ReviewReport.safeToMerge} is recalculated without this finding.
         */
        REJECT_FINDING,

        /**
         * Developer acknowledges the risk and chooses to merge anyway.
         * The finding stays CRITICAL in the report but {@code safeToMerge}
         * is set to true with a note that the risk was accepted.
         * Creates a clear audit trail: "this risk was seen and accepted."
         */
        OVERRIDE_CONTINUE,
        /**
         * Developer wants to stop the review entirely.
         * Sets ReviewSession status to CANCELLED and releases the blocked agent thread.
         */
        CANCEL
    }
}
