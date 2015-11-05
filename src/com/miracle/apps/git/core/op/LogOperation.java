package com.miracle.apps.git.core.op;

import com.miracle.apps.git.core.errors.CoreException;

import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Operation that gets the commit history
 */
public class LogOperation implements GitControlOperation {

	private final Repository repository;

	private final String path;
	
	private final AnyObjectId start;
	
	private  Iterable<RevCommit> commits;
	
	private int maxCount;
	
	
	public LogOperation(final Repository repository){
		this.repository = repository;
		this.path = null;
		this.start=null;
	}

	/**
	 * Create operation gets the commit history
	 *
	 * @param repository
	 * @param path
	 */
	public LogOperation(final Repository repository, final String path) {
		this.repository = repository;
		this.path = path;
		this.start=null;
	}
	
	/**
	 * Create operation gets the commit history
	 *
	 * @param repository
	 * @param start
	 */
	public LogOperation(final Repository repository, final AnyObjectId start) {
		this.repository = repository;
		this.start = start;
		this.path=null;
	}

	@Override
	public void execute() throws CoreException {
		try {
			LogCommand lc=Git.wrap(repository).log();
			if(path ==null && start==null){
				lc.all();
			}else{
				if(start!=null)
					lc.add(start);
				if(path!=null)
					lc.addPath(path);
			}
			if(maxCount!=0)
				lc.setMaxCount(maxCount);
			commits=lc.call();
		} catch (GitAPIException | IOException e) {
			throw new CoreException(e.getMessage());
		}
	}
	
	public Iterable<RevCommit> getCommitResults(){
		return this.commits;
	}
	
	public void setMaxCount(int maxCount) {
		this.maxCount = maxCount;
	}
}
