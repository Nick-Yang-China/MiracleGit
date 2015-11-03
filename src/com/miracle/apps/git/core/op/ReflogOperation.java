package com.miracle.apps.git.core.op;

import com.miracle.apps.git.core.errors.CoreException;
import java.util.Collection;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ReflogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.Repository;

/**
 * Operation that gets Ref Log
 */
public class ReflogOperation implements GitControlOperation {

	private final Repository repository;

	private final String ref;
	
	private Collection<ReflogEntry> reflog;
	
	public ReflogOperation(final Repository repository){
		this.repository = repository;
		this.ref=null;
	}

	/**
	 * Create operation gets Ref Log
	 *
	 * @param repository
	 * @param ref
	 */
	public ReflogOperation(final Repository repository, final String ref) {
		this.repository = repository;
		this.ref=ref;
	}
	

	@Override
	public void execute() throws CoreException {
		try {
			ReflogCommand rc=Git.wrap(repository).reflog();
			if(ref!=null){
				rc.setRef(ref);
			}
			reflog=rc.call();
		} catch (GitAPIException e) {
			throw new CoreException(e.getMessage());
		}
	}
	
	public Collection<ReflogEntry> getReflogResults(){
		return this.reflog;
	}
	
}
