package com.miracle.apps.git.core.op;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

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
	public MergeOperation( Repository repository,String refName) {
		this.repository = repository;
		this.refName = refName;
		this.mergeStrategy =null;
	}

	/**
	 * Create a MergeOperation object
	 *
	 * @param repository
	 * @param refName
	 *            name of a commit which should be merged
	 * @param mergeStrategyName
	 *            the strategy to use for merge. If not registered, the default
	 *            merge strategy according to preferences will be used.
	 *            OURS;THEIRS;SIMPLE_TWO_WAY_IN_CORE;RESOLVE;RECURSIVE
	 */
	public MergeOperation( Repository repository,String refName,
			 String mergeStrategyName) {
		this.repository = repository;
		this.refName = refName;
		MergeStrategy strategy = null;
		strategy = MergeStrategy.get(mergeStrategyName);
		this.mergeStrategy=strategy;
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

	@Override
	public String toString() {
		StringBuffer sb=new StringBuffer();
		if(mergeResult!=null){
			sb.append("Merge Result:");
			MergeStatus status=mergeResult.getMergeStatus();
		    if(status==MergeStatus.FAST_FORWARD){
		    	sb.append(status);
		    }else if(status==MergeStatus.FAST_FORWARD_SQUASHED){
		    	sb.append(status);
		    }else if(status==MergeStatus.ALREADY_UP_TO_DATE){
		    	sb.append(status);
		    }else if(status==MergeStatus.ABORTED){
		    	sb.append(status);
		    }else if(status==MergeStatus.FAILED){
		    	sb.append(status);
		    	Map<String,MergeFailureReason> fail=mergeResult.getFailingPaths();
		    	for(Map.Entry<String, MergeFailureReason> entry:fail.entrySet()){
		    		sb.append("\n"+entry.getKey()+"-->"+entry.getValue().toString());
		    	}
		    	
		    }else if(status==MergeStatus.CONFLICTING){
		    	sb.append(status);
		    	 Map<String, int[][]> allConflicts = mergeResult.getConflicts();
		    	 for (String path : allConflicts.keySet()) {
		    	 	int[][] c = allConflicts.get(path);
		    	 	sb.append("\nConflicts in file " + path);
		    	 	for (int i = 0; i < c.length; ++i) {
		    	 		sb.append("\n  Conflict #" + i);
		    	 		for (int j = 0; j < (c[i].length) - 1; ++j) {
		    	 			if (c[i][j] >= 0)
		    	 				sb.append("\n    Chunk for "
		    	 						+ mergeResult.getMergedCommits()[j] + " starts on line #"
		    	 						+ c[i][j]);
		    	 		}
		    	 	}
		    	 }
		    }else if(status==MergeStatus.CHECKOUT_CONFLICT){
		    	sb.append(status);
		    	List<String> list=mergeResult.getCheckoutConflicts();
		    	sb.append("\nConflicts in path:");
		    	for(String str:list){
		    		sb.append("\n"+str);
		    	}
		    }else if(status==MergeStatus.MERGED){
		    	sb.append(status);
		    	sb.append("\nMerge Input:");
		    	ObjectId[] objs=mergeResult.getMergedCommits();
		    	for(ObjectId obj:objs){
		    		try(RevWalk rw=new RevWalk(repository)){
		    			RevCommit commit=rw.parseCommit(obj);
		    			sb.append("\n"+commit.getId()+"|"+commit.getShortMessage());
		    		} catch (IOException e) {
						e.printStackTrace();
					}
		    	}
		    }else if(status==MergeStatus.MERGED_NOT_COMMITTED){
		    	sb.append(status);
		    }else if(status==MergeStatus.MERGED_SQUASHED){
		    	sb.append(status);
		    }else if(status==MergeStatus.MERGED_SQUASHED_NOT_COMMITTED){
		    	sb.append(status);
		    }else if(status==MergeStatus.NOT_SUPPORTED){
		    	sb.append(status);
		    }
			return sb.toString();
		}
		return super.toString();
	}
	
	public MergeOperation setMergeResult(MergeResult mergeResult) {
		this.mergeResult = mergeResult;
		return this;
	}
	
}
