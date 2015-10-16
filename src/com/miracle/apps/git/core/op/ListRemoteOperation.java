package com.miracle.apps.git.core.op;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

/**
 * Operation of listing remote repository advertised refs.
 */
public class ListRemoteOperation {
	private final LsRemoteCommand rc;

	private Collection<Ref> remoteRefs;

	/**
	 * Create listing operation for specified local repository (needed by
	 * transport) and remote repository URI.
	 *
	 * @param localDb
	 *            local repository (needed for transport) where fetch would
	 *            occur.
	 * @param uri
	 *            URI of remote repository to list.
	 * @param timeout
	 *            timeout is seconds; 0 means no timeout
	 */
	public ListRemoteOperation(final Repository localDb, final URIish uri,
			int timeout) {
		Git git = new Git(localDb);
		rc = git.lsRemote();
		rc.setRemote(uri.toString()).setTimeout(timeout);
	}

	/**
	 * @return collection of refs advertised by remote side.
	 * @throws IllegalStateException
	 *             if error occurred during earlier remote refs listing.
	 */
	public Collection<Ref> getRemoteRefs() {
		checkState();
		return remoteRefs;
	}

	/**
	 * @param refName
	 *            remote ref name to search for.
	 * @return ref with specified refName or null if not found.
	 * @throws IllegalStateException
	 *             if error occurred during earlier remote refs listing.
	 */
	public Ref getRemoteRef(final String refName) {
		checkState();
		for (Ref r: remoteRefs)
			if (r.getName().equals(refName))
				return r;
		return null;
	}

	/**
	 * Sets a credentials provider
	 * @param credentialsProvider
	 */
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		rc.setCredentialsProvider(credentialsProvider);
	}

	/**
	 * @param pm
	 *            the monitor to be used for reporting progress and responding
	 *            to cancellation. The monitor is never <code>null</code>
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	public void run() throws InvocationTargetException,
			InterruptedException {
		try {
			remoteRefs = rc.call();
		} catch (JGitInternalException e) {
			throw new InvocationTargetException(e);
		} catch (GitAPIException e) {
			throw new InvocationTargetException(e);
		}
	}

	private void checkState() {
		if (remoteRefs == null)
			throw new IllegalStateException(
					"Error occurred during remote repo " +  //$NON-NLS-1$
					"listing, no refs available"); //$NON-NLS-1$
	}
}
