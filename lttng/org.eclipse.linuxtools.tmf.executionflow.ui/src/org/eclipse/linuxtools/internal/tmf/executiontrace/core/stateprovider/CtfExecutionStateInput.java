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
	
	private static final ITmfStateValue  RUNNING_VALUE = TmfStateValue.newValueInt(StateValues.RUNNING);
	private static final ITmfStateValue  NULL_VALUE = TmfStateValue.nullValue();
	
	Map<Integer,Stack<Integer> > threadQuarkToMethodQuarkStackMap = new HashMap<Integer,Stack<Integer>>();
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
                 //String lineno = content.getField(ExecutionTraceStrings.LINE_NUMBER_FIELD).getValue().toString();
                 
                 System.out.println(eventName + " " + methodName);
                 
                 // validate that we have a quark for this class
                 Integer threadQuark=ss.getQuarkAbsoluteAndAdd(Attributes.THREADS, threadId); 
                 System.out.println("added quark="+threadQuark+" for " + threadId); //FIXME
                 
                 Stack<Integer> currentThreadMethodStack = threadQuarkToMethodQuarkStackMap.get(threadQuark);
                 if (currentThreadMethodStack == null) {
                	 currentThreadMethodStack = new Stack<Integer>();
                	 threadQuarkToMethodQuarkStackMap.put(threadQuark, currentThreadMethodStack);
                 }
                 
                 // Note that we cannot actually use the line number as a part of the name here because of the fact
                 // that the line number on exits are actually the line it exited the function, not the closing brace
                 methodName = className + "." + methodName;// + " [line " + lineno + "]";                 
                 
                 // get/create quark for this method call on this thread
                 Integer methodQuark = ss.getQuarkRelativeAndAdd(threadQuark, methodName);
                 System.out.println("added quark="+methodQuark+" for " + methodName); //FIXME
                 
                 //String fullName = ss.getFullAttributePath(methodQuark);
                 
                 ITmfStateValue threadValue;
                 
                 //update the state for this method quark
                 int methodStatusQuark = ss.getQuarkRelativeAndAdd(methodQuark, Attributes.STATUS);
                 System.out.println("added STATUS quark="+methodStatusQuark+" for " + methodName); //FIXME
                 if (isEntry) {
                	 //we are entering this function, we need to get the value currently on the stack and add 
                	 //a null value for it (ending it) and then emit a new running value for this method
                	 if (!currentThreadMethodStack.empty()) {
                		 Integer currentMethodQuark = currentThreadMethodStack.peek();	
                		 int currentMethodStatusQuark = ss.getQuarkRelativeAndAdd(currentMethodQuark,Attributes.STATUS);
                		 System.out.println("added STATUS quark="+currentMethodStatusQuark+" for pushed method quark=" + currentMethodQuark); //FIXME
                		 ss.modifyAttribute(ts, NULL_VALUE, currentMethodStatusQuark);	                	 
                	 }
                	 value = RUNNING_VALUE;
                	 threadValue = value;
                	 currentThreadMethodStack.push(methodQuark);
                 } else {
                	  // we are exiting the current method.  We need to pop it off the stack and then reactivate the 
                	 // on that is left on the top of the stack
                	 value = NULL_VALUE;
                	 threadValue = value;
                	 
                	 // it is possible to have this happen depending on when tracing was enabled
                	 if (!currentThreadMethodStack.empty() ) {
                		 currentThreadMethodStack.pop();
                	 }
                	 if (!currentThreadMethodStack.empty()) {
	                	 Integer currentMethodQuark = currentThreadMethodStack.peek();
	                	 if (currentMethodQuark != null) {
	                		 int currentMethodStatusQuark = ss.getQuarkRelativeAndAdd(currentMethodQuark,Attributes.STATUS);
	                		 System.out.println("added STATUS quark="+currentMethodStatusQuark+" for popped method quark=" + currentMethodQuark); //FIXME
	                		 ss.modifyAttribute(ts,RUNNING_VALUE, currentMethodStatusQuark);
	                		 threadValue = RUNNING_VALUE;
	                	 } 
                	 }                	 
                 }
                 ss.modifyAttribute(ts, value, methodStatusQuark);       
                
                 // here we are also going to modify the status quark for the class itself so that it also appears to be running or not                  
                 // in the given interval
                 int threadStatusQuark = ss.getQuarkRelativeAndAdd(threadQuark,Attributes.STATUS);
                 System.out.println("added STATUS quark="+threadStatusQuark+" for threadWithQuark=" + threadQuark); //FIXME
            	 ss.modifyAttribute(ts,threadValue, threadStatusQuark);                
            	
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
