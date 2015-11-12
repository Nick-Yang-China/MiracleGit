package com.miracle.apps.git.core.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import com.miracle.apps.git.core.errors.CoreException;

/**
 * An {@link IFileRevision} for a version of a specified resource in the
 * specified commit (revision).
 */
public class CommitFileRevision extends GitFileRevision{
	private final Repository db;

	private final RevCommit commit;

	private final PersonIdent author;

	private final String path;

	private ObjectId blobId;
	
	public CommitFileRevision(final Repository repo, final RevCommit rc,
			final String path) {
		this(repo, rc, path, null);
	}

	public CommitFileRevision(final Repository repo, final RevCommit rc,
			final String path, final ObjectId blob) {
		super(path);
		db = repo;
		commit = rc;
		author = rc.getAuthorIdent();
		this.path = path;
		blobId = blob;
	}

	public Repository getRepository() {
		return db;
	}

	public String getGitPath() {
		return path;
	}

	public CommitBlobStorage getStorage()
			throws CanceledException, CoreException {
		if (blobId == null)
			blobId = locateBlobObjectId();
		return new CommitBlobStorage(db, path, blobId, commit);
	}

	public long getTimestamp() {
		return author != null ? author.getWhen().getTime() : 0;
	}

	public String getContentIdentifier() {
		return commit.getId().name();
	}

	public String getAuthor() {
		return author != null ? author.getName() : null;
	}

	public String getComment() {
		return commit.getShortMessage();
	}

	@Override
	public String toString() {
		return commit.getId() + ":" + path;  //$NON-NLS-1$
	}

	/**
	 * Get the commit that introduced this file revision.
	 *
	 * @return the commit we most recently noticed this file in.
	 */
	public RevCommit getRevCommit() {
		return commit;
	}

	private ObjectId locateBlobObjectId() throws CoreException {
		try {
			final TreeWalk w = TreeWalk.forPath(db, path, commit.getTree());
			if (w == null)
				throw new CoreException("Path "+path+" not in commit "+commit.getId().name()+".");
			return w.getObjectId(0);
		} catch (IOException e) {
			throw new CoreException("IO error looking up path "+path+" in "+commit.getId().name()+".");
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((author == null) ? 0 : author.hashCode());
		result = prime * result + ((blobId == null) ? 0 : blobId.hashCode());
		result = prime * result + ((commit == null) ? 0 : commit.hashCode());
		result = prime * result + ((db == null) ? 0 : db.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CommitFileRevision other = (CommitFileRevision) obj;
		if (author == null) {
			if (other.author != null)
				return false;
		} else if (!author.equals(other.author))
			return false;
		if (blobId == null) {
			if (other.blobId != null)
				return false;
		} else if (!blobId.equals(other.blobId))
			return false;
		if (commit == null) {
			if (other.commit != null)
				return false;
		} else if (!commit.equals(other.commit))
			return false;
		if (db == null) {
			if (other.db != null)
				return false;
		} else if (!db.equals(other.db))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}
}
