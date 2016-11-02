/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.tests.builder;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaModelMarker;

public class Problem implements Comparable<Problem> {
	private String location;
	private String message;
	private IPath resourcePath;
	private int start = -1, end = -1, categoryId = -1;
	private String sourceId;
	private int severity = IMarker.SEVERITY_ERROR;

	public Problem(String location, String message, IPath resourcePath, int start, int end, int categoryId, int severity) {
		this.location = location;
		this.message = message;
		this.resourcePath = resourcePath;
		this.start = start;
		this.end = end;
		this.categoryId = categoryId;
		this.severity = severity;
	}

	public Problem(IMarker marker){
		this.location = marker.getAttribute(IMarker.LOCATION, "");
		this.message = marker.getAttribute(IMarker.MESSAGE, "");
		this.resourcePath = marker.getResource().getFullPath();
		this.start = marker.getAttribute(IMarker.CHAR_START, -1);
		this.end = marker.getAttribute(IMarker.CHAR_END, -1);
		this.categoryId = marker.getAttribute(IJavaModelMarker.CATEGORY_ID, -1);
		this.sourceId = marker.getAttribute(IMarker.SOURCE_ID, "missing");
		this.severity = marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
	}
	public int getCategoryId() {
		return categoryId;
	}

/**
 * Return the IMarker.SOURCE_ID attribute of the underlying marker if any.
 * Value null denotes a problem created from explicit structural attributes
 * (instead of using a source marker). Value "missing" denotes that the marker
 * used to initialize the problem had no IMarker.SOURCE_ID attribute.
 * @return the IMarker.SOURCE_ID attribute of the underlying marker if any
 */
public String getSourceId() {
	return this.sourceId;
}
	/**
	 * Gets the location.
	 * @return Returns a String
	 */
	public String getLocation() {
		return location;
	}
	/**
	 * Gets the message.
	 * @return Returns a String
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * Gets the resourcePath.
	 * @return Returns a IPath
	 */
	public IPath getResourcePath() {
		return resourcePath;
	}

public int getSeverity() {
	return this.severity;
}

	public int getStart() {
		return this.start;
	}

	public int getEnd() {
		return this.end;
	}

	public String toString(){
// ignore locations since the builder no longer finds exact Java elements
//		return "Problem : " + message + " [ resource : <" + resourcePath + "> location <"+ location + "> ]";
		return
			"Problem : "
			+ message
			+ " [ resource : <"
			+ resourcePath
			+ ">"
			+ (" range : <" + this.start + "," + this.end + ">")
			+ (" category : <" + this.categoryId + ">")
			+ (" severity : <" + this.severity + ">")
			+ "]";
	}

	public boolean equals(Object o){
		if (o instanceof Problem){
			return this.toString().equals(o.toString());
		}
		return false;
	}

	public int hashCode() {
		return this.toString().hashCode();
	}

	public int compareTo(Problem problem) {
		/* Replace initial implementation with toString() comparison otherwise the problems order may change
		 * when different VM are used (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=213570)...
		if (!(this.getLocation().equals(problem.getLocation()))) {
			return this.getLocation().compareTo(problem.getLocation());
		}
		if (this.getStart() < problem.getStart()) {
			return -1;
		}
		if (this.getEnd() < problem.getEnd()) {
			return -1;
		}
		return this.getMessage().compareTo(problem.getMessage());
		*/
		return this.toString().compareTo(problem.toString());
	}
}
