package com.miracle.apps.git.core.op;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuildIterator;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.RawParseUtils;

import com.miracle.apps.git.core.errors.CoreException;

/**
 * This class implements the commit of a list of files.
 */
public class CommitOperation implements GitControlOperation {

	private Collection<String> commitFileList;

	private String author;

	private String committer;

	private String message;

	private boolean amending = false;

	private boolean commitAll = false;

	private Repository repo;

	private Collection<String> notTracked;

	private boolean createChangeId;

	private boolean commitIndex;

	private RevCommit commit = null;
	
	private static List<String> only = new ArrayList<String>();
	/**
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
	@Deprecated
	public CommitOperation(Collection<String> filesToCommit, Collection<String> notTracked,
			String author, String committer, String message) throws CoreException {
		this.author = author;
		this.committer = committer;
		this.message = message;
		if (filesToCommit != null)
			commitFileList = new HashSet<String>(filesToCommit);
		if (notTracked != null)
			this.notTracked = new HashSet<String>(notTracked);
	}

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


	/**
	 * @param repository
	 */
	@Deprecated
	public void setRepository(Repository repository) {
		this.repo = repository;
	}

	@Override
	public void execute() throws GitAPIException {
		if (commitAll){
			commitAll();
		}
		else if (amending || commitFileList != null
				&& commitFileList.size() > 0 || commitIndex) {
			addUntracked();
			commit();
		} 
	}


	private void addUntracked() throws CoreException {
		if (notTracked == null || notTracked.size() == 0){
			return;
		}
		AddCommand addCommand = new Git(repo).add();
		boolean fileAdded = false;
		for (String path : notTracked){
			
			if (commitFileList.contains(path)) {
				addCommand.addFilepattern(path);
				fileAdded = true;
			}
		}
		if (fileAdded)
			try {
				addCommand.call();
			} catch (Exception e) {
				throw new CoreException(e.getMessage(), e);
			}
	}


	private void commit() throws CoreException {
		Git git = new Git(repo);
		try {
			CommitCommand commitCommand = git.commit();
			setAuthorAndCommitter(commitCommand);
			commitCommand.setAmend(amending)
					.setMessage(message)
					.setInsertChangeId(createChangeId);
			if (!commitIndex)
				for(String path:commitFileList)
					commitCommand.setOnly(path);
			commit = commitCommand.call();
		} catch (JGitInternalException e) {
			System.out.println(JGitText.get().emptyCommit);
//			throw new JGitInternalException(JGitText.get().emptyCommit);
		} catch (Exception e) {
			throw new CoreException("An internal error occurred", e);
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
			throw new CoreException("The enteredAuthor could not be parsed.");
		if (enteredCommitter == null)
			throw new CoreException("The enteredCommitter could not be parsed.");

		final PersonIdent authorIdent = new PersonIdent(enteredAuthor, commitDate, timeZone);

		final PersonIdent committerIdent = new PersonIdent(enteredCommitter, commitDate, timeZone);

		commitCommand.setAuthor(authorIdent);
		commitCommand.setCommitter(committerIdent);
	}
	
	/**
	 * 
	 * @param repository
	 * @return true means the commit have no changes, can't commit it
	 * @throws IOException 
	 * @throws IncorrectObjectTypeException 
	 * @throws AmbiguousObjectException 
	 * @throws RevisionSyntaxException 
	 */
	public static boolean CheckIfNoChangesBeforeCommit(Repository repo) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException{
		boolean emptyCommit = true;
		try (RevWalk rw = new RevWalk(repo)) {
				// determine the current HEAD and the commit it is referring to
				ObjectId headId = repo.resolve(Constants.HEAD + "^{commit}"); //$NON-NLS-1$
				// lock the index
				DirCache index = repo.lockDirCache();
				
				ObjectInserter inserter = null;

				// get DirCacheBuilder for existing index
				DirCacheBuilder existingBuilder = index.builder();

				// get DirCacheBuilder for newly created in-core index to build a
				// temporary index for this commit
				DirCache inCoreIndex = DirCache.newInCore();
				DirCacheBuilder tempBuilder = inCoreIndex.builder();

			try (TreeWalk treeWalk = new TreeWalk(repo)) {
				int dcIdx = treeWalk
						.addTree(new DirCacheBuildIterator(existingBuilder));
				int fIdx = treeWalk.addTree(new FileTreeIterator(repo));
				int hIdx = -1;
				if (headId != null)
					hIdx = treeWalk.addTree(rw.parseTree(headId));
				treeWalk.setRecursive(true);

				while (treeWalk.next()) {
					String path = treeWalk.getPathString();
					// check if current entry's path matches a specified path
					int pos = lookupOnly(path);

					CanonicalTreeParser hTree = null;
					if (hIdx != -1)
						hTree = treeWalk.getTree(hIdx, CanonicalTreeParser.class);

					DirCacheIterator dcTree = treeWalk.getTree(dcIdx,
							DirCacheIterator.class);

					if (pos >= 0) {
						// include entry in commit

						FileTreeIterator fTree = treeWalk.getTree(fIdx,
								FileTreeIterator.class);

						// check if entry refers to a tracked file
						boolean tracked = dcTree != null || hTree != null;
						if (!tracked)
							continue;

							if (emptyCommit
										&& (hTree == null || !hTree.idEqual(fTree)
												|| hTree.getEntryRawMode() != fTree
														.getEntryRawMode()))
									// this is a change
									emptyCommit = false;
							} else {
								// if no file exists on disk, neither add it to
								// index nor to temporary in-core index
	
								if (emptyCommit && hTree != null)
									// this is a change
									emptyCommit = false;
							}

					}
				}finally {
					index.unlock();
				}
			}
		
		return emptyCommit;
	}
	
	/**
	 * Look an entry's path up in the list of paths specified by the --only/ -o
	 * option
	 *
	 * In case the complete (file) path (e.g. "d1/d2/f1") cannot be found in
	 * <code>only</code>, lookup is also tried with (parent) directory paths
	 * (e.g. "d1/d2" and "d1").
	 *
	 * @param pathString
	 *            entry's path
	 * @return the item's index in <code>only</code>; -1 if no item matches
	 */
	private static int lookupOnly(String pathString) {
		String p = pathString;
		while (true) {
			int position = Collections.binarySearch(only, p);
			if (position >= 0)
				return position;
			int l = p.lastIndexOf("/"); //$NON-NLS-1$
			if (l < 1)
				break;
			p = p.substring(0, l);
		}
		return -1;
	}
}
