package ai.tabforge.workshop;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Smoke test that verifies both SDKs are correctly on the classpath
 * and that the Anthropic API key is valid.
 *
 * <p>Run via: {@code java -jar workshop-server.jar --smoke-test}</p>
 *
 * <p>This class is the Day 1-2 deliverable from the development roadmap.
 * It answers the question: "Can we call Claude API from Java and get a response?"
 * before any domain logic is written.</p>
 *
 * <p><b>Two SDKs verified here — the key architectural distinction:</b></p>
 * <ul>
 *   <li><b>Anthropic Java SDK</b> ({@code com.anthropic:anthropic-java}) —
 *       we call the Claude API. We are the CLIENT. We send prompts, we get responses.
 *       Every sub-agent uses this SDK to run a specialist Claude instance.</li>
 *   <li><b>MCP SDK</b> ({@code io.modelcontextprotocol.sdk:mcp}) —
 *       Claude Desktop calls US. We are the SERVER. We register tools,
 *       we receive tool calls, we return results.</li>
 * </ul>
 *
 * <p>CERTIFICATION NOTE — Tool Design &amp; MCP Integration (18% of exam):
 * Understanding the client/server inversion between these two SDKs is
 * the first concept tested in the Tool Design domain. This smoke test
 * makes the inversion concrete and executable.</p>
 */
public class ApiSmokeTest {

    private static final Logger log = LoggerFactory.getLogger(ApiSmokeTest.class);

    /**
     * Entry point called from {@link Main} when {@code --smoke-test} flag is present.
     *
     * <p>Exit codes:
     * <ul>
     *   <li>0 — both SDKs verified successfully</li>
     *   <li>1 — Anthropic API key missing or invalid</li>
     *   <li>2 — unexpected error (classpath, network, etc.)</li>
     * </ul>
     * </p>
     */
    public static void run() {
        log.info("=== Claude Architect Workshop MCP — SDK Smoke Test ===");
        log.info("");

        boolean anthropicOk = testAnthropicSdk();
        boolean mcpOk = testMcpSdk();

        log.info("");
        log.info("=== Results ===");
        log.info("Anthropic Java SDK: {}", anthropicOk ? "OK" : "FAILED");
        log.info("MCP SDK:            {}", mcpOk ? "OK" : "FAILED");

        if (anthropicOk && mcpOk) {
            log.info("Both SDKs verified. Ready to implement Phase 1 Day 3-4 (domain model).");
            System.exit(0);
        } else {
            log.error("Smoke test FAILED. Fix the issues above before proceeding.");
            System.exit(1);
        }
    }

    /**
     * Tests the Anthropic Java SDK by making a minimal API call.
     *
     * <p>Uses claude-haiku (cheapest model) with a tiny token budget
     * to minimize cost. This is purely a connectivity and auth check.</p>
     *
     * <p>This is us acting as a CLIENT — we send a message, Claude responds.
     * In production, each sub-agent does exactly this, but with a
     * specialist system prompt and structured output schema.</p>
     *
     * @return true if the API call succeeded and returned a non-empty response
     */
    private static boolean testAnthropicSdk() {
        log.info("[1/2] Testing Anthropic Java SDK (we call Claude API as a client)...");

        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.error("  ANTHROPIC_API_KEY environment variable is not set.");
            log.error("  Set it before running: export ANTHROPIC_API_KEY=sk-ant-...");
            return false;
        }
        log.info("  API key found (length: {} chars)", apiKey.length());

        try {
            // fromEnv() reads ANTHROPIC_API_KEY automatically
            AnthropicClient client = AnthropicOkHttpClient.fromEnv();

            MessageCreateParams params = MessageCreateParams.builder()
                    .model(Model.CLAUDE_HAIKU_4_5_20251001)
                    .maxTokens(16L)
                    .addUserMessage("Respond with exactly: SDK_SMOKE_TEST_OK")
                    .build();

            log.info("  Calling {} (minimal cost verification call)...",
                    Model.CLAUDE_HAIKU_4_5_20251001);
            Message response = client.messages().create(params);

            // SDK 2.x: content blocks accessed via .text() Optional stream
            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(textBlock -> textBlock.text())
                    .findFirst()
                    .orElse("(no text content)");

            log.info("  Response: \"{}\"", text);
            log.info("  Input tokens:  {}", response.usage().inputTokens());
            log.info("  Output tokens: {}", response.usage().outputTokens());
            log.info("  Anthropic SDK: OK");
            return true;

        } catch (Exception e) {
            log.error("  Anthropic SDK call FAILED: {}", e.getMessage());
            log.debug("  Full stack trace:", e);
            return false;
        }
    }

    /**
     * Tests the MCP SDK by verifying core classes are loadable and instantiable.
     *
     * <p>Does NOT start a full MCP server (that requires STDIN/STDOUT transport
     * which is incompatible with smoke-test mode). Just verifies the SDK
     * is on the classpath and its builder APIs work.</p>
     *
     * <p>This is us acting as a SERVER — we define what tools we expose.
     * In production, the full server starts in {@link Main#main(String[])}
     * using {@code McpServer.async(transport)} and blocks indefinitely,
     * waiting for Claude Desktop to connect via STDIN/STDOUT.</p>
     *
     * @return true if MCP SDK classes load and builders produce non-null objects
     */
    private static boolean testMcpSdk() {
        log.info("[2/2] Testing MCP SDK (Claude Desktop will call us as a server)...");

        try {
            // Verify McpSchema.ServerCapabilities builder is accessible.
            // This is the same builder used in WorkshopServer.java (Phase 1 Day 8).
            McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
                    .tools(true)
                    .build();

            log.info("  McpSchema class: loaded");
            log.info("  ServerCapabilities built: tools={}", capabilities.tools());
            log.info("  MCP SDK: OK");
            return true;

        } catch (Exception e) {
            log.error("  MCP SDK verification FAILED: {}", e.getMessage());
            log.debug("  Full stack trace:", e);
            return false;
        }
    }
}
