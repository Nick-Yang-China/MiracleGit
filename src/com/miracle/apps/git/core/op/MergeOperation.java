/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP AG and others.
 * Copyright (C) 2012, 2013 Tomasz Zarna <tzarna@gmail.com>
 * Copyright (C) 2014 Axel Richard <axel.richard@obeo.fr>
 * Copyright (C) 2015 Obeo
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Tomasz Zarna (IBM) - merge squash, bug 382720
 *    Axel Richard (Obeo) - merge message, bug 422886
 *    Laurent Delaigue (Obeo) - use of preferred merge strategy
 *******************************************************************************/
package com.miracle.apps.git.core.op;

import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;

import com.miracle.apps.git.core.errors.CoreException;

/**
 * This class implements the merge of a ref with the current head
 *
 */
public class MergeOperation implements GitControlOperation{

	private final Repository repository;

	private final String refName;

	private final MergeStrategy mergeStrategy;

	private Boolean squash;

	private FastForwardMode fastForwardMode;

	private Boolean commit;

	private MergeResult mergeResult;

	private String message;

	/**
	 * Create a MergeOperation object. Initializes the MergeStrategy with the
	 * preferred merge strategy, according to preferences.
	 *
	 * @param repository
	 * @param refName
	 *            name of a commit which should be merged
	 */
//	public MergeOperation( Repository repository,String refName) {
//		this.repository = repository;
//		this.refName = refName;
//		this.mergeStrategy = Activator.getDefault().getPreferredMergeStrategy();
//	}

	/**
	 * Create a MergeOperation object
	 *
	 * @param repository
	 * @param refName
	 *            name of a commit which should be merged
	 * @param mergeStrategyName
	 *            the strategy to use for merge. If not registered, the default
	 *            merge strategy according to preferences will be used.
	 */
	public MergeOperation( Repository repository,String refName,
			 String mergeStrategyName) {
		this.repository = repository;
		this.refName = refName;
		MergeStrategy strategy = null;
		strategy = MergeStrategy.get(mergeStrategyName);
		this.mergeStrategy=strategy;
//		this.mergeStrategy = strategy != null ? strategy : Activator.getDefault()
//				.getPreferredMergeStrategy();
	}

	/**
	 * @param squash true to squash merge commits
	 */
	public void setSquash(boolean squash) {
		this.squash = Boolean.valueOf(squash);
	}

	/**
	 * @param ffmode set the fast forward mode
	 * @since 3.0
	 */
	public void setFastForwardMode(FastForwardMode ffmode) {
		this.fastForwardMode = ffmode;
	}

	/**
	 * @param commit
	 *            set the commit option
	 * @since 3.1
	 */
	public void setCommit(boolean commit) {
		this.commit = Boolean.valueOf(commit);
	}

	/**
	 * Set the commit message to be used for the merge commit (in case one is
	 * created)
	 *
	 * @param message
	 *            the message to be used for the merge commit
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public void execute() throws CoreException {
		if (mergeResult != null)
			throw new CoreException("Operation has already been executed and cannot be executed again");
				
		Git git = new Git(repository);
				MergeCommand merge = git.merge();
				try {
					Ref ref = repository.getRef(refName);
					if (ref != null)
						merge.include(ref);
					else
						merge.include(ObjectId.fromString(refName));
				} catch (IOException e) {
					throw new CoreException("An internal error occurred", e);
				}
				if (fastForwardMode != null)
					merge.setFastForward(fastForwardMode);
				if (commit != null)
					merge.setCommit(commit.booleanValue());
				if (squash != null)
					merge.setSquash(squash.booleanValue());
				if (mergeStrategy != null) {
					merge.setStrategy(mergeStrategy);
				}
				if (message != null)
					merge.setMessage(message);
				try {
					mergeResult = merge.call();
					if (MergeResult.MergeStatus.NOT_SUPPORTED.equals(mergeResult.getMergeStatus()))
						throw new CoreException(mergeResult.toString());
				} catch (NoHeadException e) {
					throw new CoreException("Merge failed: Reference to HEAD does not exist", e);
				} catch (ConcurrentRefUpdateException e) {
					throw new CoreException("Merge failed: Another process is accessing the ref", e);
				} catch (CheckoutConflictException e) {
					mergeResult = new MergeResult(e.getConflictingPaths());
					return;
				} catch (GitAPIException e) {
					throw new CoreException(e.getLocalizedMessage(), e.getCause());
				}
	}

	/**
	 * @return the merge result, or <code>null</code> if this has not been
	 *         executed or if an exception occurred
	 */
	public MergeResult getResult() {
		return this.mergeResult;
	}
}
