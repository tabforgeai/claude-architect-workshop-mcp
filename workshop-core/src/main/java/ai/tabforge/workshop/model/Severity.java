package ai.tabforge.workshop.model;

/**
 * Risk level of a finding discovered by a sub-agent.
 *
 * <p>Analogy: like Java compiler diagnostics — ERROR stops the build,
 * WARNING is reported but does not stop it, NOTE is purely advisory.
 * Workshop uses the same three-tier model but for architectural risk,
 * not syntax errors.</p>
 *
 * <p>CERTIFICATION NOTE — Context Management & Reliability (15% of exam):
 * Severity drives the human-in-the-loop escalation logic in
 * {@code EscalationHandler}. The rule: CRITICAL + confidence ≥ 0.70
 * pauses the autonomous review and surfaces a decision to the developer.
 * This is the "circuit breaker" pattern tested in the exam.</p>
 */
public enum Severity {

    /**
     * A finding that must be resolved before the code is safe to merge.
     * Triggers human-in-the-loop escalation in {@code EscalationHandler}
     * when confidence ≥ 0.70.
     *
     * <p>Examples: SQL injection via JPQL string concatenation,
     * missing {@code @RolesAllowed} on a financial endpoint,
     * {@code @TransactionAttribute(REQUIRES_NEW)} causing silent data loss.</p>
     */
    CRITICAL,

    /**
     * A finding that should be fixed but does not require immediate human decision.
     * Included in the final {@code ReviewReport} without pausing the review.
     *
     * <p>Examples: {@code @PermitAll} on a non-public operation,
     * missing input validation on a {@code @PathParam},
     * N+1 query pattern in a hot code path.</p>
     */
    WARNING,

    /**
     * An advisory note — improvement opportunity with no immediate risk.
     * Never blocks the review. Shown at the bottom of the report.
     *
     * <p>Also used as the downgraded severity when a CRITICAL or WARNING
     * finding has confidence below 0.70 — the agent is not confident enough
     * to assert a higher severity.</p>
     *
     * <p>Examples: suboptimal EJB lifecycle choice, minor layer boundary
     * inconsistency, documentation gap in a public API.</p>
     */
    INFO
}
