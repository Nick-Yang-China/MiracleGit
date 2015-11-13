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
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FileUtils;

import com.miracle.apps.git.core.errors.CoreException;

/**
 * Clones a repository from a remote location to a local location.
 */
public class CloneOperation implements GitControlOperation {
	private final String uri;

	private final boolean allSelected;

	private Collection<String> selectedBranches;

	private final File workdir;

	private final File gitdir;

	private String refName;

	private String remoteName;

	private int timeout;

	private CredentialsProvider credentialsProvider;

	private String status;
	
	private int flag;

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
	 *            
	 * @param username
	 * 
	 * @param password
	 */
	public CloneOperation(final String uri, final boolean allSelected,
			final Collection<String> selectedBranches, final File workdir,
			final String refName, final String remoteName, int timeout,String username,String password) {
		this.uri = uri;
		this.allSelected = allSelected;
		this.selectedBranches = selectedBranches;
		this.workdir = workdir;
		this.gitdir = new File(workdir, Constants.DOT_GIT);
		this.refName = refName;
		this.remoteName = remoteName;
		this.timeout = timeout;
		if(username!=null && password !=null)
		 this.credentialsProvider=new UsernamePasswordCredentialsProvider(username, password);
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
			else
				cloneRepository.setNoCheckout(true);
			cloneRepository.setDirectory(workdir);
			cloneRepository.setRemote(remoteName);
			cloneRepository.setURI(uri.toString());
			cloneRepository.setTimeout(timeout);
			cloneRepository.setCloneAllBranches(allSelected);
			if (selectedBranches != null) {
				cloneRepository.setBranchesToClone(selectedBranches);
			}
			Git git = cloneRepository.call();
			repository = git.getRepository();
			status=repository.getRepositoryState().toString();
			if(!checkIfBranchExists(repository)){
				flag=5;
				deleteLocalGitDir();
			}
			
		} catch (Exception e) {
			deleteLocalGitDir();
			throw new CoreException("Clone operation failed:",e);
		} finally {
			if(repository!=null)
				repository.close();
		}
	}
	
	public String getCloneStatus() {
		return this.status;
	}
	
	private void deleteLocalGitDir(){
		if (workdir.exists()){
			try {
				FileUtils.delete(workdir, FileUtils.RECURSIVE | FileUtils.RETRY);
			} catch (IOException e1) {
				//ignore here
			}
		}
	}
	private boolean checkIfBranchExists(Repository repository){
		try {
			if(repository.getRef(refName)!=null){
				return true;
			}
		} catch (IOException e) {
			return false;
		}
		return false;
	}
	public int getFlag() {
		return flag;
	}
	
}
