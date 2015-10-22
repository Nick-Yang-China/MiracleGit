package com.miracle.apps.git.core.op;

import static java.util.Arrays.asList;
import java.util.HashSet;
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

	private final Set<Ref> branches;

	private final boolean force;

	/**
	 * @param repository
	 * @param branch
	 *            the branch to delete
	 * @param force
	 */
	public DeleteBranchOperation(Repository repository, Ref branch,
			boolean force) {
		this(repository, new HashSet<Ref>(asList(branch)), force);
	}

	/**
	 * @param repository
	 * @param branches
	 *            the list of branches to deleted
	 * @param force
	 */
	public DeleteBranchOperation(Repository repository, Set<Ref> branches,
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
				for (Ref branch : branches) {
					try {
						new Git(repository).branchDelete().setBranchNames(
								branch.getName()).setForce(force).call();
						status = OK;
					} catch (NotMergedException e) {
						status = REJECTED_UNMERGED;
						break;
					} catch (CannotDeleteCurrentBranchException e) {
						status = REJECTED_CURRENT;
						break;
					} catch (JGitInternalException e) {
						throw new CoreException(e.getMessage(), e);
					} catch (GitAPIException e) {
						throw new CoreException(e.getMessage(), e);
					}
				}
			}
	}

