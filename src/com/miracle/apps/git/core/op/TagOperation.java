package com.miracle.apps.git.core.op;

import java.io.IOException;

import com.miracle.apps.git.core.errors.CoreException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TagBuilder;
import org.eclipse.jgit.lib.RefUpdate.Result;

/**
 * Tags repository with given {@link TagBuilder} object.
 */
public class TagOperation implements GitControlOperation {

	private final TagBuilder tag;
	private final Repository repo;
	private final boolean shouldMoveTag;
	private Result updateResult;

	/**
	 * Construct TagOperation
	 *
	 * @param repo
	 * @param tag
	 * @param shouldMoveTag if <code>true</code> it will replace existing tag with same name
	 */
	public TagOperation(Repository repo, TagBuilder tag, boolean shouldMoveTag) {
		this.tag = tag;
		this.repo = repo;
		this.shouldMoveTag = shouldMoveTag;
	}


	@Override
	public void execute() throws GitAPIException {

			ObjectId tagId = updateTagObject();

			updateRepo(tagId);
	}

	private void updateRepo(ObjectId tagId) throws GitAPIException {
		String refName = Constants.R_TAGS + tag.getTag();

		try {
			RefUpdate tagRef = repo.updateRef(refName);
			tagRef.setNewObjectId(tagId);

			tagRef.setForceUpdate(shouldMoveTag);
			updateResult = tagRef.update();

//			if (updateResult != Result.NEW && updateResult != Result.FORCED)
//				throw new CoreException("Tag "+tag.getTag()+"creation failed (cause: +"+updateResult.toString()+")");
		} catch (IOException e) {
			throw new CoreException("Tag "+tag.getTag()+" creation failed",e);
		}
	}
	
	public Result getCreateTagResult(){
		return this.updateResult;
	}

	private ObjectId updateTagObject() throws GitAPIException {
		ObjectId startPointRef = tag.getObjectId();

		try {
			ObjectId tagId;
			repo.open(startPointRef);
			try (ObjectInserter inserter = repo.newObjectInserter()) {
				tagId = inserter.insert(tag);
				inserter.flush();
			}
			return tagId;
		} catch (IOException e) {
			throw new CoreException("Could not find object Id associated with tag "+tag.getTag());
		}
	}


	@Override
	public String toString() {
		if(updateResult!=null){
			return "Tag Result: "+updateResult.toString();
		}
		return super.toString();
	}
	
	
}
