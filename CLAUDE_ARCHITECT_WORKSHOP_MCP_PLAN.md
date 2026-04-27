# Claude Architect Workshop MCP — Implementation Plan

## Project Name Rationale

"Claude Architect Workshop" — a community learning project built as hands-on
preparation for the Claude Certified Architect exam.

- **Claude** — built around and for the Claude ecosystem
- **Architect** — targets the Claude Certified Architect certification path
- **Workshop** — this is a learning environment, not a fake official tool
- **MCP** — it is an MCP server, discoverable by anyone searching the MCP ecosystem

GitHub will surface this for everyone searching "Claude Architect certification" —
and the workshop framing is honest: this is where you learn architect patterns by building them.

> Note: verify Anthropic's community project naming guidelines before publishing.
> "Workshop" suffix clearly marks this as a community/learning project.

---

## Vision

An open-source MCP server that, when called from Claude Desktop or Cursor,
internally orchestrates multiple specialized Claude sub-agents to perform
a comprehensive code quality review of a Java / Jakarta EE project.

Built as a **learning vehicle**: every class, every pattern, every design decision
maps directly to one of the five Claude Certified Architect exam domains.
You learn by building it. You pass the exam by understanding what you built.

---

## Certification Coverage — The Core Purpose

| Exam Domain | % of Exam | Covered By |
|---|---|---|
| Agentic Architecture & Orchestration | 27% | OrchestratorAgent + 4 sub-agents, lifecycle hooks, task decomposition |
| Claude Code Configuration & Workflows | 20% | CLAUDE.md setup, custom slash commands, CI/CD GitHub Action |
| Prompt Engineering & Structured Output | 20% | JSON schemas per agent, few-shot examples, retry loops |
| Tool Design & MCP Integration | 18% | MCP tool interfaces, structured error responses |
| Context Management & Reliability | 15% | File chunking, human-in-the-loop, confidence calibration |

**This single project covers 100% of exam domains through hands-on implementation.**

---

## Maven Project Coordinates

```
groupId:    ai.tabforge
artifactId: claude-architect-workshop-mcp
name:       Claude Architect Workshop MCP
version:    1.0.0-SNAPSHOT
packaging:  pom  (root — multi-module)
```

Java base package: `ai.tabforge.workshop`

---

## How Communication Works

```
Developer (Claude Desktop or Cursor)
    │
    │  "Review my changes before PR"
    ▼
Claude (AI assistant)
    │
    │  calls MCP tool: start_review(projectPath, scope)
    ▼
Claude Architect Workshop MCP Server  ◄── THIS IS WHAT WE BUILD
    │
    │  uses Anthropic Java SDK internally to orchestrate sub-agents
    ├──► SecurityAuditorAgent     ──► Claude API (sub-agent 1)
    ├──► TransactionAnalystAgent  ──► Claude API (sub-agent 2)
    ├──► PerformanceAnalystAgent  ──► Claude API (sub-agent 3)
    └──► ArchitectureCheckerAgent ──► Claude API (sub-agent 4)
    │
    │  CRITICAL found → pauses → returns EscalationRequest to Claude Desktop
    │  Developer decides → Claude relays → Workshop MCP resumes
    │
    ▼
Structured JSON report → Claude presents findings to developer
```

**Two SDKs — two different directions:**
```
MCP SDK        — Claude Desktop calls US  (we are the server)
Anthropic SDK  — WE call Claude API       (we are the client)
```

---

## Multi-Module Structure

```
claude-architect-workshop-mcp/                 ← root POM (public, Apache 2.0)
│   pom.xml
│
├── workshop-core/                             ← domain model + interfaces
│   src/main/java/ai/tabforge/workshop/
│       model/
│           Finding.java
│           ReviewReport.java
│           ReviewScope.java
│           AgentResult.java
│           EscalationRequest.java
│           HumanDecision.java
│           Severity.java
│       agent/
│           SubAgent.java                      ← abstract base for all sub-agents
│           AgentContext.java
│           LifecycleHook.java                 ← before/after hook interface
│
├── workshop-agents/                           ← sub-agent implementations
│   src/main/java/ai/tabforge/workshop/agents/
│       SecurityAuditorAgent.java
│       TransactionAnalystAgent.java
│       PerformanceAnalystAgent.java
│       ArchitectureCheckerAgent.java
│       SelfEvaluatorAgent.java
│
├── workshop-orchestrator/                     ← orchestration engine
│   src/main/java/ai/tabforge/workshop/orchestrator/
│       OrchestratorAgent.java
│       TaskDecomposer.java
│       ContextWindowManager.java
│       EscalationHandler.java
│       AgentResultAggregator.java
│
├── workshop-server/                           ← MCP server layer
│   src/main/java/ai/tabforge/workshop/
│       Main.java
│       server/
│           WorkshopServer.java
│       tools/
│           StartReviewTool.java
│           RespondToEscalationTool.java
│           GetReportTool.java
│           ListActiveReviewsTool.java
│           CancelReviewTool.java
│
└── workshop-installer/                        ← jpackage native installers
    (produces: ClaudeArchitectWorkshop.exe / .deb / .rpm)
```

---

## Root pom.xml Structure

```xml
<groupId>ai.tabforge</groupId>
<artifactId>claude-architect-workshop-mcp</artifactId>
<packaging>pom</packaging>

<modules>
  <module>workshop-core</module>
  <module>workshop-agents</module>
  <module>workshop-orchestrator</module>
  <module>workshop-server</module>
  <module>workshop-installer</module>
</modules>

<properties>
  <java.version>21</java.version>
  <mcp.sdk.version>1.0.0</mcp.sdk.version>
  <anthropic.sdk.version>0.8.0</anthropic.sdk.version>
  <jackson.version>2.19.2</jackson.version>
  <slf4j.version>2.0.13</slf4j.version>
  <logback.version>1.5.13</logback.version>
  <junit.version>5.11.3</junit.version>
</properties>

<!-- Two SDKs with different roles — both managed here -->
<dependencyManagement>
  <dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp</artifactId>                   <!-- we ARE an MCP server -->
    <version>${mcp.sdk.version}</version>
  </dependency>
  <dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>anthropic-java</artifactId>          <!-- we CALL Claude API -->
    <version>${anthropic.sdk.version}</version>
  </dependency>
</dependencyManagement>
```

**CRITICAL RULE: STDOUT is reserved exclusively for MCP protocol.
All logging must go to STDERR via Logback.**

---

## Native Installer — How Users Run the Server

Users never call `java -jar`. They get a native executable.

Claude Desktop / Cursor configuration:
```json
{
  "mcpServers": {
    "claude-architect-workshop": {
      "command": "C:\\Program Files\\ClaudeArchitectWorkshop\\ClaudeArchitectWorkshop.exe",
      "args": ["--api-key", "${ANTHROPIC_API_KEY}"]
    }
  }
}
```

The `ANTHROPIC_API_KEY` is required because the server calls the Claude API
internally (Anthropic Java SDK) in addition to being called by Claude Desktop (MCP SDK).

jpackage produces:
- `ClaudeArchitectWorkshop.exe` — Windows (bundles JRE, no Java needed)
- `claude-architect-workshop.deb` — Ubuntu/Debian
- `claude-architect-workshop.rpm` — RHEL/Fedora

---

## Domain Model (workshop-core)

### Finding.java

```java
/**
 * Represents a single issue discovered by any sub-agent during code review.
 *
 * <p>Analogy: like a compiler diagnostic — a file path, a line number,
 * a severity level, and a human-readable message. A Java compiler produces
 * syntax errors; the Workshop produces architectural, security, and
 * quality findings. Same structure, different domain.</p>
 *
 * <p>Created by: each SubAgent implementation after calling the Claude API.
 * Collected by: AgentResultAggregator into the final ReviewReport.</p>
 *
 * @param severity      CRITICAL pauses the review and escalates to the human;
 *                      WARNING is included in the report without interruption;
 *                      INFO is advisory and never blocks the review
 * @param agentName     which sub-agent discovered this ("SecurityAuditorAgent")
 * @param filePath      source file path relative to project root
 * @param lineNumber    line in the file, -1 if finding is file-level
 * @param message       human-readable description of the problem
 * @param suggestion    concrete fix recommendation the developer can act on
 * @param confidence    0.0–1.0 confidence score; below 0.70 is auto-downgraded
 *                      from WARNING to INFO by EscalationHandler
 * @param ruleId        machine-readable identifier, e.g. "SEC-001", "TXN-003"
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
) {}
```

---

### ReviewReport.java

```java
/**
 * The final aggregated output of a complete Workshop review — all findings
 * from all agents, merged, deduplicated, and ranked by severity.
 *
 * <p>Analogy: like a Maven build report that aggregates results from
 * all plugins (Checkstyle, SpotBugs, Surefire) into one summary.
 * Each plugin is a sub-agent; this class is the combined result
 * that the developer actually reads in Claude Desktop.</p>
 *
 * <p>Serialized as JSON by GetReportTool and returned to Claude,
 * which then presents it to the developer in natural language.</p>
 *
 * @param reviewId          unique UUID for this review session
 * @param projectPath       root path of the reviewed project
 * @param startedAt         ISO-8601 timestamp
 * @param completedAt       ISO-8601 timestamp; null if still running
 * @param overallRisk       worst severity found across all agents
 * @param safeToMerge       false if any unresolved CRITICAL findings remain
 * @param findings          all findings sorted by severity descending
 * @param agentSummaries    per-agent stats: files scanned, time taken, finding count
 * @param humanDecisions    escalations that occurred and the developer's responses
 */
public record ReviewReport(
    String reviewId,
    String projectPath,
    String startedAt,
    String completedAt,
    Severity overallRisk,
    boolean safeToMerge,
    List<Finding> findings,
    Map<String, AgentSummary> agentSummaries,
    List<HumanDecision> humanDecisions
) {}
```

---

### SubAgent.java

```java
/**
 * Abstract base class for all Workshop sub-agents — the contract that
 * every specialist must fulfill.
 *
 * <p>Analogy: like the Runnable interface in Java threading — each SubAgent
 * is an independent unit of work. The OrchestratorAgent is the thread pool
 * manager that decides when to start each one; SubAgent is the contract
 * every worker must satisfy. The orchestrator does not care how each agent
 * does its job, only that it returns an AgentResult.</p>
 *
 * <p>Each concrete sub-agent:
 * <ol>
 *   <li>Receives an AgentContext with the files it should examine</li>
 *   <li>Builds a prompt using getSystemPrompt() + the file content</li>
 *   <li>Calls the Anthropic Java SDK (Claude API) with response_format enforcement</li>
 *   <li>Parses the JSON response into a List of Finding objects</li>
 *   <li>Passes its output to SelfEvaluatorAgent for confidence calibration</li>
 *   <li>Returns AgentResult to the OrchestratorAgent</li>
 * </ol>
 * </p>
 *
 * @see SecurityAuditorAgent     checks security vulnerabilities and missing access control
 * @see TransactionAnalystAgent  checks Jakarta EE transaction boundary correctness
 * @see PerformanceAnalystAgent  checks JPA queries and EJB lifecycle decisions
 * @see ArchitectureCheckerAgent checks layer violations and CDI dependency issues
 * @see SelfEvaluatorAgent       meta-agent that evaluates other agents' output quality
 */
public abstract class SubAgent {

    /**
     * Returns the system prompt that defines this agent's role, rules,
     * output schema, and few-shot examples.
     *
     * <p>Analogy: like a job description — it tells Claude exactly what role
     * to play, what to look for, and what format to respond in.
     * The system prompt IS the agent's personality and expertise.
     * Different system prompt = different specialist.</p>
     *
     * @return system prompt injected into every Anthropic API call made by this agent
     */
    protected abstract String getSystemPrompt();

    /**
     * Returns the JSON schema that the agent's output must conform to.
     *
     * <p>Analogy: like a Java interface — this is the output contract.
     * The Anthropic API call uses this schema in response_format to enforce
     * structured output. If Claude's response does not match,
     * the retry loop in execute() fires automatically.</p>
     *
     * @return JSON Schema string used in the Anthropic API response_format parameter
     */
    protected abstract String getOutputSchema();

    /**
     * Executes this agent: calls the Anthropic API, parses findings,
     * triggers self-evaluation, and returns the result.
     *
     * <p>Called by: OrchestratorAgent after TaskDecomposer assigns
     * the relevant file chunks to this agent's scope.</p>
     *
     * @param context  files, project metadata, and configuration for this pass
     * @return         AgentResult with findings list, confidence scores, token usage
     * @throws AgentExecutionException if the API call fails after max retries
     */
    public abstract AgentResult execute(AgentContext context);
}
```

---

## Orchestration Layer (workshop-orchestrator)

### OrchestratorAgent.java

```java
/**
 * The central coordinator of the Workshop review — manages all sub-agents,
 * enforces workflow, handles lifecycle hooks, and decides when to escalate.
 *
 * <p>Analogy: like a project manager who receives a large contract,
 * breaks it into work packages, assigns each to a specialist,
 * monitors progress, collects reports, and escalates to the client
 * only when a decision requires human judgment. The specialists
 * (sub-agents) do the technical work; the orchestrator coordinates
 * and decides what happens next.</p>
 *
 * <p>This class is the core learning artifact for the
 * "Agentic Architecture & Orchestration" exam domain (27%).
 * Every pattern described in the certification guide is implemented here.</p>
 *
 * <p>Review lifecycle (the "agentic loop"):
 * <ol>
 *   <li>INIT:       receive ReviewScope from StartReviewTool MCP call</li>
 *   <li>DECOMPOSE:  TaskDecomposer splits project into agent-specific chunks</li>
 *   <li>DISPATCH:   for each sub-agent: fire onBeforeAgent() hook, then execute()</li>
 *   <li>COLLECT:    receive AgentResult; fire onAfterAgent() hook</li>
 *   <li>EVALUATE:   SelfEvaluatorAgent checks output quality; retry if insufficient</li>
 *   <li>ESCALATE:   EscalationHandler checks for CRITICAL — pause if found</li>
 *   <li>AGGREGATE:  AgentResultAggregator merges all findings</li>
 *   <li>COMPLETE:   store ReviewReport, mark session as done</li>
 * </ol>
 * </p>
 *
 * @see TaskDecomposer        for how the project is split into reviewable chunks
 * @see ContextWindowManager  for how token limits are respected per agent call
 * @see EscalationHandler     for the human-in-the-loop decision logic
 * @see AgentResultAggregator for how findings from all agents are merged
 */
public class OrchestratorAgent {

    /**
     * Starts a new review session asynchronously and returns immediately
     * with a reviewId. The caller polls GetReportTool for completion.
     *
     * <p>Analogy: like submitting a job to Java's ExecutorService —
     * you get a Future (reviewId) immediately; the work runs in the background.
     * The MCP caller does not block waiting for results.</p>
     *
     * @param scope   ReviewScope defining project path and review depth
     * @return        reviewId (UUID) used by all subsequent tool calls
     */
    public String startReview(ReviewScope scope) { ... }

    /**
     * Resumes a paused review after the developer has responded to an escalation.
     *
     * <p>Called by: RespondToEscalationTool when the developer makes a decision
     * in Claude Desktop about a CRITICAL finding.</p>
     *
     * @param reviewId  the paused session to resume
     * @param decision  ACCEPT_FIX / REJECT_FINDING / OVERRIDE_CONTINUE
     */
    public void resumeAfterEscalation(String reviewId, HumanDecision decision) { ... }
}
```

---

### TaskDecomposer.java

```java
/**
 * Splits a Java project into chunks that fit within a sub-agent's context window,
 * while routing the right files to the right specialist.
 *
 * <p>Analogy: like a database query planner that breaks a large table scan
 * into page reads — TaskDecomposer ensures no single sub-agent call exceeds
 * the token limit that degrades Claude's reasoning quality.
 * It also acts as a router: SecurityAuditorAgent does not need to see
 * every file — only files with HTTP endpoints or database access.</p>
 *
 * <p>Routing rules:
 * <ul>
 *   <li>SecurityAuditorAgent    → @Path, @Stateless/@Stateful with DB access, @Entity</li>
 *   <li>TransactionAnalystAgent → @Stateless, @Stateful, @Singleton, @TransactionAttribute</li>
 *   <li>PerformanceAnalystAgent → @NamedQuery, EntityManager usage, @Stateful (lifecycle)</li>
 *   <li>ArchitectureCheckerAgent→ all files, but class-level summaries only (not full bodies)</li>
 * </ul>
 * </p>
 *
 * <p>Called by: OrchestratorAgent during the DECOMPOSE phase.
 * Returns a map of agent type to list of file batches.</p>
 *
 * @param projectPath  root path of the Java project to decompose
 * @param scope        ReviewScope (changed files only via git diff, or full project)
 * @return             Map of SubAgent class → List of FileChunk batches
 */
public Map<Class<? extends SubAgent>, List<FileChunk>> decompose(
        Path projectPath, ReviewScope scope) { ... }
```

---

### ContextWindowManager.java

```java
/**
 * Monitors token usage across all sub-agent calls and enforces context limits.
 *
 * <p>Analogy: like a memory manager in an operating system — it tracks
 * how much "space" (token budget) each process (agent) is consuming,
 * prevents any one agent from taking all available capacity, and splits
 * file sets into batches when they exceed the safe limit.</p>
 *
 * <p>This class is the core learning artifact for the
 * "Context Management & Reliability" exam domain (15%).
 * Token management across multi-agent handoffs is tested on the exam.</p>
 *
 * <p>Budget breakdown per agent call (total: 100,000 tokens):
 * <ul>
 *   <li>System prompt:   ~8,000 tokens (reserved)</li>
 *   <li>Output budget:  ~12,000 tokens (reserved)</li>
 *   <li>Available for files: ~80,000 tokens</li>
 * </ul>
 * </p>
 *
 * @param maxTokensPerAgentCall   hard limit per API call (default: 100,000)
 * @param systemPromptBudget      tokens reserved for system prompt (default: 8,000)
 * @param outputBudget            tokens reserved for agent response (default: 12,000)
 */
public class ContextWindowManager {

    /**
     * Estimates token count for a set of source files.
     * Uses approximation: characters / 4 ≈ tokens (accurate within ~15%).
     *
     * <p>Analogy: like estimating reading time by word count —
     * not exact, but reliable enough for planning purposes.</p>
     *
     * @param files  list of Java source files to estimate
     * @return       estimated total token count for the combined file content
     */
    public int estimateTokens(List<Path> files) { ... }

    /**
     * Splits a file list exceeding the token budget into smaller batches.
     * Keeps files from the same package together where possible.
     *
     * @param files        original file list (may exceed budget)
     * @param tokenBudget  maximum tokens allowed per batch
     * @return             list of batches, each within the token budget
     */
    public List<List<Path>> splitIntoBatches(List<Path> files, int tokenBudget) { ... }
}
```

---

### EscalationHandler.java

```java
/**
 * Decides when to pause the autonomous review and ask the human for a decision,
 * then manages the resumption after the human responds.
 *
 * <p>Analogy: like a circuit breaker in electrical engineering —
 * when current (risk) exceeds the safe threshold, the breaker trips
 * (escalation occurs), cutting off autonomous action until a human
 * manually resets it. Normal operation resumes after the reset.</p>
 *
 * <p>This class is the core learning artifact for the
 * "human-in-the-loop workflow" pattern tested in the exam (15% domain).</p>
 *
 * <p>Escalation rules:
 * <ul>
 *   <li>CRITICAL + confidence ≥ 0.85  → always escalate, block review</li>
 *   <li>CRITICAL + confidence 0.70–0.84 → escalate with uncertainty note</li>
 *   <li>CRITICAL + confidence < 0.70   → downgrade to WARNING, no escalation</li>
 *   <li>Five or more WARNINGs in same file → cluster escalation</li>
 * </ul>
 * </p>
 *
 * @see EscalationRequest  the object returned to the MCP caller when pausing
 * @see HumanDecision      the object received when the developer responds
 */
public class EscalationHandler {

    /**
     * Evaluates agent results and decides whether human input is required.
     *
     * @param result  findings returned by a sub-agent after self-evaluation
     * @return        Optional with EscalationRequest if human input is needed;
     *                empty Optional if review can continue autonomously
     */
    public Optional<EscalationRequest> evaluate(AgentResult result) { ... }
}
```

---

## Sub-Agent Implementations (workshop-agents)

### SecurityAuditorAgent.java

```java
/**
 * Sub-agent specializing in security vulnerabilities in Java / Jakarta EE code.
 *
 * <p>Analogy: like a penetration tester who is brought in specifically
 * to find security holes — this agent ignores code style, performance,
 * and architecture entirely. It has one job: find ways the application
 * could be exploited or misused. Narrow focus = higher accuracy.</p>
 *
 * <p>System prompt instructs Claude to look for:
 * <ul>
 *   <li>Missing @RolesAllowed on JAX-RS endpoints (unauthorized access)</li>
 *   <li>@PermitAll on sensitive operations (financial, admin, PII data)</li>
 *   <li>JPQL string concatenation instead of named parameters (injection)</li>
 *   <li>Hardcoded credentials or API keys in source code</li>
 *   <li>Sensitive fields (password, token, ssn) returned in REST responses</li>
 *   <li>Missing input validation on @PathParam / @QueryParam values</li>
 * </ul>
 * </p>
 *
 * <p>Receives from TaskDecomposer: only classes annotated with @Path,
 * @Stateless/@Stateful with EntityManager injection, and @Entity classes.</p>
 */
public class SecurityAuditorAgent extends SubAgent { ... }
```

---

### TransactionAnalystAgent.java

```java
/**
 * Sub-agent specializing in transaction boundary correctness in Jakarta EE.
 *
 * <p>Analogy: like a database administrator reviewing stored procedures
 * for commit/rollback correctness — this agent understands that in Jakarta EE,
 * wrong transaction boundaries cause data corruption that is extremely
 * hard to reproduce and diagnose in production.</p>
 *
 * <p>System prompt instructs Claude to look for:
 * <ul>
 *   <li>EJB calling another EJB with @TransactionAttribute(REQUIRES_NEW) —
 *       the inner transaction commits independently of the outer one</li>
 *   <li>@TransactionAttribute(NOT_SUPPORTED) on a method called from within
 *       a transaction — silently suspends the transaction without warning</li>
 *   <li>RuntimeException thrown from @Stateless without @ApplicationException —
 *       causes automatic rollback of the caller's transaction</li>
 *   <li>EntityManager operations outside a transaction boundary —
 *       changes are silently discarded with no error</li>
 * </ul>
 * </p>
 */
public class TransactionAnalystAgent extends SubAgent { ... }
```

---

### SelfEvaluatorAgent.java

```java
/**
 * A meta-agent that evaluates the output quality of other sub-agents
 * before their findings reach the EscalationHandler.
 *
 * <p>Analogy: like a senior developer reviewing a junior developer's PR —
 * this agent reads the findings produced by another sub-agent and asks:
 * Are these findings specific? Are the line numbers correct? Are confidence
 * scores realistic? Are there obvious gaps? If the evaluation fails,
 * OrchestratorAgent retries the sub-agent with a more focused prompt.</p>
 *
 * <p>This class is the core learning artifact for the
 * "self-evaluation pattern" tested on the exam (15% domain).
 * An agent that reflects on its own output before it reaches the human
 * is a key reliability pattern in production agentic systems.</p>
 *
 * <p>Called by: OrchestratorAgent after every sub-agent completes,
 * before findings are passed to EscalationHandler. Max 2 retries per agent.</p>
 *
 * @param agentResult  the sub-agent output to evaluate
 * @return             EvaluationVerdict: approved / retry-with-note,
 *                     with adjusted confidence scores if needed
 */
public class SelfEvaluatorAgent extends SubAgent { ... }
```

---

## MCP Tools (workshop-server)

### StartReviewTool.java

```java
/**
 * MCP tool that initiates a Workshop review session.
 * Primary entry point: called by Claude when the developer asks for a review.
 *
 * <p>Analogy: like pressing "Run" in an IDE — this tool kicks off
 * the entire multi-agent pipeline and returns immediately with a reviewId.
 * The actual analysis runs asynchronously in the background.</p>
 *
 * @param project_path  absolute path to the Java project root
 * @param scope         "changed_files" (git diff HEAD only) or "full_project"
 * @param focus         optional filter: "security", "transactions", "all" (default: "all")
 * @return              JSON: { reviewId, estimatedDurationSeconds, agentsDispatched,
 *                              estimatedCostUsd, tokenBudgetUsed }
 */
```

---

### RespondToEscalationTool.java

```java
/**
 * MCP tool that delivers the developer's decision back to a paused review.
 * This is the "human-in-the-loop" channel — the bridge between the
 * developer's judgment and the autonomous agent workflow.
 *
 * <p>Analogy: like clicking "Approve" or "Request Changes" on a GitHub PR —
 * the developer has been shown a critical finding and must decide before
 * automated processing can continue. This tool carries that decision
 * back into the Workshop engine, resuming the paused agentic loop.</p>
 *
 * <p>This tool is the core learning artifact for the
 * "human-in-the-loop workflow" pattern (15% exam domain).
 * The pattern: autonomous agent pauses → surfaces decision → human responds
 * → agent resumes. This is tested directly on the exam.</p>
 *
 * @param review_id  the paused review session to resume
 * @param decision   "ACCEPT_FIX" / "REJECT_FINDING" / "OVERRIDE_CONTINUE"
 * @param comment    optional developer note attached to the audit trail
 * @return           JSON: { status: "RESUMED" | "CANCELLED", nextStep }
 */
```

---

### GetReportTool.java

```java
/**
 * MCP tool that retrieves the current state or completed report of a review.
 *
 * <p>Analogy: like calling Future.get() in Java — since StartReviewTool
 * returns immediately (async), Claude calls this tool to check if
 * the review is done, still running, or waiting for human input.</p>
 *
 * @param review_id  the review session to query
 * @return           ReviewReport JSON if complete;
 *                   { status: "RUNNING", progress: "SecurityAuditor 60%" } if ongoing;
 *                   { status: "AWAITING_HUMAN", escalation: {...} } if paused
 */
```

---

## Prompt Engineering Pattern (Exam Domain 20%)

Each sub-agent call to the Anthropic API follows this exact structure:

```
1. SYSTEM PROMPT
   Role definition + explicit rules + output JSON schema + few-shot examples
   "You are a Jakarta EE security auditor. You ONLY report security findings.
    Output MUST match this JSON schema exactly: { findings: [ {...} ] }
    Example of a correct finding: { severity: 'CRITICAL', ruleId: 'SEC-001', ... }
    Example of what NOT to report: general code style issues"

2. USER MESSAGE
   "Review these Java files for security vulnerabilities: [file content]"

3. RESPONSE FORMAT ENFORCEMENT
   response_format: { type: "json_object", schema: FindingListSchema }

4. RETRY LOOP (max 3 attempts)
   If JSON does not match schema:
   "Your response did not match the required schema.
    Field 'confidence' must be a number 0.0–1.0. Please reformat."
   After 3 failures: return AgentResult with single ERROR finding.
```

---

## Context Management Strategy (Exam Domain 15%)

```
Example project: 150 Java files ≈ 112,500 tokens total

Without context management: single call FAILS (exceeds limit)

With Workshop ContextWindowManager:

SecurityAuditorAgent    → 23 endpoint/DB files     ≈  18,000 tokens ✓
TransactionAnalystAgent → 31 EJB/service files     ≈  24,000 tokens ✓
PerformanceAnalystAgent → 28 JPA/repository files  ≈  22,000 tokens ✓
ArchitectureCheckerAgent→ 150 files, summaries only ≈ 35,000 tokens ✓

Every agent stays within budget.
Full project is covered.
No finding missed due to truncation.
```

---

## GitHub & LinkedIn Release Strategy

**Principle: ship early, ship often. Each phase = one GitHub release + one LinkedIn post.**

Never wait for the whole project to be done. Each working milestone goes to GitHub
immediately, with a release tag and a changelog. Then a LinkedIn post.
Then we move on to the next phase while the post collects reactions.

```
Phase 1 done → tag v0.1.0 → GitHub release: "Foundation: MCP server calls Claude API"
                           → LinkedIn post with diagram + "what I learned" angle
                           → let it sit while building Phase 2

Phase 2 done → tag v0.2.0 → GitHub release: "Full agent suite: 4 specialists + SelfEvaluator"
                           → LinkedIn post: "How I implemented human-in-the-loop in Java"

Phase 3 done → tag v0.3.0 → GitHub release: "CLAUDE.md + CI/CD GitHub Action"
                           → LinkedIn post: "One slash command to review your Jakarta EE PR"

Phase 4 done → tag v1.0.0 → GitHub release: native installers, full README, demo video
                           → LinkedIn post: "From certification prep to production MCP server"
```

**Each LinkedIn post angle:**
- Phase 1: "I built an MCP server that orchestrates sub-agents — here's the architecture"
- Phase 2: "Human-in-the-loop in practice: when should an AI pause and ask a human?"
- Phase 3: "How CLAUDE.md turns Claude Code into a team tool"
- Phase 4: "I passed the Claude Certified Architect exam by building this"

**GitHub release notes template (each version):**
```
## What's in this release
[one paragraph — what works, what you can try]

## What I learned building this
[2-3 bullet points — the exam concept this phase teaches]

## Try it yourself
[minimal setup instructions for this milestone]

## Next milestone
[one sentence — what Phase N+1 adds]
```

---

## Development Roadmap

### Phase 1 — Foundation (2 weeks)

```
Day 1-2:  Multi-module Maven setup, both SDKs verified (MCP + Anthropic Java)
           Smoke test: can we call Claude API from Java and get a response?
Day 3-4:  Domain model: Finding, ReviewReport, AgentContext, Severity, HumanDecision
Day 5-6:  SubAgent abstract class + SecurityAuditorAgent (simplest specialist first)
           Verify: SecurityAuditorAgent finds a real vulnerability in sample code
Day 7:    ContextWindowManager + TaskDecomposer (estimation + routing + batching)
Day 8-9:  OrchestratorAgent (SecurityAuditor only, no escalation yet)
           + StartReviewTool + GetReportTool
           Milestone: full flow works end-to-end in Claude Desktop
Day 10:   EscalationHandler + RespondToEscalationTool
           Milestone: CRITICAL finding pauses review → developer responds → resumes
```

### Phase 2 — Full Agent Suite (2 weeks)

```
Day 11:   TransactionAnalystAgent + Jakarta EE-specific rules
Day 12:   PerformanceAnalystAgent
Day 13:   ArchitectureCheckerAgent
Day 14:   SelfEvaluatorAgent (meta-evaluation + retry loop — exam pattern)
Day 15:   AgentResultAggregator (merge + deduplicate across all agents)
Day 16:   LifecycleHooks (onBeforeAgent, onAfterAgent, onEscalation)
Day 17:   Parallel agent execution (all 4 run concurrently — CompletableFuture)
Day 18:   Full integration test on a real Jakarta EE project
```

### Phase 3 — Certification Study Artifacts (1 week)

```
- CLAUDE.md with Workshop-specific instructions (exam domain: Claude Code Config 20%)
- Custom slash command: /workshop-review (exam domain: Claude Code Config)
- GitHub Action: runs Workshop on every PR, posts findings as PR comments
  (exam domain: CI/CD integration)
- Each class gets a "CERTIFICATION NOTE" comment linking it to the exam domain it teaches
```

### Phase 4 — Polish and Launch

```
- Native installers: ClaudeArchitectWorkshop.exe / .deb / .rpm
- GitHub Actions CI/CD build for all platforms
- README with "This project teaches the Claude Certified Architect exam through code"
- Demo: "One sentence → security + transaction + architecture review of your Jakarta EE app"
- Publish: r/java, Claude community forums, Jakarta EE mailing list
```

---

## Cost Awareness

Each review makes multiple Anthropic API calls (one per agent per chunk).

```
Example: 150-file Jakarta EE project, full scan, 4 agents
  SecurityAuditorAgent:    1 call × ~18,000 tokens  ≈ $0.05
  TransactionAnalystAgent: 1 call × ~24,000 tokens  ≈ $0.07
  PerformanceAnalystAgent: 1 call × ~22,000 tokens  ≈ $0.06
  ArchitectureCheckerAgent: 2 calls × ~35,000 tokens ≈ $0.20
  SelfEvaluatorAgent:      4 calls × ~5,000 tokens  ≈ $0.05
  Total estimated: ~$0.43 per full review
```

Mitigations:
- Default scope is `changed_files` only (git diff) — reviews only modified code
- Token estimate shown before review starts: "Estimated cost: ~$0.08"
- Cheaper model option for PerformanceAnalyst + ArchitectureChecker
- Full project scan is always opt-in
