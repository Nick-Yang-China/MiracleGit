package com.miracle.apps.git.core.op;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FileUtils;

import com.miracle.apps.git.core.errors.CoreException;

/**
 * Clones a repository from a remote location to a local location.
 */
public class CloneOperation implements GitControlOperation {
	private final URIish uri;

	private final boolean allSelected;

	private boolean cloneSubmodules;

	private Collection<Ref> selectedBranches;

	private final File workdir;

	private final File gitdir;

	private String refName;

	private String remoteName;

	private int timeout;

	private CredentialsProvider credentialsProvider;
	
	/**
	 * Create a new clone operation.
	 *
	 * @param uri
	 *            remote we should fetch from.
	 * @param allSelected
	 *            true when all branches have to be fetched (indicates wildcard
	 *            in created fetch refspec), false otherwise.
	 * @param workdir
	 *            working directory to clone to. The directory may or may not
	 *            already exist.
	 */
	public CloneOperation(final URIish uri, final boolean allSelected, final File workdir) {
		this.uri = uri;
		this.allSelected = allSelected;
		this.workdir = workdir;
		this.gitdir = new File(workdir, Constants.DOT_GIT);
	}
	

	public void setSelectedBranches(Collection<Ref> selectedBranches) {
		this.selectedBranches = selectedBranches;
	}


	public void setRefName(String refName) {
		this.refName = refName;
	}


	public void setRemoteName(String remoteName) {
		this.remoteName = remoteName;
	}


	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}


	/**
	 * Create a new clone operation.
	 *
	 * @param uri
	 *            remote we should fetch from.
	 * @param allSelected
	 *            true when all branches have to be fetched (indicates wildcard
	 *            in created fetch refspec), false otherwise.
	 * @param selectedBranches
	 *            collection of branches to fetch. Ignored when allSelected is
	 *            true.
	 * @param workdir
	 *            working directory to clone to. The directory may or may not
	 *            already exist.
	 * @param refName
	 *            name of ref (usually tag or branch) to be checked out after
	 *            clone, e.g. full <code>refs/heads/master</code> or short
	 *            <code>v3.1.0</code>, or null for no checkout
	 * @param remoteName
	 *            name of created remote config as source remote (typically
	 *            named "origin").
	 * @param timeout
	 *            timeout in seconds
	 */
	public CloneOperation(final URIish uri, final boolean allSelected,
			final Collection<Ref> selectedBranches, final File workdir,
			final String refName, final String remoteName, int timeout) {
		this.uri = uri;
		this.allSelected = allSelected;
		this.selectedBranches = selectedBranches;
		this.workdir = workdir;
		this.gitdir = new File(workdir, Constants.DOT_GIT);
		this.refName = refName;
		this.remoteName = remoteName;
		this.timeout = timeout;
	}

	/**
	 * Sets a credentials provider
	 * @param credentialsProvider
	 */
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * @param cloneSubmodules
	 *            true to initialize and update submodules
	 */
	public void setCloneSubmodules(boolean cloneSubmodules) {
		this.cloneSubmodules = cloneSubmodules;
	}


	/**
	 * @return The git directory which will contain the repository
	 */
	public File getGitDir() {
		return gitdir;
	}


	@Override
	public void execute() throws GitAPIException {
		Repository repository = null;
		try {
			CloneCommand cloneRepository = Git.cloneRepository();
			cloneRepository.setCredentialsProvider(credentialsProvider);
			if (refName != null)
				cloneRepository.setBranch(refName);
//			else
//				cloneRepository.setNoCheckout(true);
			cloneRepository.setDirectory(workdir);
			cloneRepository.setRemote(remoteName);
			cloneRepository.setURI(uri.toString());
			cloneRepository.setTimeout(timeout);
			cloneRepository.setCloneAllBranches(allSelected);
			cloneRepository.setCloneSubmodules(cloneSubmodules);
			if (selectedBranches != null) {
				List<String> branches = new ArrayList<String>();
				for (Ref branch : selectedBranches)
					branches.add(branch.getName());
				cloneRepository.setBranchesToClone(branches);
			}
			Git git = cloneRepository.call();
			repository = git.getRepository();
		} catch (final Exception e) {
			try {
				if (repository != null)
					repository.close();
				FileUtils.delete(workdir, FileUtils.RECURSIVE);
			} catch (IOException ioe) {
				throw new CoreException("Clone operation failed, with failed cleanup");
			}
		} finally {
				repository.close();
		}
	}
}
