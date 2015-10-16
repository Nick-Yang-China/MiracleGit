package com.miracle.apps.git.core.op;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.RawParseUtils;

import com.miracle.apps.git.core.errors.CoreException;

/**
 * This class implements the commit of a list of files.
 */
public class CommitOperation implements GitControlOperation {

	Collection<String> commitFileList;

	private boolean commitWorkingDirChanges = false;

	private String author;

	private String committer;

	private String message;

	private boolean amending = false;

	private boolean commitAll = false;

	private Repository repo;

	Collection<String> notTracked;

	private boolean createChangeId;

	private boolean commitIndex;

	RevCommit commit = null;


	/**
	 * @param repository
	 * @param filesToCommit
	 *            a list of files which will be included in the commit
	 * @param notTracked
	 *            a list of all untracked files
	 * @param author
	 *            the author of the commit
	 * @param committer
	 *            the committer of the commit
	 * @param message
	 *            the commit message
	 * @throws CoreException
	 */
	public CommitOperation(Repository repository, Collection<String> filesToCommit, Collection<String> notTracked,
			String author, String committer, String message) throws CoreException {
		this.repo = repository;
		this.author = author;
		this.committer = committer;
		this.message = message;
		if (filesToCommit != null)
			commitFileList = new HashSet<String>(filesToCommit);
		if (notTracked != null)
			this.notTracked = new HashSet<String>(notTracked);
	}

	/**
	 * Constructs a CommitOperation that commits the index
	 * @param repository
	 * @param author
	 * @param committer
	 * @param message
	 * @throws CoreException
	 */
	public CommitOperation(Repository repository, String author, String committer,
			String message) throws CoreException {
		this.repo = repository;
		this.author = author;
		this.committer = committer;
		this.message = message;
		this.commitIndex = true;
	}


//	private void setRepository(File file) throws CoreException {
//		RepositoryMapping mapping = RepositoryMapping.getMapping(file);
//		if (mapping == null)
//			throw new CoreException(Activator.error(NLS.bind(
//					CoreText.CommitOperation_couldNotFindRepositoryMapping,
//					file), null));
//		repo = mapping.getRepository();
//	}

	/**
	 * @param repository
	 */
	public void setRepository(Repository repository) {
		repo = repository;
	}

	private Collection<String> buildFileList(Collection<File> files) throws CoreException {
		Collection<String> result = new HashSet<String>();
		for (File file : files) {
//			RepositoryMapping mapping = RepositoryMapping.getMapping(file);
//			if (mapping == null)
//				throw new CoreException(Activator.error(NLS.bind(CoreText.CommitOperation_couldNotFindRepositoryMapping, file), null));
//			String repoRelativePath = mapping.getRepoRelativePath(file);
			result.add(file.getPath());
		}
		return result;
	}

	@Override
	public void execute() throws GitAPIException {
					commitAll();
	}


	private void addUntracked() throws CoreException {
		if (notTracked == null || notTracked.size() == 0)
			return;
		AddCommand addCommand = new Git(repo).add();
		boolean fileAdded = false;
		for (String path : notTracked)
			if (commitFileList.contains(path)) {
				addCommand.addFilepattern(path);
				fileAdded = true;
			}
		if (fileAdded)
			try {
				addCommand.call();
			} catch (Exception e) {
				throw new CoreException(e.getMessage(), e);
			}
	}


	/**
	 *
	 * @param amending
	 */
	public void setAmending(boolean amending) {
		this.amending = amending;
	}

	/**
	 *
	 * @param commitAll
	 */
	public void setCommitAll(boolean commitAll) {
		this.commitAll = commitAll;
	}

	/**
	 * @param createChangeId
	 *            <code>true</code> if a Change-Id should be inserted
	 */
	public void setComputeChangeId(boolean createChangeId) {
		this.createChangeId = createChangeId;
	}

	/**
	 * @return the newly created commit if committing was successful, null otherwise.
	 */
	public RevCommit getCommit() {
		return commit;
	}

	// TODO: can the commit message be change by the user in case of a merge commit?
	private void commitAll() throws CoreException {

		Git git = new Git(repo);
		try {
			CommitCommand commitCommand = git.commit();
			setAuthorAndCommitter(commitCommand);
			commit = commitCommand.setAll(true).setMessage(message)
					.setInsertChangeId(createChangeId).call();
		} catch (JGitInternalException e) {
			throw new CoreException("An internal error occurred", e);
		} catch (GitAPIException e) {
			throw new CoreException(e.getLocalizedMessage(), e);
		}
	}

	private void setAuthorAndCommitter(CommitCommand commitCommand) throws GitAPIException {
		final Date commitDate = new Date();
		final TimeZone timeZone = TimeZone.getDefault();

		final PersonIdent enteredAuthor = RawParseUtils.parsePersonIdent(author);
		final PersonIdent enteredCommitter = RawParseUtils.parsePersonIdent(committer);
		if (enteredAuthor == null)
			throw new CoreException("The person ident ''{0}'' could not be parsed.");
		if (enteredCommitter == null)
			throw new CoreException("The person ident ''{0}'' could not be parsed.");

		PersonIdent authorIdent;
			authorIdent = new PersonIdent(enteredAuthor, commitDate, timeZone);

		final PersonIdent committerIdent = new PersonIdent(enteredCommitter, commitDate, timeZone);

		commitCommand.setAuthor(authorIdent);
		commitCommand.setCommitter(committerIdent);
	}
}
