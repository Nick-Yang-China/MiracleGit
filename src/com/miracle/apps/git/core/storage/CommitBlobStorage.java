package com.miracle.apps.git.core.storage;

import java.io.File;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

public class CommitBlobStorage extends GitBlobStorage {

	private final RevCommit commit;

	/**
	 *
	 * @param repository
	 *            from with blob version should be taken
	 * @param fileName
	 *            name of blob file
	 * @param blob
	 *            blob id
	 * @param commit
	 *            from with blob version should be taken
	 */
	public CommitBlobStorage(final Repository repository, final String fileName,
			final ObjectId blob, RevCommit commit) {
		super(repository, fileName, blob);
		this.commit = commit;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		return prime * super.hashCode()
				+ ((commit == null) ? 0 : commit.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		CommitBlobStorage other = (CommitBlobStorage) obj;
		if (commit == null) {
			if (other.commit != null)
				return false;
		} else if (!commit.equals(other.commit))
			return false;
		return true;
	}


}
