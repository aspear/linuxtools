/*******************************************************************************
 * Copyright (c) 2009 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Red Hat - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.internal.rpm.rpmlint.resolutions;

import org.eclipse.osgi.util.NLS;

@SuppressWarnings("javadoc")
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.linuxtools.rpm.rpmlint.resolutions.messages"; //$NON-NLS-1$
	public static String HardcodedPackagerTag_0;
	public static String HardcodedPrefixTag_0;
	public static String MacroInChangelog_0;
	public static String MacroInChangelog_1;
	public static String NoBuildrootTag_0;
	public static String NoBuildSection_0;
	public static String NoCleaningOfBuildroot_0;
	public static String NoCleanSection_0;
	public static String NoInstallSection_0;
	public static String NoPrepSection_0;
	public static String PatchNotApplied_0;
	public static String RpmBuildrootUsage_0;
	public static String SetupNotQuiet_0;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		//should not be instantiated
	}
}