package ai.tabforge.workshop.model;

/**
 * Defines what to review: which files and which specialist agents to run.
 *
 * <p>Passed to {@code OrchestratorAgent.startReview()} by {@code StartReviewTool}
 * after the MCP tool call arrives from Claude Desktop.</p>
 *
 * <p>Analogy: like a Maven build profile — the same project can be built
 * in different ways ({@code -P security-only}, {@code -P full-review}).
 * ReviewScope is the profile that tells the orchestrator how deep to go
 * and which specialists to involve.</p>
 *
 * <p>CERTIFICATION NOTE — Tool Design &amp; MCP Integration (18% of exam):
 * {@code ReviewScope} is constructed from the MCP tool input parameters.
 * {@code StartReviewTool} receives raw JSON from Claude, validates it,
 * and produces a {@code ReviewScope}. This is the boundary where untyped
 * MCP input becomes a typed domain object.</p>
 *
 * @param projectPath  absolute path to the Java project root
 * @param scopeType    CHANGED_FILES (git diff HEAD) or FULL_PROJECT
 * @param focus        which specialist agents to run (default: ALL)
 */
public record ReviewScope(
        String projectPath,
        ScopeType scopeType,
        FocusFilter focus
) {

    /**
     * Convenience factory — full project scan with all agents.
     * Used in tests and when the developer does not specify a scope.
     */
    public static ReviewScope fullScan(String projectPath) {
        return new ReviewScope(projectPath, ScopeType.FULL_PROJECT, FocusFilter.ALL);
    }

    /**
     * Convenience factory — only changed files (git diff HEAD), all agents.
     * Default scope for pre-PR reviews — the most common use case.
     */
    public static ReviewScope changedFiles(String projectPath) {
        return new ReviewScope(projectPath, ScopeType.CHANGED_FILES, FocusFilter.ALL);
    }

    /**
     * Which files to include in the review.
     *
     * <p>CERTIFICATION NOTE — Context Management &amp; Reliability (15% of exam):
     * CHANGED_FILES is the default scope because it minimises token usage.
     * A full project scan of 150 files costs ~$0.43; a typical PR diff
     * of 10 changed files costs ~$0.03. The token budget strategy starts
     * with the smallest scope that answers the developer's question.</p>
     */
    public enum ScopeType {
        /** Only files modified since the last git commit (git diff HEAD). */
        CHANGED_FILES,
        /** Every Java source file in the project. Opt-in; higher cost. */
        FULL_PROJECT
    }

    /**
     * Which specialist sub-agents to run.
     *
     * <p>Narrowing the focus reduces cost and speeds up the review
     * when the developer knows what they changed (e.g. only security
     * code in this PR → run SecurityAuditorAgent only).</p>
     */
    public enum FocusFilter {
        /** Run all four specialist agents. Default. */
        ALL,
        /** Run SecurityAuditorAgent only. */
        SECURITY,
        /** Run TransactionAnalystAgent only. */
        TRANSACTIONS,
        /** Run PerformanceAnalystAgent only. */
        PERFORMANCE,
        /** Run ArchitectureCheckerAgent only. */
        ARCHITECTURE
    }
}
