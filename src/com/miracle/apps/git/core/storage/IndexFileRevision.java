package com.miracle.apps.git.core.storage;

import java.io.IOException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

import com.miracle.apps.git.core.errors.CoreException;

/** An {@link IFileRevision} for the version in the Git index. */
public class IndexFileRevision extends GitFileRevision {

	// This is to maintain compatibility with the old behavior
	private static final int FIRST_AVAILABLE = -1;

	private final Repository db;

	private final String path;

	private final int stage;

	private ObjectId blobId;

	public IndexFileRevision(final Repository repo, final String path) {
		this(repo, path, FIRST_AVAILABLE);
	}

	public IndexFileRevision(final Repository repo, final String path, int stage) {
		super(path);
		this.db = repo;
		this.path = path;
		this.stage = stage;
	}

	public IndexBlobStorage getStorage() throws CoreException {
		if (blobId == null)
			blobId = locateBlobObjectId();
		return new IndexBlobStorage(db, path, blobId);
	}

	@Override
	public boolean isPropertyMissing() {
		return false;
	}

	public String getAuthor() {
		return "";  //$NON-NLS-1$
	}

	public long getTimestamp() {
		return -1;
	}

	public String getComment() {
		return null;
	}

	public String getContentIdentifier() {
		return INDEX;
	}

	private ObjectId locateBlobObjectId() throws CoreException {
		try {
			DirCache dc = db.readDirCache();
			int firstIndex = dc.findEntry(path);
			if (firstIndex < 0)
				return null;

			// Try to avoid call to nextEntry if first entry already matches
			DirCacheEntry firstEntry = dc.getEntry(firstIndex);
			if (stage == FIRST_AVAILABLE || firstEntry.getStage() == stage)
				return firstEntry.getObjectId();

			// Ok, we have to search
			int nextIndex = dc.nextEntry(firstIndex);
			for (int i = firstIndex; i < nextIndex; i++) {
				DirCacheEntry entry = dc.getEntry(i);
				if (entry.getStage() == stage)
					return entry.getObjectId();
			}
			return null;
		} catch (IOException e) {
			throw new CoreException("IO error looking up path "+path+" in index.");
		}
	}

	public Repository getRepository() {
		return db;
	}

	public String getGitPath() {
		return path;
	}
}
