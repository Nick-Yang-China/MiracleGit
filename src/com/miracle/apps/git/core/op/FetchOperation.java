package com.miracle.apps.git.core.op;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.TagOpt;
import org.eclipse.jgit.transport.URIish;

/**
 * Used to fetch from another Repository
 */
public class FetchOperation {
	private final Repository repository;

	private final RemoteConfig rc;

	private final URIish uri;

	private final int timeout;

	private final List<RefSpec> specs;

	private final boolean dryRun;

	private FetchResult operationResult;

	private CredentialsProvider credentialsProvider;

	private TagOpt tagOpt;

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
	 * @param monitor
	 * @throws InvocationTargetException
	 */
	public void run() throws InvocationTargetException {
		if (operationResult != null)
			throw new IllegalStateException("Operation has already been executed and cannot be executed again");

//		IProgressMonitor actMonitor = monitor;
//		if (actMonitor == null)
//			actMonitor = new NullProgressMonitor();
//		EclipseGitProgressTransformer gitMonitor = new EclipseGitProgressTransformer(
//				actMonitor);
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
			operationResult = command.call();
		} catch (JGitInternalException e) {
			throw new InvocationTargetException(e.getCause() != null ? e
					.getCause() : e);
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		}
	}

	/**
	 * @return the result, or <code>null</code> if the operation has not been
	 *         executed
	 */
	public FetchResult getOperationResult() {
		return operationResult;
	}
}
