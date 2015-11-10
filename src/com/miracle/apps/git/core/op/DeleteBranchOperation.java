package com.miracle.apps.git.core.op;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CannotDeleteCurrentBranchException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NotMergedException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import com.miracle.apps.git.core.errors.CoreException;

/**
 * This class implements deletion of a branch
 */
public class DeleteBranchOperation implements GitControlOperation {
	/** Operation was performed */
	public final static int OK = 0;

	/** Current branch cannot be deleted */
	public final static int REJECTED_CURRENT = 1;

	/**
	 * Branch to be deleted has not been fully merged; use force to delete
	 * anyway
	 */
	public final static int REJECTED_UNMERGED = 2;

	/** This operation was not executed yet */
	public final static int NOT_TRIED = -1;

	private int status = NOT_TRIED;

	private final Repository repository;

	private final List<String> branches;

	private final boolean force;
	
	private List<String> deleteBranchList;
	
	/**
	 * @param repository
	 * @param branch
	 *            the branch to delete: test or refs/heads/test
	 * @param force
	 * @throws IOException 
	 */
	public DeleteBranchOperation(Repository repository, String branch,
			boolean force) throws IOException {
		this(repository, Arrays.asList(branch), force);
	}

	/**
	 * @param repository
	 * @param branch
	 *            the branch to delete
	 * @param force
	 */
	public DeleteBranchOperation(Repository repository, Ref branch,
			boolean force) {
		this(repository, Arrays.asList(branch.getName()), force);
	}

	/**
	 * @param repository
	 * @param branches
	 *            the list of branches to deleted
	 * @param force
	 */
	public DeleteBranchOperation(Repository repository, List<String> branches,
			boolean force) {
		this.repository = repository;
		this.branches = branches;
		this.force = force;
	}

	/**
	 * @return one of {@link #OK}, {@link #REJECTED_CURRENT},
	 *         {@link #REJECTED_UNMERGED}, {@link #NOT_TRIED}
	 */
	public int getStatus() {
		return status;
	}

	@Override
	public void execute() throws CoreException {
	
					String[] branchnames=branches.toArray(new String[branches.size()]);
					try {
						deleteBranchList=new Git(repository).branchDelete().setBranchNames(
								branchnames).setForce(force).call();
						status = OK;
					} catch (NotMergedException e) {
						status = REJECTED_UNMERGED;
					} catch (CannotDeleteCurrentBranchException e) {
						status = REJECTED_CURRENT;
					} catch (JGitInternalException e) {
						throw new CoreException(e.getMessage(), e);
					} catch (GitAPIException e) {
						throw new CoreException(e.getMessage(), e);
					}
			}

	@Override
	public String toString() {
		StringBuffer sb=new StringBuffer();
		if(status==OK){
			sb.append("BranchDelete completed normally, below are deletebranchlist:");
			for(String branchList : deleteBranchList){
					sb.append("\n"+branchList);
			}
		}else if (status==REJECTED_CURRENT) {
			sb.append("The current branch can not be deleted successfully");
		}else if(status==REJECTED_UNMERGED){
			sb.append("Branch to be deleted has not been fully merged; use force to delete anyway");
		}else{
			sb.append("An Exception occurred during deletebranch");
		}
		
		return sb.toString();
	}

}


