/*******************************************************************************
 * Copyright (c) 2013 VMware Inc.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.statesystem;

import java.util.Map;

import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

/**
 * Factory interface for creation of IStateChangeInput instances for a given trace.
 * Extensions that implement this interface should return an instance of a IStateChangeInput
 * that is ready to go on the specified input trace
 * @author Aaron Spear aspear@vmware.com
 * @since 2.0
 */
public interface IStateSystemFactory {

    public final String EXTENSION_ID = "org.eclipse.linuxtools.tmf.core.statesystem.IStateChangeInputFactory";

    /**
     * @return a unique, fixed string id of the state system that is created from this factory
     */
    public String getStateSystemId();

    /**
     * Check to see if this factory is able to create a state system for the given trace
     * @param trace the trace to create a factory for
     * @return true if a state system can be created, false otherwise
     */
    public boolean canCreateStateSystem(ITmfTrace trace);

    /**
     * factory method to create a new state system for the given trace.  If the factory does not
     * support the given trace, null is returned. In the case of any other type of error an exception is thrown
     * that
     * @param trace the trace that this state change input is being created for
     * @param stateSystems the container that a created state system is added to.
     * @return null if the state system cannot be created by this factory for the
     * trace.  Otherwise, a valid state system is returned.
     */
    public ITmfStateSystem createStateSystem(ITmfTrace trace) throws TmfTraceException;
}
