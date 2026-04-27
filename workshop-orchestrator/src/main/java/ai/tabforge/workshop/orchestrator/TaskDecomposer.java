package ai.tabforge.workshop.orchestrator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.tabforge.workshop.agent.SubAgent;
import ai.tabforge.workshop.agents.ArchitectureCheckerAgent;
import ai.tabforge.workshop.agents.PerformanceAnalystAgent;
import ai.tabforge.workshop.agents.SecurityAuditorAgent;
import ai.tabforge.workshop.agents.TransactionAnalystAgent;
import ai.tabforge.workshop.model.ReviewScope;

public class TaskDecomposer {
	
	/**
	 * 
	 * Should distribute files for review to the agents who need to review them.
	 * Called by {@link OrchestratorAgent#startReview()}.
	 * 
	 * 
	 * @param scope
	 * @return
	 */
	public Map<Class<? extends SubAgent>, List<Path>> decompose(ReviewScope scope) throws IOException, InterruptedException{
		List<Path> allFiles;
		  if (scope.scopeType() == ReviewScope.ScopeType.FULL_PROJECT) {
		      allFiles = getAllJavaFiles(scope.projectPath());
		  } else {
		      allFiles = getChangedJavaFiles(scope.projectPath());
		  }
		  Map<Class<? extends SubAgent>, List<Path>> result = new HashMap<>();

		  result.put(SecurityAuditorAgent.class, allFiles.stream()
		      .filter(p -> p.getFileName().toString().matches(".*Resource.*|.*Endpoint.*|.*Filter.*|.*Servlet.*"))
		      .collect(Collectors.toList()));

		  result.put(TransactionAnalystAgent.class, allFiles.stream()
		      .filter(p -> p.getFileName().toString().matches(".*Service.*|.*EJB.*|.*Bean.*"))
		      .collect(Collectors.toList()));

		  result.put(PerformanceAnalystAgent.class, allFiles.stream()
		      .filter(p -> p.getFileName().toString().matches(".*Repository.*|.*DAO.*|.*Entity.*"))
		      .collect(Collectors.toList()));

		  result.put(ArchitectureCheckerAgent.class, allFiles);

		  return result;
	}
	
	  private List<Path> getAllJavaFiles(String projectPath) throws IOException {
	      try (Stream<Path> stream = Files.walk(Path.of(projectPath))) {
	          return stream
	              .filter(p -> p.toString().endsWith(".java"))
	              .collect(Collectors.toList());
	      }
	  }
	  
	  private List<Path> getChangedJavaFiles(String projectPath) throws IOException, InterruptedException {
	      Process process = new ProcessBuilder("git", "diff", "--name-only", "HEAD~1")
	          .directory(Path.of(projectPath).toFile())
	          .start();

	      List<Path> changed = new BufferedReader(new InputStreamReader(process.getInputStream()))
	          .lines()
	          .filter(line -> line.endsWith(".java"))
	          .map(line -> Path.of(projectPath, line))
	          .collect(Collectors.toList());

	      process.waitFor();
	      return changed;
	  }

}
