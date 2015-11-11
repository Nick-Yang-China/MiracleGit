package com.miracle.apps.git.core.op;

import com.miracle.apps.git.core.errors.CoreException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.PathFilter;

/**
 * Operation that gets the diff information
 */
public class DiffOperation implements GitControlOperation {

	private final Repository repository;
	
	private final String path;
	
	private final String oldTree;
	
	private final String newTree;

	private List<DiffEntry> diffs;

	private OutputStream out = new ByteArrayOutputStream();

	private Git git;
	
	@Deprecated
	public DiffOperation(final Repository repository){
		this.repository = repository;
		this.path=null;
		this.oldTree=null;
		this.newTree=null;
	}

	/**
	 * Create operation gets the diff information
	 *
	 * @param repository
	 * @param path
	 * 			Path strings use '/' to delimit directories on all platforms.
	 */
	public DiffOperation(final Repository repository, final String path) {
		this.repository = repository;
		this.path=path;
		this.oldTree=null;
		this.newTree=null;
	}
	
	/**
	 * Create operation gets the diff information
	 *
	 * @param repository
	 * @param path
	 * 			Path strings use '/' to delimit directories on all platforms.
	 */
	public DiffOperation(final Repository repository, final String path,final String newTree,final String oldTree) {
		this.repository = repository;
		this.path=path;
		this.newTree=newTree;
		this.oldTree=oldTree;
	}
	

	@Override
	public void execute() throws CoreException {
		try {
			git=Git.wrap(repository);
			DiffCommand dc=git.diff();
			if(path !=null){
				dc.setPathFilter(PathFilter.create(path));
				if(isAdded(path))
					dc.setCached(true);
				else if(isModified(path)){
					dc.setOldTree(getTreeIterator("HEAD"));
				}else if(oldTree!=null && newTree!=null){
					dc.setNewTree(getTreeIterator(newTree));
					dc.setOldTree(getTreeIterator(oldTree));
				}else{
					dc.setOldTree(getTreeIterator("HEAD^"));
				}
			}
			
			dc.setOutputStream(out);
			diffs=dc.call();
		} catch (GitAPIException | IOException e) {
			throw new CoreException(e.getMessage());
		}
	}
	
	public List<DiffEntry> getDiffEntrys(){
		return this.diffs;
	}
	
	@Override
	public String toString(){
		return out.toString();
	}
	
	private boolean isAdded(String path) throws NoWorkTreeException, GitAPIException{
		if(path!=null)
			if(git.status().addPath(path).call().getAdded().size()!=0){
				return true;
			}
		return false;
	}
	
	private boolean isModified(String path) throws NoWorkTreeException, GitAPIException{
		if(path!=null)
			if(git.status().addPath(path).call().getModified().size()!=0){
				return true;
			}
		return false;
	}
	
	private AbstractTreeIterator getTreeIterator(String name)
			throws IOException {
		final ObjectId id = repository.resolve(name);
		if (id == null)
			throw new IllegalArgumentException(name);
		final CanonicalTreeParser p = new CanonicalTreeParser();
		try (ObjectReader or = repository.newObjectReader()) {
			p.reset(or, new RevWalk(repository).parseTree(id));
			return p;
		}
	}
}
