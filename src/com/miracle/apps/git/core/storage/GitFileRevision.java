package com.miracle.apps.git.core.storage;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import com.miracle.apps.git.core.errors.CoreException;

/**
 * A Git related {@link IFileRevision}. It references a version and a resource,
 * i.e. the version we think corresponds to the resource in specific version.
 */
public class GitFileRevision {
	/** Content identifier for the working copy. */
	public static final String WORKSPACE = "Workspace";  //$NON-NLS-1$

	/** Content identifier for the working tree version.
	/* Used to access non workspace files */
	public static final String WORKING_TREE = "Working Tree";  //$NON-NLS-1$

	/** Content identifier for the content staged in the index. */
	public static final String INDEX = "Index";  //$NON-NLS-1$

	/**
	 * Obtain a file revision for a specific blob of an existing commit.
	 *
	 * @param db
	 *            the repository this commit was loaded out of, and that this
	 *            file's blob should also be reachable through.
	 * @param commit
	 *            the commit the blob was identified to be within.
	 * @param path
	 *            path within the commit's tree of the file.
	 * @param blobId
	 *            unique name of the content.
	 * @return revision implementation for this file in the given commit.
	 */
	public static GitFileRevision inCommit(final Repository db,
			final RevCommit commit, final String path, final ObjectId blobId) {
		return new CommitFileRevision(db, commit, path, blobId);
	}

	/**
	 * @param db
	 *            the repository which contains the index to use.
	 * @param path
	 *            path of the resource in the index
	 * @return revision implementation for the given path in the index
	 */
	public static GitFileRevision inIndex(final Repository db, final String path) {
		return new IndexFileRevision(db, path);
	}

	/**
	 * @param db
	 *            the repository which contains the index to use.
	 * @param path
	 *            path of the resource in the index
	 * @param stage
	 *            stage of the index entry to get, use one of the
	 *            {@link DirCacheEntry} constants (e.g.
	 *            {@link DirCacheEntry#STAGE_2})
	 * @return revision implementation for the given path in the index
	 */
	public static GitFileRevision inIndex(final Repository db,
			final String path, int stage) {
		return new IndexFileRevision(db, path, stage);
	}

	private final String path;

	GitFileRevision(final String path) {
		this.path = path;
	}

	public String getName() {
		final int last = path.lastIndexOf('/');
		return last >= 0 ? path.substring(last + 1) : path;
	}

	public boolean isPropertyMissing() {
		return false;
	}

	public URI getURI() {
		try {
			return new URI(null, null, path, null);
		} catch (URISyntaxException e) {
			return null;
		}
	}
}
