package com.miracle.apps.git.core.op;

import java.util.List;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.miracle.apps.git.core.errors.CoreException;

/**
 * Used to fetch from another Repository
 */
public class FetchOperation implements GitControlOperation {
	private final Repository repository;

	private final RemoteConfig rc;

	private final URIish uri;

	private final int timeout;

	private final List<RefSpec> specs;

	private final boolean dryRun;

	private FetchOperationResult operationResult;

	private CredentialsProvider credentialsProvider;

	private TagOpt tagOpt;
	
	private FetchResult result;

	/**
	 * Constructs a FetchOperation based on URI and RefSpecs
	 *
	 * @param repository
	 * @param uri
	 * @param refSpecs
	 * @param timeout
	 * @param dryRun
	 *
	 */
	public FetchOperation(Repository repository, URIish uri,
			List<RefSpec> refSpecs, int timeout, boolean dryRun) {
		this.repository = repository;
		this.timeout = timeout;
		this.dryRun = dryRun;
		this.uri = uri;
		this.specs = refSpecs;
		this.rc = null;
	}

	/**
	 * Constructs a FetchOperation based on a RemoteConfig
	 *
	 * @param repository
	 * @param config
	 * @param timeout
	 * @param dryRun
	 */
	public FetchOperation(Repository repository, RemoteConfig config,
			int timeout, boolean dryRun) {
		this.repository = repository;
		this.timeout = timeout;
		this.dryRun = dryRun;
		this.uri = null;
		this.specs = null;
		this.rc = config;
	}

	/**
	 * @param credentialsProvider
	 */
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}
	
	/**
	 * @param username
	 * @param password
	 */
	public void setCredentialsProvider(String username,String password) {
		if(username!=null && password!=null)
			this.credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
	}

	/**
	 * @return the operation's credentials provider
	 */
	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	/**
	 * @param tagOpt
	 */
	public void setTagOpt(TagOpt tagOpt) {
		this.tagOpt = tagOpt;
	}

	/**
	 * @return the result, or <code>null</code> if the operation has not been
	 *         executed
	 */
	public FetchOperationResult getOperationResult() {
		return operationResult;
	}

	@Override
	public void execute() throws GitAPIException {
		if (operationResult != null)
			throw new IllegalStateException("Operation has already been executed and cannot be executed again");
		FetchCommand command;
		if (rc == null)
			command = new Git(repository).fetch().setRemote(
					uri.toPrivateString()).setRefSpecs(specs);
		else
			command = new Git(repository).fetch().setRemote(rc.getName());
		command.setCredentialsProvider(credentialsProvider).setTimeout(timeout)
				.setDryRun(dryRun);//.setProgressMonitor(gitMonitor);
		if (tagOpt != null)
			command.setTagOpt(tagOpt);
		try {
			result=command.call();
			operationResult=new FetchOperationResult(result.getURI(), result);
		} catch (JGitInternalException e) {
			throw new CoreException(e.getMessage());
		} catch (Exception e) {
			throw new CoreException(e.getMessage());
		}
	}

	@Override
	public String toString() {
		StringBuffer sb=new StringBuffer();
		if(result!=null){
			sb.append("Fetch Result:"+result.getURI().toString());
			sb.append("\nRemoteTrackingList:");
			for(TrackingRefUpdate trf:result.getTrackingRefUpdates()){
				sb.append("\n"+trf.getRemoteName()+"--->"+trf.getLocalName());
			}
			return sb.toString();
		}
		return super.toString();
	}
	
	public FetchOperation setResult(FetchResult result) {
		this.result = result;
		return this;
	}
	
}
