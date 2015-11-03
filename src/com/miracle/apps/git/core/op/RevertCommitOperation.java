package com.miracle.apps.git.core.op;

import java.util.List;
import com.miracle.apps.git.core.errors.CoreException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.RevertCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Operation to revert a commit
 */
public class RevertCommitOperation implements GitControlOperation {

	private final Repository repo;

	private final List<RevCommit> commits;

	private RevCommit newHead;

	private List<Ref> reverted;

	private MergeResult result;
	
	private String strategyName;

	/**
	 * Create revert commit operation
	 *
	 * @param repository
	 * @param commits
	 *            the commits to revert (in newest-first order)
	 */
	public RevertCommitOperation(Repository repository, List<RevCommit> commits) {
		this.repo = repository;
		this.commits = commits;
	}

	/**
	 * @return new head commit
	 */
	public RevCommit getNewHead() {
		return newHead;
	}

	/**
	 * @return reverted refs
	 */
	public List<Ref> getRevertedRefs() {
		return reverted;
	}

	@Override
	public void execute() throws CoreException {
				RevertCommand command = new Git(repo).revert();
				MergeStrategy strategy = MergeStrategy.get(strategyName);
				if (strategy != null) {
					command.setStrategy(strategy);
				}
				for (RevCommit commit : commits)
					command.include(commit);
				try {
					newHead = command.call();
					reverted = command.getRevertedRefs();
					result = command.getFailingResult();
				} catch (GitAPIException e) {
					throw new CoreException(e.getLocalizedMessage(),
							e.getCause());
				}
	}

	/**
	 * Get failing result of merge
	 *
	 * @return merge result
	 */
	public MergeResult getFailingResult() {
		return result;
	}
	
	/**
	 * Setup the StrategyName
	 * @param strategyName
	 */
	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
	}
}
