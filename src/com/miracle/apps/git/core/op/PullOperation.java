package com.miracle.apps.git.core.op;

import com.miracle.apps.git.core.errors.CoreException;
import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 * Wraps the JGit API {@link PullCommand} into an operation
 */
public class PullOperation implements GitControlOperation{
	
	private final Repository repository;

	private PullResult pullResult;

	private final int timeout;

	private CredentialsProvider credentialsProvider;
	
	private MergeStrategy strategy;

	private boolean useRebase;

	private String remoteBranchName;

	/**
	 * @param repositories
	 *            the repository
	 * @param timeout
	 *            in seconds
	 *@param  remoteBranchName
	 *			  The remote branch name to be used for the pull operation
	 */
	public PullOperation(Repository repository, int timeout, String remoteBranchName) {
		this.timeout = timeout;
		this.repository = repository;
		this.remoteBranchName=remoteBranchName;
	}

	@Override
	public void execute() throws CoreException, DetachedHeadException, InvalidConfigurationException {
		if (pullResult!=null)
			throw new CoreException("Operation has already been executed and cannot be executed again");
					PullCommand pull;
					try {
						checkCurrentBranchRemoteSet();
						pull = new Git(repository).pull();
						pull.setTimeout(timeout);
						pull.setCredentialsProvider(credentialsProvider);
						pull.setRebase(useRebase);
						if(remoteBranchName!=null)
							pull.setRemoteBranchName(remoteBranchName);
						if (strategy != null) {
							pull.setStrategy(strategy);
						}
						pullResult = pull.call();
					} catch (DetachedHeadException e) {
						throw new DetachedHeadException("No local branch is currently checked out");
					} catch (InvalidConfigurationException e) {
						throw new InvalidConfigurationException("The current branch is not configured for pull");
					} catch (GitAPIException e) {
						throw new CoreException(e.getMessage());
					} catch (JGitInternalException e) {
						Throwable cause = e.getCause();
						if (cause == null || !(cause instanceof TransportException))
							cause = e;
						throw new CoreException(e.getMessage(), cause);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						throw new CoreException(e.getMessage());
					} 
	}

	/**
	 * @return the PullResult
	 */
	public PullResult getPullResult() {
		return this.pullResult;
	}

	/**
	 * @param username
	 * @param password
	 */
	public void setCredentialsProvider(String username,String password) {
		if(username!=null && password!=null)
			this.credentialsProvider = new UsernamePasswordCredentialsProvider(username,password) ;
	}

	/**
	 * @return the operation's credentials provider
	 */
	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}
	
	/**
	 * 
	 * @param strategy
	 * 		  		MergeStrategy.OURS;
	 *				MergeStrategy.THEIRS;
	 *				MergeStrategy.RECURSIVE;
	 *				MergeStrategy.RESOLVE;
	 *				MergeStrategy.SIMPLE_TWO_WAY_IN_CORE;
	 */
	public void setStrategy(MergeStrategy strategy) {
		this.strategy = strategy;

	}

	public MergeStrategy getStrategy() {
		return strategy;
	}
	
	public void setUseRebase(boolean useRebase) {
		this.useRebase = useRebase;
	}
	
	private void checkCurrentBranchRemoteSet() throws IOException{
		String CurrentBranch=repository.getBranch();
		StoredConfig sc=repository.getConfig();
		String remote=sc.getString(ConfigConstants.CONFIG_BRANCH_SECTION, CurrentBranch, ConfigConstants.CONFIG_KEY_REMOTE);
		if(remote!=null)
			if(remote.equals("."))
				sc.setString(ConfigConstants.CONFIG_BRANCH_SECTION, CurrentBranch, ConfigConstants.CONFIG_KEY_REMOTE, Constants.DEFAULT_REMOTE_NAME);
	}
}
