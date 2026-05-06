package ai.tabforge.workshop;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.tabforge.workshop.orchestrator.OrchestratorAgent;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapperSupplier;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Wires together all MCP tools and starts the server over STDIO transport.
 *
 * <p>Think of this class as a <em>switchboard operator</em>: it knows which tools exist, 
 * registers them all with the MCP framework, and  then keeps the process alive so that Claude Desktop can 
 * call any tool at any time.</p>
 *
 * <p>STDIO transport means that Claude Desktop launches this process as a child process and
 * communicates via standard input/output streams — the same mechanism used by language
 * servers (LSP). No network port is opened.</p>
 *
 * <p>Created by: {@link ai.tabforge.workshop.Main#main(String[])}.</p>
 */
public class WorkshopServer {
	
	 private static final Logger log = LoggerFactory.getLogger(WorkshopServer.class);


    /**
     * Registers all tools and starts the MCP server. <strong>Blocks indefinitely</strong>
     * until the process is terminated.
     *
     * <p>Sequence:
     * <ol>
     *   <li>Instantiate all tools, each backed by the shared {@code OrchestratorAgent}.</li>
     *   <li>Build the MCP server with STDIO transport and the combined tool list.</li>
     *   <li>Block the current thread via {@link Thread#join()} — the MCP framework handles
     *       incoming requests on its own threads.</li>
     * </ol>
     * </p>
     *
     * <p>Called from: {@link ai.tabforge.workshop.Main#main(String[])}.</p>
     */
    public void start() {
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapperSupplier().get();
        OrchestratorAgent orchestrator = new OrchestratorAgent();
        List<McpServerFeatures.AsyncToolSpecification> tools = new ArrayList<>();
        tools.add(new StartReviewTool(orchestrator).toolSpecification(jsonMapper));
        tools.add(new GetReportTool(orchestrator).toolSpecification(jsonMapper));
        tools.add(new RespondToEscalationTool(orchestrator).toolSpecification(jsonMapper));
        tools.add(new CancelReviewTool(orchestrator).toolSpecification(jsonMapper));
        log.info("All tools registered.");


        StdioServerTransportProvider transport = new StdioServerTransportProvider(jsonMapper);

        McpServer.async(transport)
            .serverInfo("claude-architect-mcp", "1.0.0")
            .capabilities(McpSchema.ServerCapabilities.builder()
                .tools(true)
                .build())
            .tools(tools)
            .build();

        log.info("Claude Architect Workshop MCP Server ready — {} tools active (STDIO transport).",
            tools.size());

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }	
}
