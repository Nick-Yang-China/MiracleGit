package com.miracle.apps.git.core.op;

import java.util.Collection;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import com.miracle.apps.git.core.errors.CoreException;

/**
 * Operation of listing remote repository advertised refs.
 */
public class ListRemoteOperation implements GitControlOperation {
	private final Repository localDb;
	
	private final URIish uri;
	
	private final int timeout;
	
	private boolean heads;
	
	private Collection<Ref> remoteRefs;
	
	private CredentialsProvider credentialsProvider;

	private boolean tags;


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
			final int timeout) {
		this.localDb=localDb;
		this.uri=uri;
		this.timeout=timeout;
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
		this.credentialsProvider=credentialsProvider;
	}

	public void setHeads(boolean heads) {
		this.heads = heads;
	}

	public void setTags(boolean tags) {
		this.tags = tags;
	}

	private void checkState() {
		if (remoteRefs == null)
			throw new IllegalStateException(
					"Error occurred during remote repo " +  //$NON-NLS-1$
					"listing, no refs available"); //$NON-NLS-1$
	}

	@Override
	public void execute() throws GitAPIException {
		// TODO Auto-generated method stub
		try {
			Git git = new Git(localDb);
			LsRemoteCommand rc = git.lsRemote();
			rc.setCredentialsProvider(credentialsProvider);
			rc.setRemote(uri.toString()).setTimeout(timeout);
			rc.setHeads(heads).setTags(tags);
			remoteRefs = rc.call();
		} catch (JGitInternalException e) {
			throw new CoreException(e.getMessage());
		} catch (GitAPIException e) {
			throw new CoreException(e.getMessage());
		}
	}
}
