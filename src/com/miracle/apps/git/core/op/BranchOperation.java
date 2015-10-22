/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2006, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.miracle.apps.git.core.op;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.miracle.apps.git.core.errors.CoreException;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CheckoutResult.Status;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.FileUtils;

/**
 * This class implements checkouts of a specific revision. A check is made that
 * this can be done without data loss.
 */
public class BranchOperation implements GitControlOperation {

	private final String target;

	private CheckoutResult result;

	private boolean delete;
	
	private Repository repository;

	/**
	 * Construct a {@link BranchOperation} object for a {@link Ref}.
	 *
	 * @param repository
	 * @param target
	 *            a {@link Ref} name or {@link RevCommit} id
	 */
	public BranchOperation(Repository repository, String target) {
		this(repository, target, true);
	}

	/**
	 * Construct a {@link BranchOperation} object for a {@link Ref}.
	 *
	 * @param repository
	 * @param target
	 *            a {@link Ref} name or {@link RevCommit} id
	 * @param delete
	 *            true to delete missing projects on new branch, false to close
	 *            them
	 */
	public BranchOperation(Repository repository, String target, boolean delete) {
		this.target = target;
		this.delete = delete;
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
					BranchOperation.this.result = co.getResult();
				}
				if (result.getStatus() == Status.NONDELETED)
					retryDelete(result.getUndeletedList());

				List<String> pathsToHandle = new ArrayList<String>();
				pathsToHandle.addAll(co.getResult().getModifiedList());
				pathsToHandle.addAll(co.getResult().getRemovedList());
				pathsToHandle.addAll(co.getResult().getConflictList());
//				IProject[] refreshProjects = ProjectUtil
//						.getProjectsContaining(repository, pathsToHandle);
//				ProjectUtil.refreshValidProjects(refreshProjects, delete,
//						new SubProgressMonitor(pm, 1));
//				pm.worked(1);
//
//				postExecute(pm);
//
//				pm.done();
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

//	/**
//	 * Compute the current projects that will be missing after the given branch
//	 * is checked out
//	 *
//	 * @param branch
//	 * @param currentProjects
//	 * @return non-null but possibly empty array of missing projects
//	 */
//	private IProject[] getMissingProjects(String branch,
//			IProject[] currentProjects) {
//		if (delete || currentProjects.length == 0)
//			return new IProject[0];
//
//		ObjectId targetTreeId;
//		ObjectId currentTreeId;
//		try {
//			targetTreeId = repository.resolve(branch + "^{tree}"); //$NON-NLS-1$
//			currentTreeId = repository.resolve(Constants.HEAD + "^{tree}"); //$NON-NLS-1$
//		} catch (IOException e) {
//			return new IProject[0];
//		}
//		if (targetTreeId == null || currentTreeId == null)
//			return new IProject[0];
//
//		Map<File, IProject> locations = new HashMap<File, IProject>();
//		for (IProject project : currentProjects) {
//			IPath location = project.getLocation();
//			if (location == null)
//				continue;
//			location = location
//					.append(IProjectDescription.DESCRIPTION_FILE_NAME);
//			locations.put(location.toFile(), project);
//		}
//
//		List<IProject> toBeClosed = new ArrayList<IProject>();
//		File root = repository.getWorkTree();
//		try (TreeWalk walk = new TreeWalk(repository)) {
//			walk.addTree(targetTreeId);
//			walk.addTree(currentTreeId);
//			walk.addTree(new FileTreeIterator(repository));
//			walk.setRecursive(true);
//			walk.setFilter(AndTreeFilter.create(PathSuffixFilter
//					.create(IProjectDescription.DESCRIPTION_FILE_NAME),
//					TreeFilter.ANY_DIFF));
//			while (walk.next()) {
//				AbstractTreeIterator targetIter = walk.getTree(0,
//						AbstractTreeIterator.class);
//				if (targetIter != null)
//					continue;
//
//				AbstractTreeIterator currentIter = walk.getTree(1,
//						AbstractTreeIterator.class);
//				AbstractTreeIterator workingIter = walk.getTree(2,
//						AbstractTreeIterator.class);
//				if (currentIter == null || workingIter == null)
//					continue;
//
//				IProject project = locations.get(new File(root, walk
//						.getPathString()));
//				if (project != null)
//					toBeClosed.add(project);
//			}
//		} catch (IOException e) {
//			return new IProject[0];
//		}
//		return toBeClosed.toArray(new IProject[toBeClosed.size()]);
//	}
}
