package com.miracle.apps.git.core.op;

import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.URIish;

/**
 * Stores the result of a fetch operation
 */
public class FetchOperationResult {
	private final URIish uri;

	private final FetchResult fetchResult;

	private final String fetchErrorMessage;

	/**
	 * @param uri
	 * @param result
	 */
	public FetchOperationResult(URIish uri, FetchResult result) {
		this.uri = uri;
		this.fetchResult = result;
		this.fetchErrorMessage = null;
	}

	/**
	 * @param uri
	 * @param errorMessage
	 */
	public FetchOperationResult(URIish uri, String errorMessage) {
		this.uri = uri;
		this.fetchResult = null;
		this.fetchErrorMessage = errorMessage;
	}

	/**
	 * @return the URI
	 *
	 */
	public URIish getURI() {
		return uri;
	}

	/**
	 * @return the result
	 */
	public FetchResult getFetchResult() {
		return fetchResult;
	}

	/**
	 * @return the error message
	 */
	public String getErrorMessage() {
		return fetchErrorMessage;
	}
}
