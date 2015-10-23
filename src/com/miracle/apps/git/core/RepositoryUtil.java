package com.miracle.apps.git.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.errors.UnmergedPathException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FileUtils;

/**
 * Utility class for handling Repositories.
 */
public class RepositoryUtil {

	private  Repository repository;

	private  String workdirPrefix;
	
	private  File gitDir;
	
	public RepositoryUtil(String workDir) {
		this(new File(workDir,Constants.DOT_GIT));
	}
	
	public RepositoryUtil(File gitDir) {
		this.gitDir=gitDir;
		this.repository=this.createLocalRepositoryByGitDir(gitDir);
	}
	
	public RepositoryUtil(Repository repository) {
		this.repository=repository;
	}
	
	private Repository createLocalRepositoryByGitDir(File gitDir){
		Repository tempRepo = null;
		 try {
			tempRepo = new FileRepositoryBuilder().findGitDir().readEnvironment().setGitDir(gitDir).build();
			workdirPrefix = tempRepo.getWorkTree().getAbsolutePath();
			workdirPrefix = workdirPrefix.replace('\\', '/');
			if (!workdirPrefix.endsWith("/")) //$NON-NLS-1$
				workdirPrefix += "/"; //$NON-NLS-1$
			
			if(tempRepo.getObjectDatabase().exists()){
				 return tempRepo; 
			 }else{
				 tempRepo.create(false);
			 }
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return tempRepo;
	}
	
	public File getGitDir(){
		return this.repository.getDirectory();
	}

	/**
	 * close repository
	 */
	public void dispose() {
		if (repository != null) {
			repository.close();
			repository = null;
		}
	}

	
	/**
	 * @return the repository
	 */
	public Repository getRepository() {
		return repository;
	}
	/**
	 * @return The default repository directory as configured in the
	 *         preferences, with variables substituted. Returns workspace
	 *         location if there was an error during substitution.
	 */
	public String getLocalRepositoryDir() {
		return workdirPrefix;
	}
	
	/**
	 * Appends content to end of given file.
	 *
	 * @param file
	 * @param content
	 * @throws IOException
	 */
	public void appendFileContent(File file, byte[] content) throws IOException {
		appendFileContent(file, new String(content, "UTF-8"), true);
	}

	/**
	 * Appends content to end of given file.
	 *
	 * @param file
	 * @param content
	 * @throws IOException
	 */
	public void appendFileContent(File file, String content) throws IOException {
		appendFileContent(file, content, true);
	}

	/**
	 * Appends content to given file.
	 *
	 * @param file
	 * @param content
	 * @param append
	 *            if true, then bytes will be written to the end of the file
	 *            rather than the beginning
	 * @throws IOException
	 */
	public void appendFileContent(File file, byte[] content, boolean append)
			throws IOException {
		appendFileContent(file, new String(content, "UTF-8"), append);
	}

	/**
	 * Appends content to given file.
	 *
	 * @param file
	 * @param content
	 * @param append
	 *            if true, then bytes will be written to the end of the file
	 *            rather than the beginning
	 * @throws IOException
	 */
	public void appendFileContent(File file, String content, boolean append)
			throws IOException {
		Writer fw = null;
		try {
			fw = new OutputStreamWriter(new FileOutputStream(file, append),
					"UTF-8");
			fw.append(content);
		} finally {
			if (fw != null)
				fw.close();
		}
	}

	/**
	 * Checks if a file with the given path exists in the HEAD tree
	 *
	 * @param path
	 * @return true if the file exists
	 * @throws IOException
	 */
	public boolean inHead(String path) throws IOException {
		ObjectId headId = repository.resolve(Constants.HEAD);
		try (RevWalk rw = new RevWalk(repository);
				TreeWalk tw = TreeWalk.forPath(repository, path,
						rw.parseTree(headId))) {
			return tw != null;
		}
	}

	public boolean inIndex(String absolutePath) throws IOException {
		return getDirCacheEntry(absolutePath) != null;
	}

	public boolean removedFromIndex(String absolutePath) throws IOException {
		DirCacheEntry dc = getDirCacheEntry(absolutePath);
		if (dc == null)
			return true;
		Ref ref = repository.getRef(Constants.HEAD);
		try (RevWalk rw = new RevWalk(repository)) {
			RevCommit c = rw.parseCommit(ref.getObjectId());

			try (TreeWalk tw = TreeWalk.forPath(repository,
					getRepoRelativePath(absolutePath), c.getTree())) {
				return tw == null || dc.getObjectId().equals(tw.getObjectId(0));
			}
		}
	}

	public long lastModifiedInIndex(String path) throws IOException {
		String repoPath = getRepoRelativePath(path);
		DirCache dc = DirCache.read(repository.getIndexFile(), repository.getFS());

		return dc.getEntry(repoPath).getLastModified();
	}

	public int getDirCacheEntryLength(String path) throws IOException {
		String repoPath = getRepoRelativePath(path);
		DirCache dc = DirCache.read(repository.getIndexFile(), repository.getFS());

		return dc.getEntry(repoPath).getLength();
	}
	
	public String getRepoRelativePath(String path) {
		if(path.contains("\\")){
			path=path.replace("\\", "/");
		}
		final int pfxLen = workdirPrefix.length();
		final int pLen = path.length();
		if (pLen > pfxLen)
			return path.substring(pfxLen);
		else if (path.length() == pfxLen - 1)
			return ""; //$NON-NLS-1$
		return null;
	}

	public Collection<String> getRepoRelativePathwithMulitPaths(Collection<String> paths) {
		ArrayList<String> list=new ArrayList<String>();
		if(paths!=null){
			for(String path:paths){
				if(path.contains("\\")){
					path=path.replace("\\", "/");
				}
				final int pfxLen = workdirPrefix.length();
				final int pLen = path.length();
				if (pLen > pfxLen)
					list.add(path.substring(pfxLen));
//				else if (path.length() == pfxLen - 1)
//					return ""; //$NON-NLS-1$
//				}
			}
	            
		}else{
			return null;
		}
		return list;
	}
	
	public URIish getUri() throws URISyntaxException {
		return new URIish("file:///" + repository.getDirectory().toString());
	}

	private DirCacheEntry getDirCacheEntry(String path) throws IOException {
		String repoPath = getRepoRelativePath(path);
		DirCache dc = DirCache.read(repository.getIndexFile(), repository.getFS());
		return dc.getEntry(repoPath);
	}
	
	public boolean removeLocalRepository(Repository repository){
		
		File rootDir=repository.getDirectory().getParentFile();
		
		if (rootDir.exists()){
			try {
				FileUtils.delete(rootDir, FileUtils.RECURSIVE | FileUtils.RETRY);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
		
		return false;
	}
	
	/**
	 * create an initial commit containing a file "dummy" in the
	 *
	 * @param message
	 *            commit message
	 * @return commit object
	 * @throws IOException
	 * @throws JGitInternalException
	 * @throws GitAPIException
	 * @throws NoFilepatternException
	 */
	public RevCommit createInitialCommit(String message) throws IOException,
			JGitInternalException, NoFilepatternException, GitAPIException {
		String repoPath = repository.getWorkTree().getAbsolutePath();
		File file = new File(repoPath, "dummy");
		if (!file.exists())
			FileUtils.createNewFile(file);
		track(file);
		return commit(message);
	}

	/**
	 * Create a file or get an existing one
	 *
	 * @param project
	 *            instance of project inside with file will be created
	 * @param name
	 *            name of file
	 * @return nearly created file
	 * @throws IOException
	 */
	public File createFile(File project, String name) throws IOException {
		String path = project.getAbsolutePath();
		int lastSeparator = path.lastIndexOf(File.separator);
		FileUtils.mkdirs(new File(path.substring(0, lastSeparator)), true);

		File file = new File(path);
		if (!file.exists())
			FileUtils.createNewFile(file);

		return file;
	}

	/**
	 * Commits the current index
	 *
	 * @param message
	 *            commit message
	 * @return commit object
	 *
	 * @throws NoHeadException
	 * @throws NoMessageException
	 * @throws UnmergedPathException
	 * @throws ConcurrentRefUpdateException
	 * @throws JGitInternalException
	 * @throws GitAPIException
	 * @throws WrongRepositoryStateException
	 */
	public RevCommit commit(String message) throws NoHeadException,
			NoMessageException, UnmergedPathException,
			ConcurrentRefUpdateException, JGitInternalException,
			WrongRepositoryStateException, GitAPIException {
		Git git = new Git(repository);
		CommitCommand commitCommand = git.commit();
		commitCommand.setAuthor("J. Git", "j.git@egit.org");
		commitCommand.setCommitter(commitCommand.getAuthor());
		commitCommand.setMessage(message);
		return commitCommand.call();
	}

	/**
	 * Adds file to version control
	 *
	 * @param file
	 * @throws IOException
	 * @throws GitAPIException
	 * @throws NoFilepatternException
	 */
	public void track(File file) throws IOException, NoFilepatternException, GitAPIException {
		String repoPath = getRepoRelativePath(file.getPath());
		new Git(repository).add().addFilepattern(repoPath).call();
	}


	/**
	 * Removes file from version control
	 *
	 * @param file
	 * @throws IOException
	 */
	public void untrack(File file) throws IOException {
		String repoPath = getRepoRelativePath(file.getPath());
		try {
			new Git(repository).rm().addFilepattern(repoPath).call();
		} catch (GitAPIException e) {
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * Creates a new branch
	 *
	 * @param refName
	 *            starting point for the new branch
	 * @param newRefName
	 * @throws IOException
	 */
	public void createBranch(String refName, String newRefName)
			throws IOException {
		RefUpdate updateRef;
		updateRef = repository.updateRef(newRefName);
		Ref startRef = repository.getRef(refName);
		ObjectId startAt = repository.resolve(refName);
		String startBranch;
		if (startRef != null)
			startBranch = refName;
		else
			startBranch = startAt.name();
		startBranch = Repository.shortenRefName(startBranch);
		updateRef.setNewObjectId(startAt);
		updateRef
				.setRefLogMessage("branch: Created from " + startBranch, false); //$NON-NLS-1$
		updateRef.update();
	}

}
