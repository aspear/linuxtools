/*******************************************************************************
 * Copyright (c) 2012 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 *
 *   Aaron Spear - Cloned and changed all values for data driven state flow
 ******************************************************************************/

package org.eclipse.linuxtools.internal.tmf.stateflow.core;

/**
 * This file defines all the attribute names used in the handler. Both the
 * construction and query steps should use them.
 *
 * These should not be externalized! The values here are used as-is in the
 * history file on disk, so they should be kept the same to keep the file format
 * compatible. If a view shows attribute names directly, the localization should
 * be done on the viewer side.
 *
 * @author alexmont
 *
 */
@SuppressWarnings({"nls", "javadoc"})
public abstract class Attributes {

	public static final String STATE = "DDState";

	/**
	 * @deprecated
	 */
	@Deprecated
	public static final String THREADS = "Threads";

	/**
	 * @deprecated
	 */
	@Deprecated
	public static final String METHODS = "Methods";

}
