package com.miracle.apps.git.core.op;

import com.miracle.apps.git.core.errors.CoreException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.RebaseResult.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * This class implements rebase.
 */
public class RebaseOperation implements GitControlOperation {
	private final Repository repository;

	private final Ref ref;

	private final Operation operation;

	private RebaseResult result;

	private final InteractiveHandler handler;

	private boolean preserveMerges = false;

	private MergeStrategy strategy;
	
	/**
	 * Construct a {@link RebaseOperation} object
	 *
	 * @param repository
	 *            the {@link Repository}
	 * @param branch
	 *            the short branch or full branch name
	 *            test or refs/heads/test
	 */

	public RebaseOperation(Repository repository, String branch) throws IOException {
		this(repository, repository.getRef(branch), Operation.BEGIN, null);
	}

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
				} 
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
	
	/**
	 * @param strategy
	 *            to provide the MergeStrategy
	 */
	public void setStrategy(MergeStrategy strategy) {
		this.strategy = strategy;
	}

	@Override
	public String toString() {
		StringBuffer sb=new StringBuffer();
		if(result!=null){
			sb.append("Rebase Result: ");
			Status status=result.getStatus();
			if(status==Status.OK){
				sb.append(status);
			}else if(status==Status.ABORTED){
				sb.append(status);
			}else if(status==Status.STOPPED){
				sb.append(status);
				RevCommit commit=result.getCurrentCommit();
				sb.append("\n"+commit.getId()+"|"+commit.getShortMessage());
			}else if(status==Status.EDIT){
				sb.append(status);
			}else if(status==Status.FAILED){
				sb.append(status);
		    	Map<String,MergeFailureReason> fail=result.getFailingPaths();
		    	for(Map.Entry<String, MergeFailureReason> entry:fail.entrySet()){
		    		sb.append("\n"+entry.getKey()+"-->"+entry.getValue().toString());
		    	}
			}else if(status==Status.UNCOMMITTED_CHANGES){
				sb.append(status);
				List<String> changes=result.getUncommittedChanges();
				sb.append("\nUncommitted Changes: ");
				for(String str:changes){
					sb.append("\n"+str);
				}
			}else if(status==Status.CONFLICTS){
				sb.append(status);
				List<String> conflicts=result.getConflicts();
				sb.append("\nList of Conflicts: ");
				for(String str:conflicts){
					sb.append("\n"+str);
				}
			}else if(status==Status.UP_TO_DATE){
				sb.append(status);
			}else if(status==Status.FAST_FORWARD){
				sb.append(status);
			}else if(status==Status.NOTHING_TO_COMMIT){
				sb.append(status);
			}else if(status==Status.INTERACTIVE_PREPARED){
				sb.append(status);
			}else if(status==Status.STASH_APPLY_CONFLICTS){
				sb.append(status);
			}
			return sb.toString();
		}
		return super.toString();
	}
	
	public RebaseOperation setResult(RebaseResult result) {
		this.result = result;
		return this;
	}
}
