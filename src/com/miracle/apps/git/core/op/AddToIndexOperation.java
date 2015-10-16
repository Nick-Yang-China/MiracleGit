package com.miracle.apps.git.core.op;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.FileTreeIterator;

/**
 */
public class AddToIndexOperation implements GitControlOperation{
	private final Collection<?> rsrcList;
	
	private Repository repo;

	/**
	 * Create a new operation to add files to the Git index
	 *
	 * @param rsrcs
	 *            collection which should be added to the
	 *            relevant Git repositories.
	 * @param repository
	 * 			  a git repository
	 */
	
	public AddToIndexOperation(final Collection<?> rsrcs,Repository repository) {
		rsrcList = rsrcs;
		repo=repository;
	}

	
	public AddToIndexOperation(ArrayList<String> rsrcs,Repository repository) {
		rsrcList = rsrcs;
		repo=repository;
	}
	
	@Override
	public void execute() throws GitAPIException {
			for(Object filepath : rsrcList){
				String file=(String)filepath;
				AddCommand command=addToCommand(repo, file);
				command.call();
			}
	}


	private AddCommand addToCommand(Repository repo,String filepattern) {
		AddCommand command = null;
		if (command == null) {
			Git git = new Git(repo);
			FileTreeIterator it = new FileTreeIterator(repo);
			command = git.add().setWorkingTreeIterator(it);
		}
		if ("".equals(filepattern)) //$NON-NLS-1$
			filepattern = "."; //$NON-NLS-1$
		
		return command;
	}
}
