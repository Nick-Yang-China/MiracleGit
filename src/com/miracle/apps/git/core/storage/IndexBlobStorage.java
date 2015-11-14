package com.miracle.apps.git.core.storage;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * Blob Storage related to a file in the index. Method <code>getFullPath</code>
 * returns a path of format <repository name>/<file path> index
 *
 * @see CommitBlobStorage
 *
 */
public class IndexBlobStorage extends GitBlobStorage {

	IndexBlobStorage(final Repository repository, final String fileName,
			final ObjectId blob) {
		super(repository, fileName, blob);
	}
}
