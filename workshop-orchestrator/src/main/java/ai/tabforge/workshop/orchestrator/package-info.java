/**
 * Orchestration engine — the agentic loop that coordinates all sub-agents.
 *
 * <p>Classes in this package implement the complete review lifecycle:</p>
 * <pre>
 *   INIT → DECOMPOSE → DISPATCH → COLLECT → EVALUATE → ESCALATE → AGGREGATE → COMPLETE
 * </pre>
 *
 * <ul>
 *   <li>{@code OrchestratorAgent}     — drives the loop; owns session state</li>
 *   <li>{@code TaskDecomposer}        — routes files to the right specialist</li>
 *   <li>{@code ContextWindowManager}  — enforces token budgets per agent call</li>
 *   <li>{@code EscalationHandler}     — human-in-the-loop circuit breaker</li>
 *   <li>{@code AgentResultAggregator} — merges + deduplicates findings across agents</li>
 * </ul>
 *
 * <p>CERTIFICATION NOTE — Agentic Architecture & Orchestration (27% of exam):
 * {@code OrchestratorAgent} is the central exam artifact for this domain.
 * The lifecycle stages above map directly to the agentic loop patterns
 * described in the certification course.</p>
 *
 * <p>CERTIFICATION NOTE — Context Management & Reliability (15% of exam):
 * {@code ContextWindowManager} and {@code EscalationHandler} together
 * implement the two key reliability patterns: token budget management
 * and human-in-the-loop escalation.</p>
 *
 * <p>Implemented in Phase 1 Day 7-10.</p>
 */
package ai.tabforge.workshop.orchestrator;
