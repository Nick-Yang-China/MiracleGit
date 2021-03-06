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
	
	private Repository repository;
	

	/**
	 * Construct a {@link BranchOperation} object for a {@link Ref}.
	 *
	 * @param repository
	 * @param target
	 *            a short branch name or full name like below:
	 *            test or refs/heads/test
	 */
	public BranchOperation(Repository repository, String target) {
		this.repository=repository;
		this.target = target;
	}

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
					this.result = co.getResult();
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

	private void retryDelete(List<String> pathList) {
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

	@Override
	public String toString() {
		StringBuffer sb=new StringBuffer();
		CheckoutResult cr=this.result;
		switch (cr.getStatus()) {
		case OK:
			sb.append("Checkout completed normally");
			break;
		case CONFLICTS:
			sb.append("Checkout has not completed because of below checkout conflicts");
			for(String str :cr.getConflictList()){
				sb.append("\n"+str);
			}
			break;
		case ERROR:
			sb.append("An Exception occurred during checkout");
			break;
		default:
			break;
		}
		return sb.toString();
	}

	
}
