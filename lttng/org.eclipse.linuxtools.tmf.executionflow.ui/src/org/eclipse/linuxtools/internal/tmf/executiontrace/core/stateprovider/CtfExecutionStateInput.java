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

import java.util.HashMap;

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

/**
 * This is the state change input plugin for TMF's state system which handles
 * the LTTng 2.0 kernel traces in CTF format.
 *
 * It uses the reference handler defined in CTFKernelHandler.java.
 *
 * @author alexmont
 *
 */
public class CtfExecutionStateInput extends AbstractStateChangeInput {
  /**
     * Instantiate a new state provider plugin.
     *
     * @param trace
     *            The LTTng 2.0 kernel trace directory
     */
    public CtfExecutionStateInput(CtfTmfTrace trace) {
        super(trace, CtfTmfEvent.class, "LTTng Execution"); //$NON-NLS-1$
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
        CtfTmfEvent event = (CtfTmfEvent) ev;
       
        final String eventName = event.getEventName();      

        try {
                      
            // we are looking for either lttng_ust_java:function_entry_event or 
            // lttng_ust_java:function_exit_event
            if (eventName.startsWith(ExecutionTraceStrings.FUNCTION_EVENT_PREFIX)) {
            	
            	// is it a function entry or exit event?
            	boolean isEntry = eventName.regionMatches(24, "entry", 0, 5);
            	
            	// these events have the same attributes:
            	// string_class  : the class name
            	// string_method : the method of the class being called
            	// int_lineno    : the line number of the call
            	            	
            	 ITmfStateValue value;

                 final ITmfEventField content = event.getContent();
                 final long ts = event.getTimestamp().getValue();
                 String threadId = Long.toHexString( (Long)(content.getField(ExecutionTraceStrings.TID_FIELD).getValue()));                
                 String className = (String)content.getField(ExecutionTraceStrings.CLASS_FIELD).getValue();
                 String methodName = (String)content.getField(ExecutionTraceStrings.METHOD_FIELD).getValue();
                 String lineno = content.getField(ExecutionTraceStrings.LINE_NUMBER_FIELD).getValue().toString();
                 
                 // validate that we have a quark for this class
                 int threadQuark=ss.getQuarkAbsoluteAndAdd(Attributes.THREADS, threadId);   	                            
                 
                 // in our model we are going to treat the line number as part of the method name as a way of handling
                 // overloading
                 methodName = className + "." + methodName;// + " [line " + lineno + "]";                 
                 
                 // get/create quark for this method call on this thread
                 Integer methodQuark = ss.getQuarkRelativeAndAdd(threadQuark, methodName);
                 
                 String fullName = ss.getFullAttributePath(methodQuark);
                 
                 //update the state for this method quark
                 int methodStatusQuark = ss.getQuarkRelativeAndAdd(methodQuark, Attributes.STATUS);
                 if (isEntry) {
                	 value = TmfStateValue.newValueInt(StateValues.RUNNING);               	 
                 } else {
                	 value = TmfStateValue.nullValue();
                 }
                 ss.modifyAttribute(ts, value, methodStatusQuark);       
                 // here we are also going to modify the status quark for the class itself so that it also appears to be running or not 
                 // in the given interval
                 int threadStatusQuark = ss.getQuarkRelativeAndAdd(threadQuark,Attributes.STATUS);
            	 ss.modifyAttribute(event.getTrace().getStartTime().getValue(),value, threadStatusQuark);  
            	
            } else {
            	// it is some other event we do not care about
            }
       } catch (AttributeNotFoundException ae) {
            /*
             * This would indicate a problem with the logic of the manager here,
             * so it shouldn't happen.
             */
            ae.printStackTrace();

        } catch (TimeRangeException tre) {
            /*
             * This would happen if the events in the trace aren't ordered
             * chronologically, which should never be the case ...
             */
            System.err.println("TimeRangeExcpetion caught in the state system's event manager."); //$NON-NLS-1$
            System.err.println("Are the events in the trace correctly ordered?"); //$NON-NLS-1$
            tre.printStackTrace();

        } catch (StateValueTypeException sve) {
            /*
             * This would happen if we were trying to push/pop attributes not of
             * type integer. Which, once again, should never happen.
             */
            sve.printStackTrace();
        }
    }   

}
