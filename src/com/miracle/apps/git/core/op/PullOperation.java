package com.miracle.apps.git.core.op;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.miracle.apps.git.core.errors.CoreException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.CredentialsProvider;

/**
 * Wraps the JGit API {@link PullCommand} into an operation
 */
public class PullOperation implements GitControlOperation{
	private final Repository[] repositories;

	private final Map<Repository, Object> results = new LinkedHashMap<Repository, Object>();

	private final int timeout;

	private CredentialsProvider credentialsProvider;

	/**
	 * @param repositories
	 *            the repository
	 * @param timeout
	 *            in seconds
	 */
	public PullOperation(Set<Repository> repositories, int timeout) {
		this.timeout = timeout;
		this.repositories = repositories.toArray(new Repository[repositories
				.size()]);
	}

	@Override
	public void execute() throws CoreException {
		if (!results.isEmpty())
			throw new CoreException("Operation has already been executed and cannot be executed again");
				
		for (int i = 0; i < repositories.length; i++) {
					Repository repository = repositories[i];
					PullCommand pull = new Git(repository).pull();
					PullResult pullResult = null;
					try {
//						pull.setProgressMonitor(new EclipseGitProgressTransformer(
//								new SubProgressMonitor(mymonitor, 1)));
						pull.setTimeout(timeout);
						pull.setCredentialsProvider(credentialsProvider);
						MergeStrategy strategy = MergeStrategy.OURS;// Activator.getDefault().getPreferredMergeStrategy();
						if (strategy != null) {
							pull.setStrategy(strategy);
						}
						pullResult = pull.call();
						results.put(repository, pullResult);
					} catch (DetachedHeadException e) {
						results.put(repository, "No local branch is currently checked out");
					} catch (InvalidConfigurationException e) {
						results.put(repository, "The current branch is not configured for pull");
					} catch (GitAPIException e) {
						results.put(repository,e.getMessage());
					} catch (JGitInternalException e) {
						Throwable cause = e.getCause();
						if (cause == null || !(cause instanceof TransportException))
							cause = e;
						results.put(repository,cause.getMessage());
					} 
				}
	}

	private boolean refreshNeeded(PullResult pullResult) {
		if (pullResult == null)
			return true;
		MergeResult mergeResult = pullResult.getMergeResult();
		if (mergeResult == null)
			return true;
		if (mergeResult.getMergeStatus() == MergeStatus.ALREADY_UP_TO_DATE)
			return false;
		return true;
	}

	/**
	 * @return the results, or an empty Map if this has not been executed
	 */
	public Map<Repository, Object> getResults() {
		return this.results;
	}

	/**
	 * @param credentialsProvider
	 */
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * @return the operation's credentials provider
	 */
	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}
}
