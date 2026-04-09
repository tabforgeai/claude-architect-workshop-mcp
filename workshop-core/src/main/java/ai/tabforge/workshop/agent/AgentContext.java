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
 * <pre><code>
 *  
 * │ SecurityAuditorAgent     →  [OrderResource.java, PaymentService.java, ...] 
 * │ TransactionAnalystAgent  →  [PaymentService.java, AccountEJB.java, ...] 
 * │ PerformanceAnalystAgent  →  [OrderRepository.java, ...] │ ArchitectureCheckerAgent→ [all files, signatures only]
 * </code></pre>
 * 
 * Having that, for each agent + his file list, OrchestratorAgent makes
 * AgentContext instance, and starts agent execution by calling
 * SubAgent.execute(), giving it an agent context as parameter.
 * 
 * </p>
 * 
 * <p>
 * CERTIFICATION NOTE — Agentic Architecture & Orchestration (27% of exam) -  Domain 1. 
 * Covers following task statements:
 * <ul>
 * <li>Task Statement 1.2: Orchestrate multi-agent systems with  coordinator-subagent patterns</li>
 * <li>Task Statement 1.3: Configure subagent invocation, context passing, and  spawning</li>
 * </ul>
 * 
 * 
 * from <a href=
 * "https://everpath-course-content.s3-accelerate.amazonaws.com/instructor%2F8lsy243ftffjjy1cx9lm3o2bw%2Fpublic%2F1773274827%2FClaude+Certified+Architect+%E2%80%93+Foundations+Certification+Exam+Guide.pdf">Claude
 * Certified Architect – Foundations Certification Exam Guide</a>
 * 
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
 *  Let's explain in a simple way what a token is in general.
    A token is a piece of text — roughly a word, or part of a word: 
     <pre><code>
     "Hello world" → 2 tokens 
     "OrderResource" → 2-3 tokens 
     "@" → 1 token
     </code></pre> 
     <p>
     It's not exactly one token = one word, but it's a good enough approximation to understand. 
     Let's assume 4 characters make up one token.
     </p>

<p>
Now, when one of our agents, let say SecurityAuditorAgent, sends an API call, that one call goes:
<pre><code>
[system prompt] ← "You are a security auditor, looking for SQL injection..."
[file contents] ← the actual Java code it analyzes
[output schema] ← "respond in this JSON format"
</code></pre>
All those words together, they're all _INPUT TOKENS_.
</p>
<p>
Then, we also have a response from the Claude API, and that response arrives in the form of some JSON.  
That output JSON represents the  _OUTPUT TOKENS_
<pre><code>
_Rule: INPUT TOKENS + OUTPUT TOKENS must be < 200,000_  ← this is Claude's total capacity for one API call, ie, this is a Context Window capacity.
</code></pre>
We will use the _tokenBudget_  value to notify the Claude API (when we send him a request) how many tokens its JSON response is allowed to have. When the total number of input tokens is calculated in some way, as a sum of 
`system prompt + file contents + output schema JSON description `
(in the application we will have a special class for these needs, the {@code ContextWindowManager}) then _tokenBudget_ could be calculated as:

<pre><code>
tokenBudget = 200.000 - INPUT TOKENS
</code></pre>
In practice, 8096 tokens are enough for a JSON response, so we will take the smaller value between the one calculated above and 8096.
</p>

	 * @return
	 */
	public int getTokenBudget() {
		return tokenBudget;
	}

}
