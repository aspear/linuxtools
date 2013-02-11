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
 ******************************************************************************/

package org.eclipse.linuxtools.tmf.executiontrace.core.trace;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.linuxtools.ctf.core.trace.CTFReaderException;
import org.eclipse.linuxtools.ctf.core.trace.CTFTrace;
import org.eclipse.linuxtools.internal.tmf.executiontrace.core.stateprovider.CtfExecutionStateInput;
import org.eclipse.linuxtools.tmf.core.TmfCommonConstants;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateChangeInput;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystem;
import org.eclipse.linuxtools.tmf.core.statesystem.StateSystemManager;

/**
 * This is the specification of CtfTmfTrace for use with LTTng 2.x kernel
 * traces. It uses the CtfKernelStateInput to generate the state history.
 *
 * @version 1.0
 * @author Alexandre Montplaisir
 */
public class CtfExecutionTrace extends CtfTmfTrace {

    /**
     * The file name of the History Tree
     */
    public final static String HISTORY_TREE_FILE_NAME = "stateHistory.ht"; //$NON-NLS-1$

    /**
     * ID of the state system we will build
     * @since 2.0
     * */
    public static final String STATE_ID = "org.eclipse.linuxtools.tmf.executiontrace"; //$NON-NLS-1$

    /**
     * Default constructor
     */
    public CtfExecutionTrace() {
        super();
    }

    @Override
    public boolean validate(final IProject project, final String path) {
        /*CTFTrace temp;
        
        // Make sure the trace is openable as a CTF trace. We do this here
        // instead of calling super.validate() to keep the reference to "temp".
        try {
            temp = new CTFTrace(path);
        } catch (CTFReaderException e) {
            return false;
        }

        //Make sure the domain is "ust" in the trace's env vars as it is what is 
        // used for the execution tracing.  TODO figure out some other annotation in the metadata
        // that uniquely identifies the execution tracing
        String dom = temp.getEnvironment().get("domain"); //$NON-NLS-1$
        if (dom != null && dom.equals("\"ust\"")) { //$NON-NLS-1$
            return true;
        }
        return false;
        */
    	return true;
    }

    @Override
    protected void buildStateSystem() throws TmfTraceException {
        super.buildStateSystem();

        /* Set up the path to the history tree file we'll use */
        IResource resource = this.getResource();
        String supplDirectory = null;

        try {
            // get the directory where the history file will be stored.
            supplDirectory = resource.getPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER);
        } catch (CoreException e) {
            throw new TmfTraceException(e.toString(), e);
        }

        final File htFile = new File(supplDirectory + File.separator + HISTORY_TREE_FILE_NAME);
        final IStateChangeInput htInput = new CtfExecutionStateInput(this);

        ITmfStateSystem ss = StateSystemManager.loadStateHistory(htFile, htInput, STATE_ID, false);
        fStateSystems.put(STATE_ID, ss);
    }

}
