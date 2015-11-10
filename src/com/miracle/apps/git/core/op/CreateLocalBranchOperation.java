package com.miracle.apps.git.core.op;

import java.io.IOException;

import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;

import com.miracle.apps.git.core.errors.CoreException;

/**
 * This class implements creation of a local branch based on a commit or another
 * branch
 */
public class CreateLocalBranchOperation implements GitControlOperation {
	private final String name;

	private final Repository repository;

	private final String baseBranchName;

	private final RevCommit commit;

	private UpstreamConfig upstreamConfig;
	
	private boolean checkOutFlag;
	
	private Ref ref;
	
	/**
	 * @param repository
	 * @param name
	 *            the name for the new local branch (without prefix)
	 * @param ref
	 *            the branch or tag to base the new branch upon
	 * @param config
	 *            how to do the upstream configuration
	 */
	public CreateLocalBranchOperation(Repository repository, String name,
			Ref ref, UpstreamConfig config) {
		this.name = name;
		this.repository = repository;
		this.baseBranchName = ref.getName();
		this.commit = null;
		this.upstreamConfig = config;
	}
	
	/**
	 * @param repository
	 * @param name
	 *            the name for the new local branch (without prefix)
	 * @param basebranchname
	 *            the branch name or tag name to base the new branch upon:
	 *            master or refs/heads/master
	 * @throws IOException 
	 */
	public CreateLocalBranchOperation(Repository repository, String newname,
			String basebranchname) throws IOException {
		this.name = newname;
		this.repository = repository;
		this.baseBranchName = basebranchname;
		this.commit = null;
	}

	/**
	 * @param repository
	 * @param name
	 *            the name for the new local branch (without prefix)
	 * @param commit
	 *            a commit to base the new branch upon
	 */
	public CreateLocalBranchOperation(Repository repository, String name,
			RevCommit commit) {
		this.name = name;
		this.repository = repository;
		this.baseBranchName = null;
		this.commit = commit;
		this.upstreamConfig = null;
	}

	@Override
	public void execute() throws CoreException {
				Git git = new Git(repository);
				try {
					if (baseBranchName != null) {
						SetupUpstreamMode mode;
						if (upstreamConfig == UpstreamConfig.NONE)
							mode = SetupUpstreamMode.NOTRACK;
						else
							mode = SetupUpstreamMode.SET_UPSTREAM;
						ref=git.branchCreate().setName(name).setStartPoint(
								baseBranchName).setUpstreamMode(mode).call();
					}
					else
						ref=git.branchCreate().setName(name).setStartPoint(commit)
								.setUpstreamMode(SetupUpstreamMode.NOTRACK)
								.call();
				} catch (Exception e) {
					throw new CoreException(e.getMessage(), e);
				}

				if (UpstreamConfig.REBASE == upstreamConfig) {
					// set "branch.<name>.rebase" to "true"
					StoredConfig config = repository.getConfig();
					config.setBoolean(ConfigConstants.CONFIG_BRANCH_SECTION,
							name, ConfigConstants.CONFIG_KEY_REBASE, true);
					try {
						config.save();
					} catch (IOException e) {
						throw new CoreException(e.getMessage(), e);
					}
				}
				
				if(checkOutFlag){
					if(this.name!=null || this.commit!=null)
					try {
						git.checkout().setName(name).call();
					} catch (GitAPIException e) {
						e.printStackTrace();
					}
				}
				
			}

	public CreateLocalBranchOperation setCheckOutFlag(boolean checkOutFlag) {
		this.checkOutFlag = checkOutFlag;
		return this;
	}

	/**
	 * Describes how to configure the upstream branch
	 */
	public static enum UpstreamConfig {
		/** Rebase */
		REBASE(),
		/** Merge */
		MERGE(),
		/** No configuration */
		NONE();

		/**
		 * Get the default upstream config for the specified repository and
		 * upstream branch ref.
		 *
		 * @param repo
		 * @param upstreamRefName
		 * @return the default upstream config
		 */
		public static UpstreamConfig getDefault(Repository repo,
				String upstreamRefName) {
			String autosetupMerge = repo.getConfig().getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, null,
					ConfigConstants.CONFIG_KEY_AUTOSETUPMERGE);
			if (autosetupMerge == null)
				autosetupMerge = ConfigConstants.CONFIG_KEY_TRUE;
			boolean isLocalBranch = upstreamRefName.startsWith(Constants.R_HEADS);
			boolean isRemoteBranch = upstreamRefName.startsWith(Constants.R_REMOTES);
			if (!isLocalBranch && !isRemoteBranch)
				return NONE;
			boolean setupMerge = autosetupMerge
					.equals(ConfigConstants.CONFIG_KEY_ALWAYS)
					|| (isRemoteBranch && autosetupMerge
							.equals(ConfigConstants.CONFIG_KEY_TRUE));
			if (!setupMerge)
				return NONE;
			String autosetupRebase = repo.getConfig().getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, null,
					ConfigConstants.CONFIG_KEY_AUTOSETUPREBASE);
			if (autosetupRebase == null)
				autosetupRebase = ConfigConstants.CONFIG_KEY_NEVER;
			boolean setupRebase = autosetupRebase
					.equals(ConfigConstants.CONFIG_KEY_ALWAYS)
					|| (autosetupRebase.equals(ConfigConstants.CONFIG_KEY_LOCAL) && isLocalBranch)
					|| (autosetupRebase.equals(ConfigConstants.CONFIG_KEY_REMOTE) && isRemoteBranch);
			if (setupRebase)
				return REBASE;
			return MERGE;
		}
	}
	
	public void setUpstreamConfig(UpstreamConfig upstreamConfig) {
		this.upstreamConfig = upstreamConfig;
	}

	@Override
	public String toString() {
		if(ref!=null){
			return new StringBuffer().append("BranchCreate: ").append(ref.getName()).toString();
		}
		return super.toString();
	}

	
}