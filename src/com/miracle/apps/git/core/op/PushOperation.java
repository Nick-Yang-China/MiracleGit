package com.miracle.apps.git.core.op;

import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;

import com.miracle.apps.git.core.errors.CoreException;

/**
 * Push operation: pushing from local repository to one or many remote ones.
 */
public class PushOperation implements GitControlOperation{

	private final Repository localDb;

	private final PushOperationSpecification specification;

	private final boolean dryRun;

	private final String remoteName;

	private final int timeout;

	private OutputStream out;

	private PushOperationResult operationResult;

	private CredentialsProvider credentialsProvider;

	private List<RefSpec> specs;

	/**
	 * Create push operation for provided specification.
	 *
	 * @param localDb
	 *            local repository.
	 * @param specification
	 *            specification of ref updates for remote repositories.
	 * @param dryRun
	 *            true if push operation should just check for possible result
	 *            and not really update remote refs, false otherwise - when push
	 *            should act normally.
	 * @param timeout
	 *            the timeout in seconds (0 for no timeout)
	 */
	public PushOperation(final Repository localDb,
			final PushOperationSpecification specification,
			final boolean dryRun, int timeout) {
		this(localDb, null, specification,null, dryRun, timeout);
	}

	/**
	 * Creates a push operation for a remote configuration.
	 *
	 * @param localDb
	 * @param remoteName
	 * @param specs
	 * 			 The ref specs to be used in the push operation
	 * @param dryRun
	 * @param timeout
	 */
	public PushOperation(final Repository localDb, final String remoteName,List<RefSpec> specs,
			final boolean dryRun, int timeout) {
		this(localDb, remoteName, null,specs, dryRun, timeout);
	}

	private PushOperation(final Repository localDb, final String remoteName,
			PushOperationSpecification specification,List<RefSpec> specs, final boolean dryRun,
			int timeout) {
		this.localDb = localDb;
		this.specification = specification;
		this.dryRun = dryRun;
		this.remoteName = remoteName;
		this.timeout = timeout;
		this.specs=specs;
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
	 * @return push operation result
	 */
	public PushOperationResult getOperationResult() {
		if (operationResult == null)
			throw new IllegalStateException("Operation has not yet been executed and cannot return a result");
		return operationResult;
	}

	/**
	 * @return operation specification, as provided in constructor (may be
	 *         <code>null</code>)
	 */
	public PushOperationSpecification getSpecification() {
		return specification;
	}

	private URIish getPushURIForErrorHandling() {
		RemoteConfig rc = null;
		try {
			rc = new RemoteConfig(localDb.getConfig(), remoteName);
			return rc.getPushURIs().isEmpty() ? rc.getURIs().get(0) : rc
					.getPushURIs().get(0);
		} catch (URISyntaxException e) {
			// should not happen
			return null;
		}
	}

	/**
	 * Sets the output stream this operation will write sideband messages to.
	 *
	 * @param out
	 *            the outputstream to write to
	 * @since 3.0
	 */
	public void setOutputStream(OutputStream out) {
		this.out = out;
	}

	@Override
	public void execute() throws GitAPIException {
		if (operationResult != null)
			throw new IllegalStateException("Operation has already been executed and cannot be executed again");

		if (this.specification != null)
			for (URIish uri : this.specification.getURIs()) {
				for (RemoteRefUpdate update : this.specification.getRefUpdates(uri))
					if (update.getStatus() != Status.NOT_ATTEMPTED)
						throw new IllegalStateException("The RemoteRefUpdate instance cannot be re-used");
			}

		operationResult = new PushOperationResult();
		Git git = new Git(localDb);

		if (specification != null){
			for (final URIish uri : specification.getURIs()) {
					Collection<RemoteRefUpdate> refUpdates = specification.getRefUpdates(uri);

					try {
						Transport transport = Transport.open(localDb, uri);
						transport.setDryRun(dryRun);
						transport.setTimeout(timeout);
						if (credentialsProvider != null)
							transport.setCredentialsProvider(credentialsProvider);
						PushResult result = transport.push(NullProgressMonitor.INSTANCE, refUpdates, out);

						operationResult.addOperationResult(result.getURI(), result);
						specification.addURIRefUpdates(result.getURI(), result.getRemoteUpdates());
					} catch (JGitInternalException e) {
						String errorMessage = e.getCause() != null ? e
								.getCause().getMessage() : e.getMessage();
						String userMessage = "An internal Exception occurred during push: "+errorMessage;
						operationResult.addOperationResult(uri, userMessage);
						throw new CoreException(userMessage, e);
					} catch (Exception e) {
						operationResult.addOperationResult(uri, e.getMessage());
						throw new CoreException(e.getMessage(), e);
					}

				} 
		}
		else {
			try {
				Iterable<PushResult> results = git.push().setRemote(
						remoteName).setDryRun(dryRun).setTimeout(timeout)
						.setCredentialsProvider(credentialsProvider)
						.setOutputStream(out).setRefSpecs(specs).call();
				for (PushResult result : results) {
					operationResult.addOperationResult(result.getURI(), result);
				}
			} catch (JGitInternalException e) {
				String errorMessage = e.getCause() != null ? e.getCause()
						.getMessage() : e.getMessage();
				String userMessage = "An internal Exception occurred during push: "+errorMessage;
				URIish uri = getPushURIForErrorHandling();
				operationResult.addOperationResult(uri, userMessage);
				throw new CoreException(userMessage, e);
			} catch (Exception e) {
				URIish uri = getPushURIForErrorHandling();
				operationResult.addOperationResult(uri, e.getMessage());
				throw new CoreException(e.getMessage(), e);
			}
		}
	}
}
