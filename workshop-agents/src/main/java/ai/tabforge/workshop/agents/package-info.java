/**
 * Concrete sub-agent implementations for the Workshop review pipeline.
 *
 * <p>Each class in this package is a specialist that calls the Anthropic Java SDK
 * with a domain-specific system prompt to analyze a specific aspect of Java code:</p>
 * <ul>
 *   <li>{@code SecurityAuditorAgent}     — finds security vulnerabilities in JAX-RS / JPA</li>
 *   <li>{@code TransactionAnalystAgent}  — finds Jakarta EE transaction boundary errors</li>
 *   <li>{@code PerformanceAnalystAgent}  — finds JPA N+1 queries and EJB lifecycle issues</li>
 *   <li>{@code ArchitectureCheckerAgent} — finds layer violations and CDI dependency issues</li>
 *   <li>{@code SelfEvaluatorAgent}       — meta-agent that evaluates other agents' output</li>
 * </ul>
 *
 * <p>CERTIFICATION NOTE — Agentic Architecture & Orchestration (27% of exam):
 * Narrow specialist agents with focused system prompts outperform a single
 * general-purpose agent on complex multi-domain analysis. Agent specialization
 * is a core pattern in production agentic systems.</p>
 *
 * <p>Implemented in Phase 1 Day 5-6 (Security) and Phase 2 Day 11-14 (all agents).</p>
 */
package ai.tabforge.workshop.agents;
