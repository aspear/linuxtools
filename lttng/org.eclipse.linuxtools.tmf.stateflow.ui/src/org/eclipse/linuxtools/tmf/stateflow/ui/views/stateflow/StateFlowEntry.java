/*******************************************************************************
 * Copyright (c) 2012 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.stateflow.ui.views.stateflow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.linuxtools.internal.tmf.stateflow.views.common.EventIterator;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.stateflow.core.trace.CtfExecutionTrace;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;

/**
 * An entry in the Control Flow view
 */
public class StateFlowEntry implements ITimeGraphEntry {
    private final int fContextQuark;
    private final ITmfTrace fTrace;
    private StateFlowEntry fParent = null;
    private final ArrayList<StateFlowEntry> fChildren = new ArrayList<StateFlowEntry>();
    private final String fName;
    private final String fContextInfo;
    //private final int fThreadId;
    private final int fParentQuark;
    private long fBirthTime = -1;
    private long fStartTime = -1;
    private long fEndTime = -1;
    private List<ITimeEvent> fEventList = new ArrayList<ITimeEvent>();
    private List<ITimeEvent> fZoomedEventList = null;

    /**
     * Constructor
     *
     * @param methodQuark
     *            The attribute quark matching the state entry
     * @param trace
     *            The trace on which we are working
     * @param name
     *            The exec_name of this entry
     * @param threadId
     *            The TID of the thread
     * @param parentThreadId
     *            the Parent_TID of this thread
     * @param birthTime
     *            The birth time of this entry (this allows separating different
     *            process that could have the same TID)
     * @param startTime
     *            The start time of this process's lifetime
     * @param endTime
     *            The end time of this process
     */
    public StateFlowEntry(int contextQuark, ITmfTrace trace, String name, String contextInfo, int parentQuark, long birthTime, long startTime, long endTime) {
        fContextQuark = contextQuark;
        fTrace = trace;
        fName = name;
        fContextInfo = contextInfo;
        //fThreadId = threadId;
        fParentQuark = parentQuark;
        fBirthTime = birthTime;
        fStartTime = startTime;
        fEndTime = endTime;
    }

    @Override
    public ITimeGraphEntry getParent() {
        return fParent;
    }

    @Override
    public boolean hasChildren() {
        return fChildren != null && fChildren.size() > 0;
    }

    @Override
    public List<StateFlowEntry> getChildren() {
        return fChildren;
    }

    @Override
    public String getName() {
        return fName;
    }
    
    public String getContextInfo() {
        return fContextInfo;
    }    

    @Override
    public long getStartTime() {
        return fStartTime;
    }

    @Override
    public long getEndTime() {
        return fEndTime;
    }

    @Override
    public boolean hasTimeEvents() {
        return true;
    }

    @Override
    public Iterator<ITimeEvent> getTimeEventsIterator() {
        return new EventIterator(fEventList, fZoomedEventList);
    }

    @Override
    public Iterator<ITimeEvent> getTimeEventsIterator(long startTime, long stopTime, long visibleDuration) {
        return new EventIterator(fEventList, fZoomedEventList, startTime, stopTime);
    }

    /**
     * Get the quark of the attribute matching this thread's TID
     *
     * @return The quark
     */
    public int getNodeQuark() {
        return fContextQuark;
    }

    /**
     * Get the CTF trace object
     *
     * @return The trace
     */
    public ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Get this entry's thread ID
     *
     * @return The TID
     */
   // public int getThreadId() {
   //     return fThreadId;
   // }
    
    public int getParentQuark() {
        return fParentQuark;
    }

    /**
     * Get the birth time of this entry/process
     *
     * @return The birth time
     */
    public long getBirthTime() {
        return fBirthTime;
    }

    /**
     * Add an event to this process's timeline
     *
     * @param event
     *            The time event
     */
    public void addEvent(ITimeEvent event) {
        long start = event.getTime();
        long end = start + event.getDuration();
        synchronized (fEventList) {
            fEventList.add(event);
            if (fStartTime == -1 || start < fStartTime) {
                fStartTime = start;
            }
            if (fEndTime == -1 || end > fEndTime) {
                fEndTime = end;
            }
        }
    }

    /**
     * Set the general event list of this entry
     *
     * @param eventList
     *            The list of time events
     */
    public void setEventList(List<ITimeEvent> eventList) {
        fEventList = eventList;
    }

    /**
     * Set the zoomed event list of this entry
     *
     * @param eventList
     *            The list of time events
     */
    public void setZoomedEventList(List<ITimeEvent> eventList) {
        fZoomedEventList = eventList;
    }

    /**
     * Add a child entry to this one (to show relationships between processes as
     * a tree)
     *
     * @param child
     *            The child entry
     */
    public void addChild(StateFlowEntry child) {
        child.fParent = this;
        fChildren.add(child);
    }
    
    @Override
    public String toString() {
    	StringBuilder result = new StringBuilder(100);
    	result.append("[name='").append(fName).append("' ")
    	      .append(" quark=").append(fContextQuark)
    	      .append(" parentQuark=").append(fParentQuark)
    		  .append("]");
    	return result.toString();
    }
}
