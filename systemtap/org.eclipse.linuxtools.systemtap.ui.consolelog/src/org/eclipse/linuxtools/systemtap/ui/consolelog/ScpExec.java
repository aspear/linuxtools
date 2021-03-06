/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.systemtap.ui.consolelog;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.linuxtools.systemtap.graphingapi.ui.widgets.ExceptionErrorDialog;
import org.eclipse.linuxtools.systemtap.structures.process.SystemtapProcessFactory;
import org.eclipse.linuxtools.systemtap.structures.runnable.Command;
import org.eclipse.linuxtools.systemtap.structures.runnable.StreamGobbler;
import org.eclipse.linuxtools.systemtap.ui.consolelog.internal.ConsoleLogPlugin;
import org.eclipse.linuxtools.systemtap.ui.consolelog.preferences.ConsoleLogPreferenceConstants;
import org.eclipse.ui.PlatformUI;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;

public class ScpExec extends Command {

	private Channel channel;

	/**
	 * @since 2.0
	 */
	public ScpExec(String cmds[]) {
		super(cmds, null);
		this.command = ""; //$NON-NLS-1$
		for (String cmd:cmds) {
			this.command = this.command + " " + cmd; //$NON-NLS-1$
		}
	}

	@Override
	protected IStatus init() {
		String user = ConsoleLogPlugin.getDefault().getPreferenceStore()
				.getString(ConsoleLogPreferenceConstants.SCP_USER);
		String host = ConsoleLogPlugin.getDefault().getPreferenceStore()
				.getString(ConsoleLogPreferenceConstants.HOST_NAME);
		String password = ConsoleLogPlugin.getDefault()
				.getPreferenceStore()
				.getString(ConsoleLogPreferenceConstants.SCP_PASSWORD);

		try {
			channel = SystemtapProcessFactory.execRemote(
					new String[] { command }, System.out, System.err, user, host, password);

			errorGobbler = new StreamGobbler(channel.getExtInputStream());
			inputGobbler = new StreamGobbler(channel.getInputStream());

			this.transferListeners();
			return Status.OK_STATUS;
		} catch (JSchException e) {
			IStatus status = new Status(IStatus.ERROR, ConsoleLogPlugin.PLUGIN_ID, Messages.ScpExec_FileTransferFailed, e);
			ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.ScpExec_Error, e.getMessage(), status);
			return status;
		} catch (IOException e) {
			IStatus status = new Status(IStatus.ERROR, ConsoleLogPlugin.PLUGIN_ID, Messages.ScpExec_FileTransferFailed, e);
			ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.ScpExec_Error, e.getMessage(), status);
			return status;
		}
	}

	@Override
	public void run() {
		try {
			channel.connect();

			errorGobbler.start();
			inputGobbler.start();

			while (!stopped) {
				if (channel.isClosed() || (channel.getExitStatus() != -1)) {
					stop();
					break;
				}
			}

		} catch (JSchException e) {
			ExceptionErrorDialog.openError(Messages.ScpExec_errorConnectingToServer, e);
		}
	}

    /* Stops the process from running and stops the <code>StreamGobblers</code> from monitoring
	 * the dead process.
	 */
	@Override
	public synchronized void stop() {
		if(!stopped) {
            channel.disconnect();
		}
	}

	static int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0) {
			return b;
		}
		if (b == -1) {
			return b;
		}

		if (b == 1 || b == 2) {
			StringBuilder sb = new StringBuilder();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
		}
		return b;
	}

   private String command;

   public static final int INPUT_STREAM = 1;
}
