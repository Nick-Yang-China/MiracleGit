package com.miracle.apps.git.core.op;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.miracle.apps.git.core.errors.CoreException;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CheckoutResult.Status;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.FileUtils;

/**
 * This class implements checkouts of a specific revision. A check is made that
 * this can be done without data loss.
 */
public class BranchOperation implements GitControlOperation {

	private final String target;

	private CheckoutResult result;

//	private boolean delete;
	
	private Repository repository;
	
//	private List<String> pathsToHandle;

	/**
	 * Construct a {@link BranchOperation} object for a {@link Ref}.
	 *
	 * @param repository
	 * @param target
	 *            a {@link Ref} name or {@link RevCommit} id
	 */
	public BranchOperation(Repository repository, String target) {
		this.repository=repository;
		this.target = target;
	}

//	/**
//	 * Construct a {@link BranchOperation} object for a {@link Ref}.
//	 *
//	 * @param repository
//	 * @param target
//	 *            a {@link Ref} name or {@link RevCommit} id
//	 * @param delete
//	 *            true to delete missing projects on new branch, false to close
//	 *            them
//	 */
//	public BranchOperation(Repository repository, String target, boolean delete) {
//		this.repository=repository;
//		this.target = target;
//		this.delete = delete;
//	}

	@Override
	public void execute() throws CoreException {
				CheckoutCommand co = new Git(repository).checkout();
				co.setName(target);
				try {
					co.call();
				} catch (CheckoutConflictException e) {
					return;
				} catch (JGitInternalException e) {
					throw new CoreException(e.getMessage(), e);
				} catch (GitAPIException e) {
					throw new CoreException(e.getMessage(), e);
				} finally {
					BranchOperation.this.result = co.getResult();
				}
				if (result.getStatus() == Status.NONDELETED)
					retryDelete(result.getUndeletedList());

//				pathsToHandle = new ArrayList<String>();
//				pathsToHandle.addAll(co.getResult().getModifiedList());
//				pathsToHandle.addAll(co.getResult().getRemovedList());
//				pathsToHandle.addAll(co.getResult().getConflictList());
	}


	/**
	 * @return the result of the operation
	 */
	public CheckoutResult getResult() {
		return result;
	}

	void retryDelete(List<String> pathList) {
		// try to delete, but for a short time only
		long startTime = System.currentTimeMillis();
		for (String path : pathList) {
			if (System.currentTimeMillis() - startTime > 1000)
				break;
			File fileToDelete = new File(repository.getWorkTree(), path);
			if (fileToDelete.exists())
				try {
					// Only files should be passed here, thus
					// we ignore attempt to delete submodules when
					// we switch to a branch without a submodule
					if (!fileToDelete.isFile())
						FileUtils.delete(fileToDelete, FileUtils.RETRY);
				} catch (IOException e) {
					// ignore here
				}
		}
	}

}
