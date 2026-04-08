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
 *  
 * │ SecurityAuditorAgent     →  [OrderResource.java, PaymentService.java, ...] 
 * │ TransactionAnalystAgent  →  [PaymentService.java, AccountEJB.java, ...] 
 * │ PerformanceAnalystAgent  →  [OrderRepository.java, ...] │ ArchitectureCheckerAgent→ [all files, signatures only]
 * 
 * Having that, for each agent + his file list, OrchestratorAgent makes
 * AgentContext instance, and starts agent execution by calling
 * SubAgent.execute(), giving it an agent context as parameter.
 * 
 * </p>
 * 
 * <p>
 * CERTIFICATION NOTE — Agentic Architecture & Orchestration (27% of exam) -
 * Domain 1. Covers following task statements:
 * <ul>
 * <li>Task Statement 1.2: Orchestrate multi-agent systems with
 * coordinator-subagent patterns</li>
 * <li>Task Statement 1.3: Configure subagent invocation, context passing, and
 * spawning</li>
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

	public int getTokenBudget() {
		return tokenBudget;
	}

}
