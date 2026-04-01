/**
 * Abstract base classes and interfaces for Workshop sub-agents.
 *
 * <p>Contains:</p>
 * <ul>
 *   <li>{@code SubAgent}      — abstract base; every specialist extends this</li>
 *   <li>{@code AgentContext}  — files + metadata passed to each agent's execute()</li>
 *   <li>{@code LifecycleHook} — onBeforeAgent / onAfterAgent / onEscalation hooks</li>
 * </ul>
 *
 * <p>CERTIFICATION NOTE — Agentic Architecture & Orchestration (27% of exam):
 * {@code SubAgent} is the contract in the agent specialization pattern.
 * The OrchestratorAgent works with {@code SubAgent} references only —
 * it does not know (and does not need to know) which specialist is running.</p>
 *
 * <p>Implemented in Phase 1 Day 5-6.</p>
 */
package ai.tabforge.workshop.agent;
