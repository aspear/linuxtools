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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.linuxtools.internal.tmf.stateflow.core.Attributes;
import org.eclipse.linuxtools.internal.tmf.stateflow.core.StateValues;
import org.eclipse.linuxtools.internal.tmf.stateflow.ui.Messages;
import org.eclipse.linuxtools.tmf.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.tmf.core.exceptions.StateSystemDisposedException;
import org.eclipse.linuxtools.tmf.core.exceptions.StateValueTypeException;
import org.eclipse.linuxtools.tmf.core.exceptions.TimeRangeException;
import org.eclipse.linuxtools.tmf.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.tmf.core.statesystem.IStatePresentationInfo;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemPresentationInfo;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystem;
import org.eclipse.linuxtools.tmf.core.statevalue.ITmfStateValue;
import org.eclipse.linuxtools.tmf.stateflow.core.trace.CtfExecutionTrace;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.swt.graphics.RGB;

/**
 * Presentation provider for the control flow view
 */
public class StateFlowPresentationProvider extends TimeGraphPresentationProvider {

	private IStateSystemPresentationInfo presentationInfo=null;
	private StateItem[]  stateItemTable = new StateItem[0];
	private Map<ITmfStateValue,Integer>  stateValueToIndexMap = new HashMap<ITmfStateValue,Integer>();
    
    synchronized void setPresentationData( IStateSystemPresentationInfo presentationInfo ) {
    	this.presentationInfo = presentationInfo;
    	
    	//recreate all data structures here
    	IStatePresentationInfo[] allStates = presentationInfo.getAllStates();
    	stateItemTable = new StateItem[allStates.length];
    	stateValueToIndexMap.clear();
    	
    	for (int i=0;i<allStates.length;++i) {
    		IStatePresentationInfo info = allStates[i];
    		stateItemTable[i] = new StateItem(info.getStateColor(),info.getStateString());
    		stateValueToIndexMap.put(info.getStateValue(),new Integer(i));
    	}
    }
    

    @Override
    public String getStateTypeName() {
        return Messages.StateFlowView_stateTypeName;
    }

    @Override
    synchronized public StateItem[] getStateTable() {
    	return stateItemTable;
    }

    @Override
    synchronized public int getStateTableIndex(ITimeEvent event) {
        if (event instanceof StateFlowEvent) {
        	return ((StateFlowEvent)event).getStateIndex();           	
        }
        return 0; //unknown is always 0
    }

    @Override
    synchronized public String getEventName(ITimeEvent event) {
        if (event instanceof StateFlowEvent) {
        	int index = ((StateFlowEvent)event).getStateIndex();        	
        	return stateItemTable[index].getStateString();        	
//        	ITmfStateValue stateValue = ((StateFlowEvent)event).getStateValue();
//        	if (presentationInfo != null) {
//        		IStatePresentationInfo pi = presentationInfo.getPresentationForStateValue(stateValue);
//        		if (pi != null) {
//        			return pi.getStateString();
//        		}
//        	}        	
        }
        return "Unknown";
    }

    @Override
    synchronized public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event) {
    	//
        if (event instanceof StateFlowEvent) {
            //StateFlowEvent stateFlowEvent = (StateFlowEvent)event;
            
            //stateFlowEvent.get
        	StateFlowEntry entry = (StateFlowEntry) event.getEntry();
        	Map<String, String> retMap = new LinkedHashMap<String, String>();
        	//FIXME these are old hard coded values   	
        	
        	if (entry.getContextInfo() != null ) {
        	    retMap.put("Context",entry.getContextInfo());
        	}
        	if (entry.getName() != null) {
        	    retMap.put("Name", entry.getName());
        	}
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
