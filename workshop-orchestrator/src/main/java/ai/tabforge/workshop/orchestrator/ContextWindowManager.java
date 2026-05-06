package ai.tabforge.workshop.orchestrator;

import java.util.ArrayList;
import java.util.List;

/**
 * Enforces token budget constraints for each sub-agent API call.
 *
 * <p>Every Claude API call has two costs that share the same 200,000-token context window:
 * <pre>
 *   INPUT  = system prompt + file content
 *   OUTPUT = JSON findings array returned by Claude
 *   INPUT + OUTPUT must be &lt; 200,000 tokens
 * </pre>
 *
 * <p>This class answers two questions before every {@code analyzeFile()} call:
 * <ol>
 *   <li>How many output tokens can we safely request?
 *       ({@link #computeMaxOutputTokens(String, String)})</li>
 *   <li>Is this file too large for one call — and if so, how do we split it?
 *       ({@link #chunkFile(String, String)})</li>
 * </ol>
 *
 * <p>Token estimation uses the 4-characters-per-token approximation.
 * This is not exact (real tokenizers depend on the model and language),
 * but it is accurate enough for Java source code and avoids a dependency
 * on a tokenizer library.
 *
 * <p>CERTIFICATION NOTE — Context Management &amp; Reliability (15% of exam):
 * This class directly implements the "token budget management" pattern tested
 * in Domain 5. The two key ideas: (1) estimate input cost before making a call,
 * (2) chunk content that would exceed the budget rather than letting the API
 * reject the request. Both appear in the exam as reliability patterns for
 * production agentic systems.
 */
public class ContextWindowManager {

    /** Claude's context window — shared by input and output tokens. */
    static final int CONTEXT_WINDOW = 200_000;

    /**
     * Maximum output tokens we request per call.
     * 8,096 is enough for a complete JSON findings array from any specialist agent.
     * Requesting more wastes quota and slows responses.
     */
    static final int MAX_OUTPUT_TOKENS = 8_096;

    /**
     * Characters-per-token approximation.
     * Rule of thumb: 1 token ≈ 4 English characters, or ~0.75 words.
     * Java identifiers, annotations, and braces tokenize similarly.
     */
    private static final int CHARS_PER_TOKEN = 4;

    /**
     * Safety margin applied to all budget calculations.
     * Keeps us 10% below the theoretical limit so estimation errors
     * never push a call over the window.
     */
    private static final double SAFETY_FACTOR = 0.9;

    /**
     * Estimates the token count for any string.
     * Uses the 4-chars-per-token approximation — fast and dependency-free.
     *
     * @param text system prompt, file content, or any other string
     * @return estimated token count; 0 for null or empty input
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / (double) CHARS_PER_TOKEN);
    }

    /**
     * Returns the safe {@code maxTokens} value to pass to
     * {@code MessageCreateParams.builder().maxTokens(...)}.
     *
     * <p>Formula:
     * <pre>
     *   inputTokens = estimate(systemPrompt) + estimate(fileContent)
     *   available   = (CONTEXT_WINDOW - inputTokens) * SAFETY_FACTOR
     *   result      = min(available, MAX_OUTPUT_TOKENS)
     * </pre>
     *
     * <p>If {@code available &lt;= 0} the content is too large for one call.
     * The caller should invoke {@link #chunkFile(String, String)} first.
     *
     * @param systemPrompt the agent's system prompt (result of {@code buildPrompt()})
     * @param fileContent  the Java source file to be analyzed
     * @return safe max output tokens; 0 means the file must be chunked before calling
     */
    public int computeMaxOutputTokens(String systemPrompt, String fileContent) {
        int inputTokens = estimateTokens(systemPrompt) + estimateTokens(fileContent);
        int available = (int) ((CONTEXT_WINDOW - inputTokens) * SAFETY_FACTOR);
        if (available <= 0) return 0;
        return Math.min(available, MAX_OUTPUT_TOKENS);
    }

    /**
     * The default {@code maxOutputTokens} used when actual file content is not
     * yet known — for example, when {@code OrchestratorAgent} builds an
     * {@code AgentContext} before the agent has read the files.
     *
     * <p>This value is conservative: a JSON findings array rarely exceeds
     * 4,000 tokens, so 8,096 gives comfortable headroom without over-allocating.
     *
     * @return {@link #MAX_OUTPUT_TOKENS}
     */
    public int getDefaultMaxOutputTokens() {
        return MAX_OUTPUT_TOKENS;
    }

    /**
     * Splits file content into chunks that each fit within the context window
     * alongside the given system prompt.
     *
     * <p>When {@link #computeMaxOutputTokens(String, String)} returns 0,
     * the file is too large for one API call. This method splits it on line
     * boundaries (never mid-line) so each chunk can be analyzed independently.
     * The results are merged by the caller.
     *
     * <p>Chunking strategy:
     * <pre>
     *   maxContentTokens = (CONTEXT_WINDOW - promptTokens - MAX_OUTPUT_TOKENS) * SAFETY_FACTOR
     *   split on newlines until each chunk stays within maxContentTokens
     * </pre>
     *
     * <p>CERTIFICATION NOTE — Context Management &amp; Reliability (15%):
     * File chunking is the standard solution when a codebase file exceeds the
     * context window. The exam tests whether you know to split on semantic
     * boundaries (lines, methods) rather than raw character offsets, and to
     * merge partial results after each chunk is processed.
     *
     * @param fileContent  the full content of the file to split
     * @param systemPrompt the agent's system prompt (consumed from the budget)
     * @return list of chunks — single-element list if no chunking was needed;
     *         each chunk is guaranteed to fit in one API call
     */
    public List<String> chunkFile(String fileContent, String systemPrompt) {
        int promptTokens = estimateTokens(systemPrompt);
        int maxContentTokens = (int) ((CONTEXT_WINDOW - promptTokens - MAX_OUTPUT_TOKENS) * SAFETY_FACTOR);
        int maxContentChars = maxContentTokens * CHARS_PER_TOKEN;

        if (fileContent.length() <= maxContentChars) {
            return List.of(fileContent);
        }

        List<String> chunks = new ArrayList<>();
        String[] lines = fileContent.split("\n", -1);
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            // +1 accounts for the newline character stripped by split()
            if (current.length() + line.length() + 1 > maxContentChars && !current.isEmpty()) {
                chunks.add(current.toString());
                current = new StringBuilder();
            }
            current.append(line).append("\n");
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }
}
