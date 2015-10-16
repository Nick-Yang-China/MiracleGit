package com.miracle.apps.git.core.op;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * interface for JGit operations
 *
 */
public interface GitControlService {
	

		/**
		 * Executes the operation
		 *
		 * @throws GitAPIException
		 */
		void execute() throws GitAPIException ;


		/**
		 * A task to be performed before execution begins
		 */
		interface PreExecuteTask {

			/**
			 * Executes the task
			 *
			 * @param repository
			 *            the git repository
			 *
			 * @throws GitAPIException
			 */
			void preExecute(Repository repository)
					throws GitAPIException;
		}

		/**
		 * A task to be performed after execution completes
		 */
		interface PostExecuteTask {

			/**
			 * Executes the task
			 *
			 * @param repository
			 *            the git repository
			 *
			 * @throws GitAPIException
			 */
			void postExecute(Repository repository)
					throws GitAPIException;
		}


}
