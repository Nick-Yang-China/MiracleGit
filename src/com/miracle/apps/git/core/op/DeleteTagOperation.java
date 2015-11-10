package com.miracle.apps.git.core.op;

import com.miracle.apps.git.core.errors.CoreException;

import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * Operation that deletes a tag
 */
public class DeleteTagOperation implements GitControlOperation {

	private final Repository repository;

	private String[] tags;
	
	private List<String> deleteTagLists;

	/**
	 * Create operation that deletes a single tag
	 *
	 * @param repository
	 * @param tag
	 * 			tagshortname or refs/heads/tagname
	 */
	public DeleteTagOperation(final Repository repository, final String tag) {
		this.repository = repository;
		tags=new String[1];
		this.tags[0] = tag;
	}
	
	/**
	 * Create operation that deletes multi tags
	 *
	 * @param repository
	 * @param tags
	 */
	public DeleteTagOperation(final Repository repository, final String... tags) {
		this.repository = repository;
		this.tags = tags;
	}

	@Override
	public void execute() throws CoreException {
		try {
			deleteTagLists=Git.wrap(repository).tagDelete().setTags(tags).call();
		} catch (GitAPIException e) {
			throw new CoreException("Exception deleting tag");
		}
	}

	@Override
	public String toString() {
		StringBuffer sb=new StringBuffer();
		if(deleteTagLists!=null && !deleteTagLists.isEmpty()){
			sb.append("TagDelete completed normally, below are deletetaglist:");
			for(String list:deleteTagLists){
				sb.append("\n"+list);
			}
			return sb.toString();
		}
		return super.toString();
	}
	
	
}
