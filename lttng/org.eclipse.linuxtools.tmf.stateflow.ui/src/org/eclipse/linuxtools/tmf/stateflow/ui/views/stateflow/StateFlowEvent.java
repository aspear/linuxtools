/*******************************************************************************
 * Copyright (c) 2012 Ericsson
 * Copyright (c) 2013 VMware Inc.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *   Aaron Spear - Cloned and refactored for data driven state flow
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.stateflow.ui.views.stateflow;

import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeEvent;

/**
 * Time Event specific to the control flow view
 */
public class StateFlowEvent extends TimeEvent {

    private final int stateIndex;

    /**
     * Constructor
     *
     * @param entry
     *            The entry to which this time event is assigned
     * @param time
     *            The timestamp of this event
     * @param duration
     *            The duration of this event
     * @param status
     *            The status assigned to the event
     */
    public StateFlowEvent(ITimeGraphEntry entry, long time, long duration, int stateIndex) {
        super(entry, time, duration);
        this.stateIndex = stateIndex;
    }

    /**
     * Get this event's state value
     */
    public int getStateIndex() {
        return stateIndex;
    }
}