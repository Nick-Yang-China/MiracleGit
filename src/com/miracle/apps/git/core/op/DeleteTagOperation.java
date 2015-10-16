package com.miracle.apps.git.core.op;

import com.miracle.apps.git.core.errors.CoreException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * Operation that deletes a tag
 */
public class DeleteTagOperation implements GitControlOperation {

	private final Repository repository;

	private final String tag;

	/**
	 * Create operation that deletes a single tag
	 *
	 * @param repository
	 * @param tag
	 */
	public DeleteTagOperation(final Repository repository, final String tag) {
		this.repository = repository;
		this.tag = tag;
	}

	@Override
	public void execute() throws CoreException {
		try {
			Git.wrap(repository).tagDelete().setTags(tag).call();
		} catch (GitAPIException e) {
			throw new CoreException("Exception deleting tag");
		}
	}
}
