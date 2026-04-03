# Claude Architect Workshop MCP

> **A community learning project** — an open-source MCP server that prepares you
> for the [Claude Certified Architect](https://anthropic.skilljar.com/claude-certified-architect-foundations-access-request)
> exam by making you **build** the patterns, not just read about them.

![Java](https://img.shields.io/badge/Java-21-blue)
![Maven](https://img.shields.io/badge/Maven-multi--module-orange)
![MCP SDK](https://img.shields.io/badge/MCP%20SDK-1.0.0-green)
![Anthropic SDK](https://img.shields.io/badge/Anthropic%20Java%20SDK-2.18.0-blueviolet)
![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey)
![Status](https://img.shields.io/badge/Status-Phase%201%20In%20Progress-yellow)

---

## What This Is

A Java MCP server that, when registered with Claude Desktop or Cursor, orchestrates
multiple specialized Claude sub-agents to perform a comprehensive code quality review
of a Java / Jakarta EE project.

You ask Claude: *"Review my changes before I open a PR."*
Claude calls this server. The server dispatches four specialist agents to the Claude API,
each with a focused system prompt and a strict JSON output schema. If a critical issue
is found, the review pauses and Claude asks you for a decision before continuing.

**But the real purpose is educational.**

Every class, every design decision, every comment in this codebase is annotated with
a `CERTIFICATION NOTE` that explains exactly which exam domain it teaches and why.
You prepare for the Claude Certified Architect exam by building the system —
not by memorizing slides.

---

## The Two SDKs — The Core Architectural Concept

This project uses two SDKs with **opposite roles**. Understanding this inversion is
the first concept tested in the Tool Design & MCP Integration exam domain (18%):

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│   MCP SDK  (io.modelcontextprotocol.sdk:mcp)                   │
│   → Claude Desktop CALLS US. We are the SERVER.                │
│   → We register tools. We receive tool calls. We return JSON.  │
│                                                                 │
│   Anthropic Java SDK  (com.anthropic:anthropic-java)           │
│   → WE CALL the Claude API. We are the CLIENT.                 │
│   → Each sub-agent sends a prompt, receives a response.        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

The same Claude model is both the orchestrator (calling our MCP tools)
and the specialists (called by our agents via the Anthropic SDK).
This is a pattern that appears throughout production agentic systems.

---

## How It Works

```
Developer (Claude Desktop / Cursor)
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
    ├──► SecurityAuditorAgent     ──► Claude API  (finds security vulnerabilities)
    ├──► TransactionAnalystAgent  ──► Claude API  (finds Jakarta EE transaction errors)
    ├──► PerformanceAnalystAgent  ──► Claude API  (finds JPA/EJB performance issues)
    └──► ArchitectureCheckerAgent ──► Claude API  (finds layer violations)
    │
    │  CRITICAL found → pauses → returns EscalationRequest to Claude Desktop
    │  Developer decides → Claude relays → Workshop MCP resumes
    │
    ▼
Structured JSON report → Claude presents findings to developer
```

---

## Certification Coverage

This single project covers **100% of the Claude Certified Architect exam domains**
through hands-on implementation.

> Official exam guide: [Claude Certified Architect – Foundations Certification Exam Guide](https://everpath-course-content.s3-accelerate.amazonaws.com/instructor%2F8lsy243ftffjjy1cx9lm3o2bw%2Fpublic%2F1773274827%2FClaude+Certified+Architect+%E2%80%93+Foundations+Certification+Exam+Guide.pdf)

| # | Exam Domain (official name) | % of Exam | Where It Is Built in This Project |
|---|---|---|---|
| Domain 1 | Agentic Architecture & Orchestration | 27% | `OrchestratorAgent` + 4 sub-agents, lifecycle hooks, task decomposition |
| Domain 2 | Tool Design & MCP Integration | 18% | MCP tool interfaces, `StartReviewTool`, `RespondToEscalationTool`, structured error responses |
| Domain 3 | Claude Code Configuration & Workflows | 20% | `CLAUDE.md`, custom slash commands, CI/CD GitHub Action |
| Domain 4 | Prompt Engineering & Structured Output | 20% | JSON schemas per agent, few-shot examples, `response_format` enforcement, retry loops |
| Domain 5 | Context Management & Reliability | 15% | File chunking, human-in-the-loop, confidence calibration, `ContextWindowManager` |

**Every class in the codebase has a `CERTIFICATION NOTE` in its Javadoc
linking it to the exam domain it teaches.**

**Development rule:** every feature added to this project must map to at least one
of the five domains above. If a feature does not advance exam coverage, it does not
belong in this codebase.

---

## Project Structure

```
claude-architect-workshop-mcp/          ← root POM (this repo)
│
├── workshop-core/                      ← domain model + agent contract
│   ├── ai.tabforge.workshop.model/
│   │   ├── Finding.java                ← one issue found by one agent
│   │   ├── ReviewReport.java           ← complete output of a full review
│   │   ├── ReviewScope.java            ← what to review (changed files / full project)
│   │   ├── AgentResult.java            ← what one sub-agent returns
│   │   ├── AgentSummary.java           ← per-agent token + timing stats
│   │   ├── EscalationRequest.java      ← sent to human when CRITICAL found
│   │   ├── HumanDecision.java          ← developer's response to escalation
│   │   └── Severity.java               ← CRITICAL / WARNING / INFO
│   └── ai.tabforge.workshop.agent/
│       ├── SubAgent.java               ← abstract base: template method pattern
│       ├── AgentContext.java            ← files + metadata passed to execute()
│       └── LifecycleHook.java          ← onBeforeAgent / onAfterAgent / onEscalation
│
├── workshop-agents/                    ← sub-agent implementations (Phase 1-2)
│   └── ai.tabforge.workshop.agents/
│       ├── SecurityAuditorAgent.java   ← finds JAX-RS / JPA security issues
│       ├── TransactionAnalystAgent.java← finds Jakarta EE transaction errors
│       ├── PerformanceAnalystAgent.java← finds JPA N+1 / EJB lifecycle issues
│       ├── ArchitectureCheckerAgent.java ← finds layer violations
│       └── SelfEvaluatorAgent.java     ← meta-agent: evaluates other agents' output
│
├── workshop-orchestrator/              ← agentic loop engine (Phase 1)
│   └── ai.tabforge.workshop.orchestrator/
│       ├── OrchestratorAgent.java      ← drives the review lifecycle
│       ├── TaskDecomposer.java         ← routes files to the right specialist
│       ├── ContextWindowManager.java   ← enforces token budgets per API call
│       ├── EscalationHandler.java      ← human-in-the-loop circuit breaker
│       └── AgentResultAggregator.java  ← merges findings from all agents
│
├── workshop-server/                    ← MCP server layer (Phase 1)
│   └── ai.tabforge.workshop/
│       ├── Main.java                   ← entry point
│       ├── ApiSmokeTest.java           ← verifies both SDKs at startup
│       └── tools/
│           ├── StartReviewTool.java    ← MCP: initiates a review session
│           ├── GetReportTool.java      ← MCP: polls for results / final report
│           ├── RespondToEscalationTool.java ← MCP: human-in-the-loop channel
│           ├── ListActiveReviewsTool.java   ← MCP: lists running sessions
│           └── CancelReviewTool.java   ← MCP: cancels a running review
│
└── workshop-installer/                 ← jpackage native installers (Phase 4)
    └── ClaudeArchitectWorkshop.exe / .deb / .rpm
```

---

## Key Design Decisions (and What They Teach)

### 1. Specialist agents over a general agent (27% domain)

Instead of one Claude call with a giant prompt ("find all problems"),
four focused specialists run in parallel. Each has a narrow system prompt,
a domain-specific JSON schema, and receives only the files relevant to its specialty.

`SecurityAuditorAgent` never sees files that have no HTTP endpoints or database access.
`TransactionAnalystAgent` never sees files with no EJB annotations.

**Why this matters for the exam:** Narrow context = higher precision.
A specialist agent that reviews 20 targeted files outperforms a general agent
reviewing 150 files on every metric: accuracy, token cost, latency.

### 2. Human-in-the-loop as a circuit breaker (15% domain)

When a CRITICAL finding has confidence ≥ 0.70, the review **pauses**.
It does not fail. It does not silently continue. It surfaces a decision
to the developer via Claude Desktop and waits.

Three outcomes: `ACCEPT_FIX` / `REJECT_FINDING` / `OVERRIDE_CONTINUE`.
Each is recorded in `ReviewReport.humanDecisions` as an audit trail.

**Why this matters for the exam:** Knowing *when* to pause an autonomous agent
and yield to a human — and *how* to resume cleanly — is a core reliability pattern.

### 3. STDOUT reserved for MCP protocol (18% domain)

**No `System.out.println` anywhere in this codebase.** Ever.

Claude Desktop communicates with this server via JSON-RPC over STDIN/STDOUT.
A single stray log line corrupts the framing and silently breaks the connection.
All logging goes through SLF4J → Logback → STDERR.

This constraint is configured in `workshop-server/src/main/resources/logback.xml`
and enforced by code review. It is one of the most common bugs in first-time MCP
server implementations.

### 4. `Finding` record IS the output schema (20% domain)

The JSON schema enforced via `response_format` in every Anthropic API call
is derived directly from the fields of the `Finding` Java record.

`confidence`, `ruleId`, `severity` — these field names appear identically
in the Java record, the JSON schema string, and the Claude system prompt's
few-shot examples. One contract, three representations.

---

## Current Status

This is **Phase 1, in progress** — the foundation has been laid.

```
✅ Day 1-2  Multi-module Maven setup, both SDKs wired and compiling
              Anthropic Java SDK 2.18.0 + MCP SDK 1.0.0
              ApiSmokeTest: MCP SDK verified ✓, Anthropic API pending credits
✅ Day 3-4  Complete domain model implemented (8 classes, all with CERTIFICATION NOTEs)
              Finding, ReviewReport, ReviewScope, AgentResult, AgentSummary,
              EscalationRequest, HumanDecision, Severity

⏳ Day 5-6  SubAgent abstract class + SecurityAuditorAgent
              (first real Claude API call from a sub-agent)
⏳ Day 7    ContextWindowManager + TaskDecomposer
⏳ Day 8-9  OrchestratorAgent + StartReviewTool + GetReportTool
              (first testable end-to-end flow in Claude Desktop)
⏳ Day 10   EscalationHandler + RespondToEscalationTool
              (human-in-the-loop complete)
```

---

## Roadmap

```
v0.1.0  Phase 1 complete: end-to-end flow works in Claude Desktop
        SecurityAuditorAgent finds real vulnerabilities in sample code

v0.2.0  Phase 2: full agent suite
        All 4 specialists + SelfEvaluatorAgent + parallel execution

v0.3.0  Phase 3: certification study artifacts
        CLAUDE.md, custom slash command /workshop-review,
        GitHub Action that runs the Workshop on every PR

v1.0.0  Phase 4: native installers + full README + demo video
        ClaudeArchitectWorkshop.exe / .deb / .rpm
```

---

## Building

```bash
# Requires Java 21 and Maven 3.9+

git clone https://github.com/YOUR_USERNAME/claude-architect-workshop-mcp.git
cd claude-architect-workshop-mcp

# Compile everything
mvn compile

# Verify both SDKs are on the classpath
export ANTHROPIC_API_KEY=sk-ant-...
mvn package -pl workshop-server -am -DskipTests -q
java -jar workshop-server/target/workshop-server-1.0.0-SNAPSHOT.jar --smoke-test
```

Expected output:
```
Anthropic Java SDK: OK
MCP SDK:            OK
Both SDKs verified. Ready to implement Phase 1 Day 5-6.
```

---

## Claude Desktop Configuration

*(Available after v0.1.0 — native installer produced in Phase 4.
Until then, use the fat JAR directly.)*

```json
{
  "mcpServers": {
    "claude-architect-workshop": {
      "command": "java",
      "args": ["-jar", "/path/to/workshop-server-1.0.0-SNAPSHOT.jar"],
      "env": {
        "ANTHROPIC_API_KEY": "sk-ant-..."
      }
    }
  }
}
```

---

## Why This Approach

Most certification prep is passive: watch a video, take a quiz, move on.
This project forces active learning. You cannot add the `EscalationHandler`
without understanding *why* an agentic loop needs a pause point.
You cannot implement `ContextWindowManager` without understanding token budgets.
You cannot make `SecurityAuditorAgent` return valid findings without
understanding structured output and schema enforcement.

The exam tests whether you can *design* these systems, not just describe them.
This codebase is the practice ground.

---

## This Is a Journey

This project is not built in one sitting. It is developed **incrementally**,
one concept at a time, with full understanding at each step before moving forward.

Every meaningful phase of development is documented in the
[Show and tell](https://github.com/tabforgeai/claude-architect-workshop-mcp/discussions/categories/show-and-tell)
section of this repository's GitHub Discussions — with explanations of:

- what was built and why
- which exam domain it covers
- what architectural decision was made and what the alternatives were
- what the author learned in the process

If you are following along as a fellow CCA candidate, the Discussions are the
narrative behind the code. The code shows *what* was built; the Discussions
explain *why* it was built that way.

Speed is not the goal. Understanding is.

---

## Contributing

This project is in active development as a learning exercise.
Issues, suggestions, and pull requests are welcome.

If you are also preparing for the Claude Certified Architect exam,
feel free to use this codebase as a study companion.

---

## License

Apache 2.0 — see [LICENSE](LICENSE).

> **Note:** "Claude Architect Workshop" is a community learning project.
> It is not affiliated with or endorsed by Anthropic.
> "Claude" is a trademark of Anthropic.
