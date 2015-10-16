package com.miracle.apps.git.core.op;

import com.miracle.apps.git.core.errors.CoreException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
/**
 * This class implements renaming of a branch
 */
public class RenameBranchOperation implements GitControlOperation {
	private final Repository repository;

	private final Ref branch;

	private final String newName;

	/**
	 * @param repository
	 * @param branch
	 *            the branch to rename
	 * @param newName
	 *            the new name
	 */
	public RenameBranchOperation(Repository repository, Ref branch,
			String newName) {
		this.repository = repository;
		this.branch = branch;
		this.newName = newName;
	}

	@Override
	public void execute() throws CoreException {
				try {
					new Git(repository).branchRename().setOldName(
							branch.getName()).setNewName(newName).call();
				} catch (JGitInternalException e) {
					throw new CoreException(e.getMessage(), e);
				} catch (GitAPIException e) {
					throw new CoreException(e.getMessage(), e);
				}
			}
	}

