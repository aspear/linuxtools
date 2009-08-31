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

package org.eclipse.linuxtools.systemtap.localgui.core;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.progress.UIJob;

public class ShellOpener extends UIJob{
	private Shell shell;

	public ShellOpener(String name, Shell sh) {
		super(name);
		shell = sh;
	}

	@Override
	public IStatus runInUIThread(IProgressMonitor monitor) {
		shell.open();
		return Status.OK_STATUS;
	}
	
	public boolean isDisposed() {
		if (shell.isDisposed())
			return true;
		return false;
	}
	
}
