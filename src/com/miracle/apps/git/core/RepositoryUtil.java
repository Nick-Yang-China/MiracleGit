package com.miracle.apps.git.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.errors.UnmergedPathException;
import org.eclipse.jgit.lib.CheckoutEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.ReflogReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.FileUtils;

/**
 * Utility class for handling Repositories.
 */
public class RepositoryUtil {
	private final Map<String, Map<String, String>> commitMappingCache = new HashMap<String, Map<String, String>>();
	
	private  Repository repository;

	private  String workdirPrefix;
	
	private  File gitDir;
	
	public RepositoryUtil(String workDir) {
		this(new File(workDir,Constants.DOT_GIT));
	}
	
	public RepositoryUtil(File gitDir) {
		this.gitDir=gitDir;
		if(gitDir.exists()){
			this.repository=this.createRepoWithCacheByGitDir(gitDir);
		}else{
			this.repository=this.createLocalRepositoryByGitDir(gitDir);
		}
		this.workdirPrefix=getWorkdirPrefix(this.repository);
	}
	
	public RepositoryUtil(Repository repository) {
		this.repository=repository;
		this.workdirPrefix=getWorkdirPrefix(this.repository);
	}
	
	private Repository createLocalRepositoryByGitDir(File gitDir){
		Repository tempRepo = null;
		 try {
			tempRepo = new FileRepositoryBuilder().findGitDir().readEnvironment().setGitDir(gitDir).build();
			
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
	
	/**
	 * get the repository, meantime add it the RepositoryCache.
	 * @param gitDir
	 * @return Repository 
	 * 			a git repository which comes from the RepositoryCache
	 */
	private Repository createRepoWithCacheByGitDir(File gitDir){
		Repository tempRepo = null;
		try {
			tempRepo=RepositoryCache.open(FileKey.exact(gitDir, FS.DETECTED));
		} catch (RepositoryNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tempRepo;
	}
	
	private String getWorkdirPrefix(Repository repository){
		String tempworkdirPrefix = repository.getWorkTree().getAbsolutePath();
		tempworkdirPrefix = tempworkdirPrefix.replace('\\', '/');
		if (!tempworkdirPrefix.endsWith("/")) //$NON-NLS-1$
			tempworkdirPrefix += "/"; //$NON-NLS-1$
		
		return tempworkdirPrefix;
	}
	
	public File getGitDir(){
		return this.repository.getDirectory();
	}
	
	/**
	 * close the git repository in the RepositoryCache
	 * @param repository
	 */
	public void closeRepositoryInCache(Repository repository){
		if(repository!=null){
			RepositoryCache.close(repository);
		}
	}
	
	/**
	 * clear repositories in RepositoryCache
	 */
	public void clear(){
		RepositoryCache.clear();
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
	
	public String getWorkDirPrefix() {
		return workdirPrefix;
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
	
	
	private DirCacheEntry getDirCacheEntry(String path) throws IOException {
		String repoPath = getRepoRelativePath(path);
		DirCache dc = DirCache.read(repository.getIndexFile(), repository.getFS());
		return dc.getEntry(repoPath);
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
	
	/**
	 * get file of relative path in repo
	 * 
	 * @param path
	 * @return relative path of file
	 */
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
	
	public Collection<String> getRepoRelativePathwithMulitPaths(String... path) {
		return getRepoRelativePathwithMulitPaths(Arrays.asList(path));
	}
	/**
	 * get List of relative paths in repo
	 * 
	 * @param paths fils absolute paths
	 * @return list of relative paths in the repo
	 */
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
	
	/**
	 * Checks if given repository is in the 'detached HEAD' state.
	 *
	 * @return <code>true</code> if the repository is in the 'detached HEAD'
	 *         state, <code>false</code> if it's not or an error occurred
	 * @since 3.2
	 */
	public static boolean isDetachedHead(Repository repository) {
		try {
			return ObjectId.isId(repository.getFullBranch());
		} catch (IOException e) {
		    return false;
		}
	}
	
	/**
	 * Resolve HEAD and parse the commit. Returns null if HEAD does not exist or
	 * could not be parsed.
	 * <p>
	 * Only use this if you don't already have to work with a RevWalk.
	 *
	 * @return the commit or null if HEAD does not exist or could not be parsed.
	 * @since 2.2
	 */
	public static RevCommit parseHeadCommit(Repository repository) {
		try (RevWalk walk = new RevWalk(repository)) {
			Ref head = repository.getRef(Constants.HEAD);
			if (head == null || head.getObjectId() == null)
				return null;

			RevCommit commit = walk.parseCommit(head.getObjectId());
			return commit;
		} catch (IOException e) {
			return null;
		}
	}
	
	/**
	 * Get short branch text for given repository
	 *
	 * @param repository
	 * @return short branch text
	 * @throws IOException
	 */
	public String getShortBranch() throws IOException {
		Ref head = repository.getRef(Constants.HEAD);
		if (head == null || head.getObjectId() == null)
			return "NO-HEAD";

		if (head.isSymbolic())
			return repository.getBranch();

		String id = head.getObjectId().name();
		String ref = mapCommitToRef(id, false);
		if (ref != null)
			return Repository.shortenRefName(ref) + ' ' + id.substring(0, 7);
		else
			return id.substring(0, 7);
	}
	
	/**
	 * Tries to map a commit to a symbolic reference.
	 * <p>
	 * This value will be cached for the given commit ID unless refresh is
	 * specified. The return value will be the full name, e.g.
	 * "refs/remotes/someBranch", "refs/tags/v.1.0"
	 * <p>
	 * Since this mapping is not unique, the following precedence rules are
	 * used:
	 * <ul>
	 * <li>Tags take precedence over branches</li>
	 * <li>Local branches take preference over remote branches</li>
	 * <li>Newer references take precedence over older ones where time stamps
	 * are available. Use commiter time stamp from commit if no stamp can be
	 * found on the tag</li>
	 * <li>If there are still ambiguities, the reference name with the highest
	 * lexicographic value will be returned</li>
	 * </ul>
	 *
	 * @param repository
	 *            the {@link Repository}
	 * @param commitId
	 *            a commit
	 * @param refresh
	 *            if true, the cache will be invalidated
	 * @return the symbolic reference, or <code>null</code> if no such reference
	 *         can be found
	 */
	public String mapCommitToRef(String commitId,boolean refresh) {
		synchronized (commitMappingCache) {

			if (!ObjectId.isId(commitId)) {
				return null;
			}

			try {
				ReflogReader reflogReader = repository.getReflogReader(Constants.HEAD);
				if (reflogReader != null) {
					List<ReflogEntry> lastEntry = reflogReader.getReverseEntries();
					for (ReflogEntry entry : lastEntry) {
						if (entry.getNewId().name().equals(commitId)) {
							CheckoutEntry checkoutEntry = entry.parseCheckout();
							if (checkoutEntry != null) {
								Ref ref = repository.getRef(checkoutEntry.getToBranch());
								if (ref != null) {
									if (ref.getObjectId().getName()
											.equals(commitId))
										return checkoutEntry.getToBranch();
									ref = repository.peel(ref);
								}
								if (ref != null) {
									ObjectId id = ref.getPeeledObjectId();
									if (id != null && id.getName().equals(commitId))
										return checkoutEntry.getToBranch();
								}
							}
						}
					}
				}
			} catch (IOException e) {
				// ignore here
			}

			Map<String, String> cacheEntry = commitMappingCache.get(repository
					.getDirectory().toString());
			if (!refresh && cacheEntry != null
					&& cacheEntry.containsKey(commitId)) {
				// this may be null in fact
				return cacheEntry.get(commitId);
			}
			if (cacheEntry == null) {
				cacheEntry = new HashMap<String, String>();
				commitMappingCache.put(repository.getDirectory().getPath(),
						cacheEntry);
			} else {
				cacheEntry.clear();
			}

			Map<String, Date> tagMap = new HashMap<String, Date>();
			try (RevWalk rw = new RevWalk(repository)) {
				Map<String, Ref> tags = repository.getRefDatabase().getRefs(
						Constants.R_TAGS);
				for (Ref tagRef : tags.values()) {
					RevObject any = rw.parseAny(repository.resolve(tagRef.getName()));
					if (any instanceof RevTag) {
						RevTag tag = (RevTag) any;
						if (tag.getObject().name().equals(commitId)) {
							Date timestamp;
							if (tag.getTaggerIdent() != null) {
								timestamp = tag.getTaggerIdent().getWhen();
							} else {
								try {
									RevCommit commit = rw.parseCommit(tag.getObject());
									timestamp = commit.getCommitterIdent().getWhen();
								} catch (IncorrectObjectTypeException e) {
									// not referencing a comit.
									timestamp = null;
								}
							}
							tagMap.put(tagRef.getName(), timestamp);
						}
					} else if (any instanceof RevCommit) {
						RevCommit commit = ((RevCommit)any);
						if (commit.name().equals(commitId))
							tagMap.put(tagRef.getName(), commit.getCommitterIdent().getWhen());
					} // else ignore here
				}
			} catch (IOException e) {
				// ignore here
			}

			String cacheValue = null;

			if (!tagMap.isEmpty()) {
				// we try to obtain the "latest" tag
				Date compareDate = new Date(0);
				for (Map.Entry<String, Date> tagEntry : tagMap.entrySet()) {
					if (tagEntry.getValue() != null
							&& tagEntry.getValue().after(compareDate)) {
						compareDate = tagEntry.getValue();
						cacheValue = tagEntry.getKey();
					}
				}
				// if we don't have time stamps, we sort
				if (cacheValue == null) {
					String compareString = ""; //$NON-NLS-1$
					for (String tagName : tagMap.keySet()) {
						if (tagName.compareTo(compareString) >= 0) {
							cacheValue = tagName;
							compareString = tagName;
						}
					}
				}
			}

			if (cacheValue == null) {
				// we didnt't find a tag, so let's look for local branches
				Set<String> branchNames = new TreeSet<String>();
				// put this into a sorted set
				try {
					Map<String, Ref> remoteBranches = repository
							.getRefDatabase().getRefs(Constants.R_HEADS);
					for (Ref branch : remoteBranches.values()) {
						if (branch.getObjectId().name().equals(commitId)) {
							branchNames.add(branch.getName());
						}
					}
				} catch (IOException e) {
					// ignore here
				}
				if (!branchNames.isEmpty()) {
					// get the last (sorted) entry
					cacheValue = branchNames.toArray(new String[branchNames
							.size()])[branchNames.size() - 1];
				}
			}

			if (cacheValue == null) {
				// last try: remote branches
				Set<String> branchNames = new TreeSet<String>();
				// put this into a sorted set
				try {
					Map<String, Ref> remoteBranches = repository
							.getRefDatabase().getRefs(Constants.R_REMOTES);
					for (Ref branch : remoteBranches.values()) {
						if (branch.getObjectId().name().equals(commitId)) {
							branchNames.add(branch.getName());
						}
					}
					if (!branchNames.isEmpty()) {
						// get the last (sorted) entry
						cacheValue = branchNames.toArray(new String[branchNames
								.size()])[branchNames.size() - 1];
					}
				} catch (IOException e) {
					// ignore here
				}
			}
			cacheEntry.put(commitId, cacheValue);
			return cacheValue;
		}
	}
	
	public RevCommit mapCommitIdToRevCommit(String commitId){
		RevCommit commit = null;
		if (!ObjectId.isId(commitId)) {
			return null;
		}
		try(RevWalk revWalk=new RevWalk(repository)){
			commit=revWalk.parseCommit(ObjectId.fromString(commitId));
		} catch (MissingObjectException e) {
			e.printStackTrace();
		} catch (IncorrectObjectTypeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return commit;
	}
	
	
	/**
	 * Tries to split the conflict file
	 * @param file 
	 *          the absolute path of conflict file
	 * @return map
	 *         Map<String,String>
	 */
	public Map<String,String> getConflictFileContentWithSplit(File file){
		StringBuffer sb=new StringBuffer();
		Map<String,String> map=new HashMap<String, String>();
		BufferedReader br=null;
		String temp=null;
		try {
			br=new BufferedReader(new FileReader(file));
			
			while((temp=br.readLine())!=null){
				String str=new String(temp);
				if(str.startsWith("<<<<<<<")){
					sb.append(str.substring(7).trim());
				}else if(str.startsWith("[{") && str.endsWith("}]")){
					sb.append(str);
				}else if(str.startsWith("=======")){
					continue;
				}else if(str.startsWith(">>>>>>>")){
					sb.append(str.substring(7).trim());
					break;
				}
				sb.append("SPLIT");
			}
			
			
			String[] strs= sb.toString().split("SPLIT");
			if(strs.length==4){
				map.put(strs[0], strs[1]);
				map.put(strs[3], strs[2]);
			}
			
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}
	
	
	/**
	 * Tries to revert back conflict file
	 * @param file 
	 *          the absolute path of conflict file
	 * @param source
	 *          file contents 
	 * @throws IOException 
	 */
	public void revertMapContentToConflictFile(File file,Map<String,String> source) throws IOException{
		
		String[] strs=new String[4];
		for (Map.Entry<String, String> entry : source.entrySet()) {  
			if(entry.getKey().equals("HEAD")){
				strs[0]=entry.getKey();
				strs[1]=entry.getValue();
			}else if(entry.getKey().startsWith("branch")){
				strs[3]=entry.getKey();
				strs[2]=entry.getValue();
			}
		} 
		
		BufferedWriter bw=null;
		try {
			bw=new BufferedWriter(new FileWriter(file));
			bw.write("<<<<<<< "+strs[0]);
			bw.newLine();
			bw.write(strs[1]);
			bw.newLine();
			bw.write("=======");
			bw.newLine();
			bw.write(strs[2]);
			bw.newLine();
			bw.write(">>>>>>> "+strs[3]);
			
			bw.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(bw!=null){
				bw.close();
			}
		}
	}
	
	
	/**
	 * Appends content to end of given file.
	 *
	 * @param file
	 * @param content
	 * @throws IOException
	 */
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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


	@Deprecated
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
	@Deprecated
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
	@Deprecated
	public File createFile(File project, String name) throws IOException {
		String path = project.getAbsolutePath();
		int lastSeparator = path.lastIndexOf(File.separator);
		FileUtils.mkdirs(new File(path.substring(0, lastSeparator)), true);

		File file = new File(path,name);
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
	@Deprecated
	public RevCommit commit(String message) throws NoHeadException,
			NoMessageException, UnmergedPathException,
			ConcurrentRefUpdateException, JGitInternalException,
			WrongRepositoryStateException, GitAPIException {
		Git git = new Git(repository);
		CommitCommand commitCommand = git.commit();
		commitCommand.setAuthor("J. Git", "j.git@egit.org");
		commitCommand.setCommitter(commitCommand.getAuthor());
		commitCommand.setMessage(message);
//		commitCommand.setAll(true);
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
	@Deprecated
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
	@Deprecated
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
	@Deprecated
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
