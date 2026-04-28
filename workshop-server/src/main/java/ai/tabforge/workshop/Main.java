package ai.tabforge.workshop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the Claude Architect Workshop MCP Server.
 *
 * <p>Launched by Claude Desktop / Cursor via the path registered in
 * claude_desktop_config.json. Starts the MCP server on STDIN/STDOUT
 * (StdioServerTransport) and blocks until the parent process disconnects.</p>
 *
 * <p>CRITICAL: This class must NEVER write to System.out directly.
 * STDOUT is owned by the MCP protocol (JSON-RPC framing).
 * All diagnostic output goes through SLF4J → Logback → STDERR.
 * One stray println corrupts the MCP framing and breaks Claude Desktop.</p>
 *
 * <p>Analogy: like a language server (LSP) — it communicates via a
 * structured protocol on STDIN/STDOUT. Your IDE's log window shows
 * its stderr; it never clutters the protocol channel.</p>
 *
 * <p>CERTIFICATION NOTE — Tool Design &amp; MCP Integration (18% of exam):
 * The MCP server lifecycle (start → register tools → serve → shutdown)
 * is what this class orchestrates. The tools themselves live in
 * {@code ai.tabforge.workshop.tools}.</p>
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Claude Architect Workshop MCP Server starting...");
        log.info("Version: 1.0.0-SNAPSHOT");
        log.info("ANTHROPIC_API_KEY present: {}", System.getenv("ANTHROPIC_API_KEY") != null);

        // Phase 1 Day 1-2: Verify both SDKs are on the classpath.
        // Run with: java -cp workshop-server.jar ai.tabforge.workshop.Main --smoke-test
        if (args.length > 0 && "--smoke-test".equals(args[0])) {
            ApiSmokeTest.run();
            return;
        }

        // Full MCP server startup — implemented in Phase 1 Day 8-9
        log.info("MCP server startup not yet implemented. Run with --smoke-test to verify SDKs.");
        log.info("See Phase 1 Day 8-9 in CLAUDE_ARCHITECT_WORKSHOP_MCP_PLAN.md");
    }
}
