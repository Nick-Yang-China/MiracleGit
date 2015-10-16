package com.miracle.apps.git.core.op;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import com.miracle.apps.git.core.errors.CoreException;

/**
 * A class for changing a ref and possibly index and workdir too.
 */
public class ResetOperation implements GitControlService {

	private final Repository repository;

	private final String refName;

	private final ResetType type;

	/**
	 * Construct a {@link ResetOperation}
	 *
	 * @param repository
	 * @param refName
	 * @param type
	 */
	public ResetOperation(Repository repository, String refName, ResetType type) {
		this.repository = repository;
		this.refName = refName;
		this.type = type;
	}

	@Override
	public void execute() throws CoreException {
		ResetCommand reset = Git.wrap(repository).reset();
		reset.setMode(type);
		reset.setRef(refName);
		try {
			reset.call();
		} catch (GitAPIException e) {
			throw new CoreException(e.getLocalizedMessage(), e.getCause());
		}

	}
}
