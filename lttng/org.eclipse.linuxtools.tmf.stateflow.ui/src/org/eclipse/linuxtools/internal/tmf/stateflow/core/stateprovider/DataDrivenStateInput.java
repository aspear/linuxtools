/*******************************************************************************
 * Copyright (c) 2012 Ericsson
 * Copyright (c) 2010, 2011 École Polytechnique de Montréal
 * Copyright (c) 2010, 2011 Alexandre Montplaisir <alexandre.montplaisir@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider;

import org.eclipse.linuxtools.internal.tmf.stateflow.core.Attributes;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventType;
import org.eclipse.linuxtools.tmf.core.event.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.statesystem.AbstractStateChangeInput;
import org.eclipse.linuxtools.tmf.core.statesystem.IStatePresentationInfo;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemContextHierarchyInfo;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystemBuilder;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

/**
 * This is the state change input plugin for TMF's state system which handles
 * the LTTng 2.0 kernel traces in CTF format.
 *
 * It uses the reference handler defined in CTFKernelHandler.java.
 *
 * @author alexmont
 *
 */
public class DataDrivenStateInput extends AbstractStateChangeInput {
			
	private int currentContextQuark = -1;
	private String[] lastContextIds;
	private int rootQuark = -1;
	//private StateItem[] possibleStates=null;
	

    public DataDrivenStateInput(StateSystemPresentationInfo statePresentationInfo, ITmfTrace trace,Class<? extends ITmfEvent> eventType, String id) {
        super(trace, eventType, id);
        
        this.statePresentationInfo = statePresentationInfo;
        
        // the last contextids are sized for the number of contexts we have in the hierarchy
    	lastContextIds = new String[statePresentationInfo.getContextHierarchy().length];       
    }

    @Override
    public void assignTargetStateSystem(ITmfStateSystemBuilder ssb) {
        /* We can only set up the locations once the state system is assigned */
        super.assignTargetStateSystem(ssb);
        
        initializeStateSystem(ssb);
    }
    
    private void initializeStateSystem(ITmfStateSystemBuilder ssb) {
    	// add the root level context nodes to the tree
    	IStateSystemContextHierarchyInfo[] hierarchy = statePresentationInfo.getContextHierarchy();    	
    	rootQuark = ssb.getQuarkAbsoluteAndAdd(hierarchy[0].getContextId());    	   	
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        /*
         * AbstractStateChangeInput should have already checked for the correct
         * class type
         */
    	if (statePresentationInfo == null) {
    		return;
    	}
    
    	// get the event 
    	ITmfEventType eventType = event.getType();
		if (eventType == null) {
			return;
		}		
		String eventTypeName = eventType.getName();	    	
        	    	
        try {          	       	
            	// check to see if there are context changes that can be extracted from this event
            	String[] contextIds = statePresentationInfo.getContext(eventTypeName,event);
            	if (contextIds != null) {
            		// we do have a context change with this event, update the member that tracks the current context
            		// accordingly
            		for (int c=0;c<lastContextIds.length;++c) {
            			if (contextIds[c] != null) {
            				lastContextIds[c] = contextIds[c];
            			}
            		}
            		currentContextQuark = ss.getQuarkRelativeAndAdd(rootQuark, lastContextIds);
            	}
            	
            	// TODO the state below could possibly be for multiple or a single level in the hierarchy
            	// we need to figure out an efficient way to determine what could possibly be multiple different
            	// states for different levels from the same event.  for now we assume it is always for the 
            	// last level
            	if (currentContextQuark != -1) {
	            	//check to see if there is new state that we can extract for this event
	            	IStatePresentationInfo newState = statePresentationInfo.getNewState(eventTypeName,event);
	            	if (newState != null) {
	            		           		          		
	            		// NOTE: the ITmfTimestamp values that come on the events could be in any scale.  We use nanosecond
	            		// scale in the state system, so normalize the timestamp from the event before addding the new state value
	            		final long ts = event.getTimestamp().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();	            		
	                    
	                    // get the status quark for this particular context
	                    int statusQuark = ss.getQuarkRelativeAndAdd(currentContextQuark,Attributes.STATE);
	                    	                    
	                    //FIXME
	                    //long traceStartTime = event.getTrace().getStartTime().getValue();
	            		//long traceEndTime = event.getTrace().getEndTime().getValue();
	                    //System.out.println("Add Quark=" + currentContextQuark + " statusQuark=" + statusQuark+ " state="+newState.getStateValue().toString() + " at ts='" + ts + "' traceStart='" + traceStartTime + "' traceEndTime='"+traceEndTime+"'");
	                    	                
	                    // set the state at this particular point in time to the new value
	                    ss.modifyAttribute(ts, newState.getStateValue(), statusQuark);                    
	            	}           
            	}
     
        } catch (Exception e) {
           
            e.printStackTrace();
        }
    }   
}
