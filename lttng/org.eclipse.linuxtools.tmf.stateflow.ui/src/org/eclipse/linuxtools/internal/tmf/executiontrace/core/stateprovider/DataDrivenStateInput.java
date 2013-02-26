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

package org.eclipse.linuxtools.internal.tmf.executiontrace.core.stateprovider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.eclipse.linuxtools.internal.tmf.executiontrace.core.Attributes;
import org.eclipse.linuxtools.internal.tmf.executiontrace.core.ExecutionTraceStrings;
import org.eclipse.linuxtools.internal.tmf.executiontrace.core.StateValues;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.tmf.core.exceptions.StateValueTypeException;
import org.eclipse.linuxtools.tmf.core.exceptions.TimeRangeException;
import org.eclipse.linuxtools.tmf.core.statesystem.AbstractStateChangeInput;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystemBuilder;
import org.eclipse.linuxtools.tmf.core.statevalue.ITmfStateValue;
import org.eclipse.linuxtools.tmf.core.statevalue.TmfStateValue;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfTrace;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.StateItem;

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
	
	private static final ITmfStateValue  RUNNING_VALUE = TmfStateValue.newValueInt(StateValues.RUNNING);
	private static final ITmfStateValue  NULL_VALUE = TmfStateValue.nullValue();
	
	private Map<Integer,Stack<Integer> > threadQuarkToMethodQuarkStackMap = new HashMap<Integer,Stack<Integer>>();
	
	private Map<StateItem,ITmfStateValue> stateItemToStateValueMap = new HashMap<StateItem,ITmfStateValue>();
	
	private StateInfo stateInfo = null;
	//private int currentContextQuark = -1;
	//private StateItem[] possibleStates=null;
	

    public DataDrivenStateInput(ITmfTrace trace,Class<? extends ITmfEvent> eventType, String id) {
        super(trace, eventType, id);
        
        // FIXME as an interim step, we check to see if the given trace has the special xml file
        // in the trace directory we are using to describe the state schema
        File traceDirectory;
        File traceFile = new File(trace.getPath());
        if (traceFile.isDirectory()) {
        	traceDirectory = traceFile;
        } else {
        	traceDirectory = traceFile.getParentFile();
        }
        
        File stateSchemaXml = new File(traceDirectory,trace.getName() + ".state-schema.xml");
        if (stateSchemaXml.exists() && stateSchemaXml.isFile()) {
        	stateInfo = new StateInfo();       	
        	
        	try {
        		stateInfo.parseInfoText(new BufferedInputStream(new FileInputStream(stateSchemaXml)));
        		
        		//get all known/possible states and create a map to the state values for efficiency.
        		final StateItem[] allStates = stateInfo.getAllStates();
        		for (StateItem s : allStates) {
        			stateItemToStateValueMap.put(s, TmfStateValue.newValueString(s.getStateString()));
        		}        		
        	}catch(Exception e) {
        		System.err.println("Error parsing state schema file " + e.getMessage() + " " + stateSchemaXml.getAbsolutePath());
        		//FIXME BETTER LOGGING HERE
        		e.printStackTrace();
        	}
        } else {
        	System.out.println("Trace " + trace.getName() + " does NOT have a state schema file " + stateSchemaXml.getAbsolutePath());
        }
        
    }

    @Override
    public void assignTargetStateSystem(ITmfStateSystemBuilder ssb) {
        /* We can only set up the locations once the state system is assigned */
        super.assignTargetStateSystem(ssb);
    }

    @Override
    protected void eventHandle(ITmfEvent ev) {
        /*
         * AbstractStateChangeInput should have already checked for the correct
         * class type
         */
    	if (stateInfo == null) {
    		return;
    	}
    	      
        //final String eventName = event.getEventName(); 
    	    	
        try {                       	
            	int contextQuark = -1;            	
            	
            	// check to see if there are context changes that can be extracted from this event
            	String[] parentAndThisContext = stateInfo.getContext(ev);
            	if (parentAndThisContext != null) {  
            		String parent = parentAndThisContext[0];
            		String currentContext = parentAndThisContext[1];
            		contextQuark=ss.getQuarkAbsoluteAndAdd(parent,currentContext);            		
            	}
            	//check to see if there is new state that we can extract for this event
            	StateItem newState = stateInfo.getNewState(ev);
            	if (newState != null) {
            		final ITmfEventField content = ev.getContent();
                    final long ts = ev.getTimestamp().getValue();
                    ITmfStateValue stateValue = stateItemToStateValueMap.get(newState);
                    if (stateValue == null) {
                    	System.err.println("TODO error reporting, this should not be possible unless there is a bug");
                    } else {
                    	// get the status quark for this particular context
                    	int statusQuark = ss.getQuarkRelativeAndAdd(contextQuark, Attributes.STATUS);
                    	// set the state at this particular point in time to the new value
                        ss.modifyAttribute(ts, stateValue, statusQuark);
                    }
            	}           
     
        } catch (Exception e) {
           
            e.printStackTrace();
        }
    }   
}
