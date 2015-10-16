/*******************************************************************************
 * Copyright (c) 2010, 2015 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler <mathias.kinzler@sap.com> - initial implementation
 *    Laurent Delaigue (Obeo) - use of preferred merge strategy
 *******************************************************************************/
package com.miracle.apps.git.core.op;

import com.miracle.apps.git.core.errors.CoreException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;

/**
 * This class implements rebase.
 */
public class RebaseOperation implements GitControlService {
	private final Repository repository;

	private final Ref ref;

	private final Operation operation;

	private RebaseResult result;

	private final InteractiveHandler handler;

	private boolean preserveMerges = false;

	/**
	 * Construct a {@link RebaseOperation} object for a {@link Ref}.
	 * <p>
	 * Upon {@link #execute(IProgressMonitor)}, the current HEAD will be rebased
	 * onto the provided {@link Ref}
	 *
	 * @param repository
	 *            the {@link Repository}
	 * @param ref
	 *            the branch or tag
	 */
	public RebaseOperation(Repository repository, Ref ref) {
		this(repository, ref, Operation.BEGIN, null);
	}

	/**
	 * Construct a {@link RebaseOperation} object for a {@link Ref}.
	 * <p>
	 * Upon {@link #execute(IProgressMonitor)}, the current HEAD will be rebased
	 * interactively onto the provided {@link Ref}
	 *
	 * @param repository
	 *            the {@link Repository}
	 * @param ref
	 *            the branch or tag
	 * @param handler
	 */
	public RebaseOperation(Repository repository, Ref ref,
			InteractiveHandler handler) {
		this(repository, ref, Operation.BEGIN, handler);
	}

	/**
	 * Used to abort, skip, or continue a stopped rebase operation that has been
	 * started before.
	 *
	 * @param repository
	 *            the {@link Repository}
	 * @param operation
	 *            one of {@link Operation#ABORT}, {@link Operation#CONTINUE},
	 *            {@link Operation#SKIP}
	 */
	public RebaseOperation(Repository repository, Operation operation) {
		this(repository, null, operation, null);
	}

	/**
	 * Used to abort, skip, or continue a stopped rebase interactive operation
	 * that has been started before.
	 *
	 * @param repository
	 *            the {@link Repository}
	 * @param operation
	 *            one of {@link Operation#ABORT}, {@link Operation#CONTINUE},
	 *            {@link Operation#SKIP}
	 * @param handler
	 */
	public RebaseOperation(Repository repository, Operation operation,
			InteractiveHandler handler) {
		this(repository, null, operation, handler);
	}

	private RebaseOperation(Repository repository, Ref ref,
			Operation operation, InteractiveHandler handler) {
		this.repository = repository;
		this.ref = ref;
		this.operation = operation;
		this.handler = handler;
	}

	@Override
	public void execute() throws CoreException {
		if (result != null)
			throw new CoreException("Operation has already been executed and cannot be executed again");

		RebaseCommand cmd = new Git(repository).rebase()
						.setProgressMonitor(NullProgressMonitor.INSTANCE);
				MergeStrategy strategy =MergeStrategy.OURS; //Activator.getDefault().getPreferredMergeStrategy();
				if (strategy != null) {
					cmd.setStrategy(strategy);
				}
				try {
					if (handler != null)
						cmd.runInteractively(handler, true);
					if (operation == Operation.BEGIN) {
						cmd.setPreserveMerges(preserveMerges);
						result = cmd.setUpstream(ref.getName()).call();
					}
					else
						result = cmd.setOperation(operation).call();

				} catch (NoHeadException e) {
					throw new CoreException(e.getMessage(), e);
				} catch (RefNotFoundException e) {
					throw new CoreException(e.getMessage(), e);
				} catch (JGitInternalException e) {
					throw new CoreException(e.getMessage(), e);
				} catch (GitAPIException e) {
					throw new CoreException(e.getMessage(), e);
				} finally {
//					if (refreshNeeded())
//						ProjectUtil.refreshValidProjects(validProjects,
//								new SubProgressMonitor(actMonitor, 1));
				}
	}

	private boolean refreshNeeded() {
		if (result == null)
			return true;
		if (result.getStatus() == RebaseResult.Status.UP_TO_DATE)
			return false;
		return true;
	}

	/**
	 * @return the result of calling {@link #execute(IProgressMonitor)}, or
	 *         <code>null</code> if this has not been executed yet
	 */
	public RebaseResult getResult() {
		return result;
	}

	/**
	 * @return the {@link Repository}
	 */
	public final Repository getRepository() {
		return repository;
	}

	/**
	 * @return the {@link Operation} if it has been set, otherwise null
	 */
	public final Operation getOperation() {
		return operation;
	}

	/**
	 * @param preserveMerges
	 *            true to preserve merges during the rebase
	 */
	public void setPreserveMerges(boolean preserveMerges) {
		this.preserveMerges = preserveMerges;
	}
}
