package ai.tabforge.workshop.orchestrator;

import java.util.concurrent.CountDownLatch;

import ai.tabforge.workshop.agent.SubAgent;
import ai.tabforge.workshop.model.EscalationRequest;
import ai.tabforge.workshop.model.ReviewReport;
import ai.tabforge.workshop.model.ReviewScope;

/**
 * 
 *  When Claude AI periodically calls GetReportTool in order to obtain review status (and so, when  GetReportTool calls {@link OrchestratorAgent#getStatus()})
 *  the GetReportToll will return JSON filled with data from this class.
 *  
 *  An instance of this class is created in {@link OrchestratorAgent#startReview())} and is stored in an internal list of active sessions.
 *  
 *  

 *  TODO: GetReportTool description must document all possible response formats:
   RUNNING, AWAITING_HUMAN, COMPLETED, CANCELLED, FAILED
 *
 * <p>CERTIFICATION NOTE — Domain 1: Agentic Architecture &amp; Orchestration (27%):
 * Covers Task Statement 1.7: <em>"Manage session state, resumption, and forking"</em>.
 * {@code ReviewSession} is the live state of one review — it is created when the review
 * starts, mutated as agents report progress, paused when escalation occurs, and
 * finalized when the review completes. The orchestrator never loses track of where
 * a session is — even after a human-in-the-loop pause and resumption.</p>
 */
public class ReviewSession {
	private String reviewId;
	private ReviewScope scope;
	private ReviewReport.ReviewStatus status;
	private ReviewReport report;
	private EscalationRequest pendingEscalation;
	private String startedAt;

	/**
	 * We need following value only in the Phase 1. In that phase, agents (launched by {@link OrchestratorAgent#startReview(ReviewScope))} will run sequentially, one after another.
	 * And we need to know which agent is currently executing. (In Phase 2,  agents work in parallel with CompletableFuture)
	 * 
	 * @return
	 */

	private String currentAgentName;
	private int totalFiles;
	private int processedFiles;
	/**
	 * File currently being processed by the agent, represented by currentAgentName 
	 * The part of "analyzing OrderResource.java"  is formed from here.
	 * For details on how, and where, this value is set, see {@link OrchestratorAgent#updateProgress())}
	 * 
	 */
	private String currentlyProcessedFile;   // "OrderResource.java"
	private CountDownLatch latch; // blocking mechanism
	
	public CountDownLatch getLatch() {
		return latch;
	}
    /**
     * 
     * @param latch {@link OrchestratorAgent#escalate()}  (which is called in {@link SubAgent#execute()})  - will make CountDownLatch and preserve it here
     *     Will be read and released in the  {@link OrchestratorAgent#resumeAfterEscalation()}.
     *     
     *     It needs to be stored in the session, because that way it can be retrieved in resumeAfterEscalation() - which accept the reviewId
     *     
     */
	public void setLatch(CountDownLatch latch) {
		this.latch = latch;
	}

	public String getCurrentAgentName() {
		return currentAgentName;
	}

	/**
	 * Being called by  {@link OrchestratorAgent#startReview())} when the agent starts 
	 * @param currentAgentName
	 */
	public void setCurrentAgentName(String currentAgentName) {
		this.currentAgentName = currentAgentName;
	}

	public int getTotalFiles() {
		return totalFiles;
	}

	public void setTotalFiles(int totalFiles) {
		this.totalFiles = totalFiles;
	}

	public int getProcessedFiles() {
		return processedFiles;
	}

	public void setProcessedFiles(int processedFiles) {
		this.processedFiles = processedFiles;
	}

	public String getCurrentlyProcessedFile() {
		return currentlyProcessedFile;
	}

	public void setCurrentlyProcessedFile(String currentlyProcessedFile) {
		this.currentlyProcessedFile = currentlyProcessedFile;
	}
	/**
	 * In the phase 1, agents (launched by {@link OrchestratorAgent#startReview(ReviewScope))} will run sequentially, one after another. 
	 * When Claude AI periodically calls GetReportTool in order to obtain review status (and so, when  GetReportTool calls {@link OrchestratorAgent#getStatus()})
	 * then, if the status is not  {@link ReviewReport.ReviewStatus#COMPLETED}, the GetReportToll might return following JSON,
	 * filled with values from this class: 
	 * 
	 *  Status RUNNING:
         <pre><code>  
        {
           "status": "RUNNING",
           "progress": "SecurityAuditor 60% — analyzing OrderResource.java"
        }
         </code></pre>
         
        Pay attention on:  "progress": "SecurityAuditor 60% — analyzing OrderResource.java"
        Following method returns that value 
	 * 	
	 * @return
	 */

	  public String getProgress() {
	      if (totalFiles == 0) return "Initializing...";
	      int percent = (processedFiles * 100) / totalFiles;
	      return String.format("%s %d%% — analyzing %s",
	    		 getCurrentAgentName(), percent, getCurrentlyProcessedFile());
	  }

       // --------
	public String getReviewId() {
		return reviewId;
	}

	public ReviewScope getScope() {
		return scope;
	}

	public ReviewReport.ReviewStatus getStatus() {
		return status;
	}

	public void setStatus(ReviewReport.ReviewStatus status) {
		this.status = status;
	}

	public ReviewReport getReport() {
		return report;
	}

	public void setReport(ReviewReport report) {
		this.report = report;
	}

	public EscalationRequest getPendingEscalation() {
		return pendingEscalation;
	}

	public void setPendingEscalation(EscalationRequest pendingEscalation) {
		this.pendingEscalation = pendingEscalation;
	}

	public String getStartedAt() {
		return startedAt;
	}
	
	public ReviewSession() {
		
	}

	public ReviewSession(String reviewId, ReviewScope scope, ReviewReport.ReviewStatus status, ReviewReport report,
			EscalationRequest pendingEscalation, String startedAt) {
		this.reviewId = reviewId;
		this.scope = scope;
		this.status = status;
		this.report = report;
		this.pendingEscalation = pendingEscalation;
		this.startedAt = startedAt;
	}


}
