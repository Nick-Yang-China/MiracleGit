package com.miracle.apps.git.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
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
	
	private  String gitDir;
	
	RepositoryUtil(String gitDir) {
		this.gitDir=gitDir;
	}
	
	public Repository createLocalRepositoryByGitDir(String gitDir){
		 try {
			repository = new FileRepositoryBuilder().findGitDir().readEnvironment().setGitDir(new File(gitDir)).build();
			
			if(repository.getObjectDatabase().exists()){
				
				 return repository; 
			 }else{
				 
				 repository.create(false);
			 }
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		workdirPrefix = repository.getWorkTree().getAbsolutePath();
		workdirPrefix = workdirPrefix.replace('\\', '/');
		if (!workdirPrefix.endsWith("/")) //$NON-NLS-1$
			workdirPrefix += "/"; //$NON-NLS-1$
		
		
		return repository;
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
			path.replace("\\", "/");
		}
		final int pfxLen = workdirPrefix.length();
		final int pLen = path.length();
		if (pLen > pfxLen)
			return path.substring(pfxLen);
		else if (path.length() == pfxLen - 1)
			return ""; //$NON-NLS-1$
		return null;
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
		
		File rootDir=repository.getDirectory();
		
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

}
