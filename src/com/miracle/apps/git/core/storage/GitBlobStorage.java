package com.miracle.apps.git.core.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import com.miracle.apps.git.core.errors.CoreException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.WorkingTreeOptions;
import org.eclipse.jgit.util.io.AutoCRLFInputStream;

/**
 * Provides access to a git blob.
 */
public class GitBlobStorage{
	/** Repository containing the object this storage provides access to. */
	protected final Repository db;

	/** Repository-relative path of the underlying object. */
	protected final String path;

	/** Id of this object in its repository. */
	protected final ObjectId blobId;

	/**
	 * @param repository
	 *            The repository containing this object.
	 * @param path
	 *            Repository-relative path of the underlying object. This path
	 *            is not validated by this class, i.e. it's returned as is by
	 *            {@code #getAbsolutePath()} and {@code #getFullPath()} without
	 *            validating if the blob is reachable using this path.
	 * @param blob
	 *            Id of this object in its repository.
	 */
	public GitBlobStorage(final Repository repository, final String path,
			final ObjectId blob) {
		this.db = repository;
		this.path = path;
		this.blobId = blob;
	}

	public InputStream getContents() throws CoreException {
		try {
			return open();
		} catch (IOException e) {
			throw new CoreException("IO error reading Git blob "+blobId+" with path "+path);
		}
	}

	private InputStream open() throws IOException, CoreException,
			IncorrectObjectTypeException {
		if (blobId == null)
			return new ByteArrayInputStream(new byte[0]);

		try {
			WorkingTreeOptions workingTreeOptions = db.getConfig().get(WorkingTreeOptions.KEY);
			final InputStream objectInputStream = db.open(blobId,
					Constants.OBJ_BLOB).openStream();
			switch (workingTreeOptions.getAutoCRLF()) {
			case INPUT:
				// When autocrlf == input the working tree could be either CRLF or LF, i.e. the comparison
				// itself should ignore line endings.
			case FALSE:
				return objectInputStream;
			case TRUE:
			default:
				return new AutoCRLFInputStream(objectInputStream, true);
			}
		} catch (MissingObjectException notFound) {
			throw new CoreException("Git blob "+blobId+" with path "+path+" not found");
		}
	}


	public String getName() {
		final int last = path.lastIndexOf('/');
		return last >= 0 ? path.substring(last + 1) : path;
	}

	public boolean isReadOnly() {
		return true;
	}

	public int hashCode() {
		return Arrays.hashCode(new Object[] { blobId, db, path });
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GitBlobStorage other = (GitBlobStorage) obj;
		if (blobId == null) {
			if (other.blobId != null)
				return false;
		} else if (!blobId.equals(other.blobId))
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

	/**
	 * Returns the absolute path on disk of the underlying object.
	 * <p>
	 * The returned path may not point to an existing file if the object does
	 * not exist locally.
	 * </p>
	 *
	 * @return The absolute path on disk of the underlying object.
	 */
	public File getAbsolutePath() {
		if (db.isBare()) {
			return null;
		}
		return new File(db.getWorkTree().getAbsolutePath() + File.separatorChar
				+ path);
	}
}
