package ai.tabforge.workshop.agent;

import java.nio.file.Path;
import java.util.List;

/**
 * <p>
 * Analogy: Like the work order that the shift leader gives to the worker at the
 * beginning of the shift — it contains exactly what he needs to do (list of
 * files), where is the construction site (projectPath), order number
 * (reviewId), and how much material we can spend (tokenBudget). The worker does
 * not decides what to do — just executes what is written in the work order.
 * </p>
 * 
 * <p>
 * Created by: {@code OrchestratorAgent}, after {@code TaskDecomposer} finishes
 * his job. TaskDecomposer returns data structure which describes which agent
 * should work with which files, for example:
 * </p>
 * <pre><code>
 * │ SecurityAuditorAgent     →  [OrderResource.java, PaymentService.java, ...]
 * │ TransactionAnalystAgent  →  [PaymentService.java, AccountEJB.java, ...]
 * │ PerformanceAnalystAgent  →  [OrderRepository.java, ...]
 * │ ArchitectureCheckerAgent →  [all files, signatures only]
 * </code></pre>
 * <p>
 * Having that, for each agent + his file list, OrchestratorAgent makes
 * AgentContext instance, and starts agent execution by calling
 * SubAgent.execute(), giving it an agent context as parameter.
 * </p>
 *
 * <p>
 * CERTIFICATION NOTE &mdash; Agentic Architecture &amp; Orchestration (27% of exam) - Domain 1.
 * Covers following task statements:
 * </p>
 * <ul>
 * <li>Task Statement 1.2: Orchestrate multi-agent systems with coordinator-subagent patterns</li>
 * <li>Task Statement 1.3: Configure subagent invocation, context passing, and spawning</li>
 * </ul>
 * <p>
 * from <a href=
 * "https://everpath-course-content.s3-accelerate.amazonaws.com/instructor%2F8lsy243ftffjjy1cx9lm3o2bw%2Fpublic%2F1773274827%2FClaude+Certified+Architect+%E2%80%93+Foundations+Certification+Exam+Guide.pdf">Claude
 * Certified Architect &#8211; Foundations Certification Exam Guide</a>
 * </p>
 */
public class AgentContext {

	private final String projectPath;
	private final List<Path> fileList;
	private final String reviewId;
	private final int tokenBudget;

	public AgentContext(String projectPath, List<Path> fileList, String reviewId, int tokenBudget) {
		this.projectPath = projectPath;
		this.fileList = fileList;
		this.reviewId = reviewId;
		this.tokenBudget = tokenBudget;
	}

	public String getProjectPath() {
		return projectPath;
	}

	public List<Path> getFileList() {
		return fileList;
	}

	public String getReviewId() {
		return reviewId;
	}

/**
 * Let's explain in a simple way what a token is in general.
 * A token is a piece of text — roughly a word, or part of a word:
 * <pre><code>
 * "Hello world"  → 2 tokens
 * "OrderResource" → 2-3 tokens
 * "@"            → 1 token
 * </code></pre>
 * <p>
 * It's not exactly one token = one word, but it's a good enough approximation.
 * Let's assume 4 characters make up one token.
 * </p>
 * <p>
 * When one of our agents (e.g. SecurityAuditorAgent) sends an API call, that call goes:
 * </p>
 * <pre><code>
 * [system prompt] ← "You are a security auditor, looking for SQL injection..."
 * [file contents] ← the actual Java code it analyzes
 * [output schema] ← "respond in this JSON format"
 * </code></pre>
 * <p>
 * All those words together are INPUT TOKENS. The Claude API response (a JSON with findings)
 * represents OUTPUT TOKENS. Rule: INPUT + OUTPUT must be &lt; 200,000 — that is the context window.
 * </p>
 * <p>
 * tokenBudget tells the API how many output tokens its response may use:
 * </p>
 * <pre><code>
 * tokenBudget = 200,000 - INPUT TOKENS
 * </code></pre>
 * <p>
 * In practice 8096 tokens is enough for a JSON response, so we take the smaller of the two.
 * {@code ContextWindowManager} handles this calculation.
 * </p>
 *
 * @return the maximum number of output tokens allowed for one agent API call
 */
	public int getTokenBudget() {
		return tokenBudget;
	}

}
