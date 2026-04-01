/**
 * Domain model for the Claude Architect Workshop MCP server.
 *
 * <p>These are pure Java records — no framework dependencies, no SDK imports.
 * They represent the data structures that flow between all layers of the system:</p>
 * <ul>
 *   <li>{@code Finding}           — a single issue discovered by a sub-agent</li>
 *   <li>{@code ReviewReport}      — the complete aggregated output of a review</li>
 *   <li>{@code ReviewScope}       — what to review (changed files vs full project)</li>
 *   <li>{@code AgentResult}       — what a sub-agent returns after calling Claude API</li>
 *   <li>{@code EscalationRequest} — sent to Claude Desktop when human input is needed</li>
 *   <li>{@code HumanDecision}     — the developer's response to an escalation</li>
 *   <li>{@code Severity}          — CRITICAL / WARNING / INFO</li>
 * </ul>
 *
 * <p>CERTIFICATION NOTE — Prompt Engineering & Structured Output (20% of exam):
 * The JSON schema enforced in every Anthropic API call is derived from
 * these record types. {@code Finding} IS the output contract that every
 * sub-agent's {@code response_format} parameter enforces.</p>
 *
 * <p>Implemented in Phase 1 Day 3-4.</p>
 */
package ai.tabforge.workshop.model;
