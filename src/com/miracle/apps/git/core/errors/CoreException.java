package com.miracle.apps.git.core.errors;

import org.eclipse.jgit.api.errors.GitAPIException;

public class CoreException extends GitAPIException {
	private static final long serialVersionUID = 1L;

	/**
	 * @param message
	 * @param cause
	 */
	public CoreException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public CoreException(String message) {
		super(message);
	}

}
