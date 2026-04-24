package ai.tabforge.workshop.agent;

import ai.tabforge.workshop.model.Finding;


/**
 * SubAgent need reference for OrchestratorAgent.
 * 
 * Since this maven sub-module (workshop-core) does not have dependency for workshop-orchestrator (where OrchestratorAgent is), we
 * cannot directly import ai.tabforge.workshop.orchestrator.OrchestratorAgent into SubAgent (needed for updateProgress() and escalate() call),
 * but workshop-orchestrator depends on workshop-core (so, OrchestratorAgent can implement this interface) - solution is that SubAgent has reference
 * to this intterface, and thus access to OrchestratorAgent.  
 * 
 */
public interface ProgressReporter {
    void updateProgress(String reviewId, String currentFile);
    /**
     * <p>
     *  If agent finds critical finding during file procession, and before continuing with the next file, 
     *  it will notify the orchestrator of the escalation, and stop the work. The agent does this by calling this method (the orchestrator implements this interface)
     *  </p>
     *  
     *   <p>
     *   By calling this method, the agent sends the reviewId, as well as information about his finding for the file it processes ( Severity - CRITICAL, INFO, WARNING, then lineNumber, ...)
     *   Knowing rewiewId and Finding, this method then sends the Finding on deciding what to do - let's call the mechanism that decides what to do next - the Escalation Handler
     *   </p>
     * @param reviewId
     * @param finding
     */
    void escalate(String reviewId, Finding finding);
}