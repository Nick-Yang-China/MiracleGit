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

	private final String oldBranchName;

	private final String newName;
	
	private Ref ref;

	/**
	 * @param repository
	 * @param branch
	 *            the branch to rename
	 * @param newName
	 *            the new name
	 */
	public RenameBranchOperation(Repository repository, Ref branch,
			String newName) {
		this(repository, branch.getName(), newName);
	}
	
	/**
	 * @param repository
	 * @param oldBranchName
	 *            the branch to rename
	 * @param newBranchName
	 *            the new name
	 */
	public RenameBranchOperation(Repository repository, String oldBranchName,
			String newName) {
		this.repository = repository;
		this.oldBranchName = oldBranchName;
		this.newName = newName;
	}

	@Override
	public void execute() throws CoreException {
				try {
					ref=new Git(repository).branchRename().setOldName(oldBranchName).setNewName(newName).call();
				} catch (JGitInternalException e) {
					throw new CoreException(e.getMessage(), e);
				} catch (GitAPIException e) {
					throw new CoreException(e.getMessage(), e);
				}
			}

	@Override
	public String toString() {
		if(ref!=null){
			return new StringBuffer().append("BranchRename: "+ref.getName()).toString();
		}
		return super.toString();
	}
}

	

