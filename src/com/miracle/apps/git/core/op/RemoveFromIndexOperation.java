package com.miracle.apps.git.core.op;

import static org.eclipse.jgit.lib.Constants.HEAD;

import java.io.IOException;
import java.util.Collection;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * Remove from Git Index operation (unstage).
 */
public class RemoveFromIndexOperation implements GitControlService {

	private  Collection<String> pathsby;
	private  Repository repo;

	/**
	 * @param pathsby
	 *            list of paths that should be removed from index
	 * @param repository
	 *            repository in with given files should be removed from index
	 */
	public RemoveFromIndexOperation(Collection<String> paths,Repository repository) {
		this.pathsby = paths;
		this.repo=repository;
	}


	@Override
	public void execute() throws GitAPIException {
			GitCommand<?> command = prepareCommand();
			command.call();

	}


	private GitCommand<?> prepareCommand() {
		Git git = new Git(repo);
		if (hasHead(repo)) {
			ResetCommand resetCommand = git.reset();
			resetCommand.setRef(HEAD);
			for (String path : pathsby)
				resetCommand.addPath(getCommandPath(path));
			return resetCommand;
		} else {
			RmCommand rmCommand = git.rm();
			rmCommand.setCached(true);
			for (String path : pathsby)
				rmCommand.addFilepattern(getCommandPath(path));
			return rmCommand;
		}
	}

	private boolean hasHead(Repository repository) {
		try {
			Ref head = repository.getRef(HEAD);
			return head != null && head.getObjectId() != null;
		} catch (IOException e) {
			return false;
		}
	}

	private String getCommandPath(String path) {
		if ("".equals(path)) // Working directory //$NON-NLS-1$
			return "."; //$NON-NLS-1$
		else
			return path;
	}
}
