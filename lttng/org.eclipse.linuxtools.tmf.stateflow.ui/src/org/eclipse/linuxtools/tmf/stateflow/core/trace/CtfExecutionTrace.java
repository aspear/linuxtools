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

package org.eclipse.linuxtools.tmf.stateflow.core.trace;

import java.io.InputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.linuxtools.ctf.core.trace.CTFReaderException;
import org.eclipse.linuxtools.ctf.core.trace.CTFTrace;
import org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider.XmlDataDrivenStateSystemFactory;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystem;
import org.eclipse.linuxtools.tmf.stateflow.ui.Activator;

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
        CTFTrace temp;

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
    }

    @Override
    protected void buildStateSystem() throws TmfTraceException {

    	// the super class implementation of buildStateSystem uses a factory method which purely analyzes the
    	// file system to figure out which state system factory to use.  We already know which one to use here,
    	// and so there is no need to use this.  Additionally we want to use the schema file included in the
    	// plugin so that the user doesn't have to provide one with every trace which is annoying.
    	InputStream schemaXmlInputStream = Activator.getResource("resources/execution-trace.state-schema.xml");
    	if (schemaXmlInputStream != null) {
    		 XmlDataDrivenStateSystemFactory factory = new XmlDataDrivenStateSystemFactory();
    		 ITmfStateSystem ss = factory.createStateSystem(this,schemaXmlInputStream);
             if (ss != null) {
                 fStateSystems.put(factory.getStateSystemId(), ss);
                 return;
             }
    	}

    	// default behavior, call the super class
    	super.buildStateSystem();
    }

}
