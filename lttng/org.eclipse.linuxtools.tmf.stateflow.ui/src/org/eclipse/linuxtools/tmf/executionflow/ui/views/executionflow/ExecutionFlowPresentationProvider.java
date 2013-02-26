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

package org.eclipse.linuxtools.tmf.executionflow.ui.views.executionflow;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.linuxtools.internal.tmf.executionflow.ui.Messages;
import org.eclipse.linuxtools.internal.tmf.executiontrace.core.Attributes;
import org.eclipse.linuxtools.internal.tmf.executiontrace.core.StateValues;
import org.eclipse.linuxtools.tmf.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.tmf.core.exceptions.StateSystemDisposedException;
import org.eclipse.linuxtools.tmf.core.exceptions.StateValueTypeException;
import org.eclipse.linuxtools.tmf.core.exceptions.TimeRangeException;
import org.eclipse.linuxtools.tmf.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystem;
import org.eclipse.linuxtools.tmf.core.statevalue.ITmfStateValue;
import org.eclipse.linuxtools.tmf.executiontrace.core.trace.CtfExecutionTrace;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.swt.graphics.RGB;

/**
 * Presentation provider for the control flow view
 */
public class ExecutionFlowPresentationProvider extends TimeGraphPresentationProvider {

    private enum State {
        UNKNOWN     (new RGB(255, 255, 255)),
        WAITING     (new RGB(200, 0, 0)),
        RUNNING     (new RGB(0, 200, 0));

        public final RGB rgb;

        private State (RGB rgb) {
            this.rgb = rgb;
        }
    }

    @Override
    public String getStateTypeName() {
        return Messages.ExecutionFlowView_stateTypeName;
    }

    @Override
    public StateItem[] getStateTable() {
        StateItem[] stateTable = new StateItem[State.values().length];
        for (int i = 0; i < stateTable.length; i++) {
            State state = State.values()[i];
            stateTable[i] = new StateItem(state.rgb, state.toString());
        }
        return stateTable;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        if (event instanceof ExecutionFlowEvent) {
            int status = ((ExecutionFlowEvent) event).getStatus();
            if (status == StateValues.RUNNING) {
                return State.RUNNING.ordinal();
            } else if (status == StateValues.WAITING) {
                return State.WAITING.ordinal();
            }
        }
        return State.UNKNOWN.ordinal();
    }

    @Override
    public String getEventName(ITimeEvent event) {
        if (event instanceof ExecutionFlowEvent) {
            int status = ((ExecutionFlowEvent) event).getStatus();
            if (status == StateValues.RUNNING) {
                return State.RUNNING.toString();
            } else if (status == StateValues.WAITING) {
                return State.WAITING.toString();
            }
        }
        return State.UNKNOWN.toString();
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event) {
    	//
        if (event instanceof ExecutionFlowEvent) {
        	ExecutionFlowEntry entry = (ExecutionFlowEntry) event.getEntry();
        	Map<String, String> retMap = new LinkedHashMap<String, String>();
        	retMap.put("Package",entry.getContextInfo());
        	retMap.put("Class.Method", entry.getName());
        	return retMap;
        }
        else {
        	return super.getEventHoverToolTipInfo(event);
        }
    }
     /*   	
            ExecutionFlowEntry entry = (ExecutionFlowEntry) event.getEntry();
            ITmfStateSystem ssq = entry.getTrace().getStateSystem(CtfExecutionTrace.STATE_ID);
            int tid = entry.getThreadId();          

            try {
                //Find every CPU first, then get the current thread
                int cpusQuark = ssq.getQuarkAbsolute(Attributes.CPUS);
                List<Integer> cpuQuarks = ssq.getSubAttributes(cpusQuark, false);
                for (Integer cpuQuark : cpuQuarks) {
                    int currentThreadQuark = ssq.getQuarkRelative(cpuQuark, Attributes.CURRENT_THREAD);
                    ITmfStateInterval interval = ssq.querySingleState(event.getTime(), currentThreadQuark);
                    if (!interval.getStateValue().isNull()) {
                        ITmfStateValue state = interval.getStateValue();
                        int currentThreadId = state.unboxInt();
                        if (tid == currentThreadId) {
                            retMap.put(Messages.ExecutionFlowView_attributeCpuName, ssq.getAttributeName(cpuQuark));
                            break;
                        }
                    }
                }

            } catch (AttributeNotFoundException e) {
                e.printStackTrace();
            } catch (TimeRangeException e) {
                e.printStackTrace();
            } catch (StateValueTypeException e) {
                e.printStackTrace();
            } catch (StateSystemDisposedException e) {
                //Ignored 
            }
        }
        return retMap;
        */
    

}
