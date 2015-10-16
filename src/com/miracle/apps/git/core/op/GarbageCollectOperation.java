package com.miracle.apps.git.core.op;

import com.miracle.apps.git.core.errors.CoreException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * Operation to garbage collect a git repository
 */
public class GarbageCollectOperation implements GitControlService {

	private Repository repository;

	/**
	 * @param repository the repository to garbage collect
	 */
	public GarbageCollectOperation(Repository repository) {
		this.repository = repository;
	}

	/**
	 * Execute garbage collection
	 */
	@Override
	public void execute() throws CoreException {
		Git git = new Git(repository);
//		EclipseGitProgressTransformer pm = new EclipseGitProgressTransformer(
//				monitor);
		try {
			git.gc().setProgressMonitor(null).call();
		} catch (GitAPIException e) {
			throw new CoreException(e.getMessage(), e);
		}
	}
}
