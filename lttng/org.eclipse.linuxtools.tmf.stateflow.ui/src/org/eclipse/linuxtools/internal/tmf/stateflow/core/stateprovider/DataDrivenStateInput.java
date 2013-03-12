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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

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
	
	 
    
	private static class ContextStack {
		/**
	     * copy from the source to the destination any values that are not null.  
	     * @param source
	     * @param dest
	     * @param startingLevel
	     */
	    public static void copyContext( final String[] source, final String[] dest, int startingLevel ) {
	    	for (int i=startingLevel;i<source.length;++i) {
	    		if (source[i] != null) {    			
	    			dest[i] = source[i];
	    		}
	    	}
	    }
	    
		final String[] contextId;
		final int contextLevel;
		
		private LinkedHashMap<String,ContextStack> 	contextToChildStackMap = null;
		private Stack<String[]> 			 		stack = null;
		
		public ContextStack(final String[] contextId, int contextLevel) {
			this.contextId = contextId;
			this.contextLevel = contextLevel;
		}
		
		public String[] getId() {
			return contextId;
		}
		
		public int getContextLevel() {
			return contextLevel;
		}
			
		public synchronized ContextStack getChildContextStack( final String childContext, boolean createIfNotExisting ) {
			
			if (contextToChildStackMap == null && createIfNotExisting) {
				contextToChildStackMap = new LinkedHashMap<String,ContextStack>();
			}
			
			if (contextToChildStackMap != null) {
				ContextStack child = contextToChildStackMap.get(childContext);
				if (child == null && createIfNotExisting) {
					final String[] childId = contextId.clone();
					childId[contextLevel+1] = childContext;
					child = new ContextStack(childId,contextLevel+1);
					contextToChildStackMap.put(childContext,child);					
				}
				return child;
			} else {
				return null;
			}
		}
		
		/**
		 * push the CURRENT context id value on the stack and then change the current value
		 * considering the given newContextIds
		 * @param newContextIds
		 */
		public synchronized void push(final String[] newContextIds) {
			getStack().push(contextId);
			copyContext(newContextIds,contextId,this.contextLevel);
		}
		
		/**
		 * push the CURRENT context id value on the stack and then change the current value
		 * considering the given newContextIds
		 * @param newContextIds
		 */
		public synchronized void pop() {
			String[] ids = getStack().pop();
			copyContext(ids,contextId,this.contextLevel);
		}
			
		/**
		 * access to the stack for this instance in the hierarchy.  Note that this 
		 * method does lazy creation of the stack object
		 * @return
		 */
		public synchronized Stack<String[]>  getStack() {
			if (stack == null) {
				stack = new Stack<String[]>();
			}				
			return stack;			
		}
		
		void dispose() {
			if (contextToChildStackMap != null) {
				for (ContextStack child : contextToChildStackMap.values()) {
					child.dispose();
				}				
				contextToChildStackMap.clear();
			}
			if (stack != null) {
				stack.clear();
			}
		}
	}
	
	//************************************************************************
	
	private int currentContextQuark = -1;
	//private String[] currentContextIds;
	private ContextStack currentContextStack=null;
	private int rootQuark = -1;
	private ContextStack rootContextStack;
	
	//************************************************************************

    public DataDrivenStateInput(StateSystemPresentationInfo statePresentationInfo, ITmfTrace trace,Class<? extends ITmfEvent> eventType, String id) {
        super(trace, eventType, id);
        
        this.statePresentationInfo = statePresentationInfo;
        
        // the last contextids are sized for the number of contexts we have in the hierarchy
    	//currentContextIds = new String[statePresentationInfo.getContextHierarchy().length];
    	
    	rootContextStack = new ContextStack(new String[statePresentationInfo.getContextHierarchy().length],-1);
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
     
    /** fill in any missing context ids using the lastContextIds value.   */
    private void completeNewContext( final String[] contextIds ) {
    	if (currentContextStack != null) {
	    	final String[] currentContextIds = currentContextStack.getId();
	    	for (int i=0;i<contextIds.length;++i) {
	    		if (contextIds[i] == null) {    			
	    			contextIds[i] = currentContextIds[i];
	    		} 
	    	}
    	}
    }
    
    private ContextStack getContextStack( final String[] contextIds ) {
    	ContextStack returnValue = rootContextStack.getChildContextStack(contextIds[0],true);
    	int length = contextIds.length;
    	for (int i=1;i<length;++i) {
    		ContextStack child = returnValue.getChildContextStack(contextIds[i], true);
    		returnValue = child;
    	}
    	return returnValue;
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
            	// check to see if there is a context switch in this event
            	final String[] contextIds = statePresentationInfo.getSwitchContext(eventTypeName,event);
            	if (contextIds != null) {
            		
            		// there is, some of the values may be null, which means to get the values for that from
            		// the current context
            		completeNewContext(contextIds);
            		
            		// switch the lastContextStack to be the value indicated by this context.            		
            		currentContextStack = getContextStack(contextIds);
            		
            		String[] newId = currentContextStack.getId();
            		
            		currentContextQuark = ss.getQuarkRelativeAndAdd(rootQuark, newId);
            	}
            	
            	// the context may or may not have been switched by the code above.  Lets check to see if there
            	// are any context pushes in this event.  That means to take the 
            	final String[] pushedContextIds = statePresentationInfo.getPushContext(eventTypeName,event);
            	if (pushedContextIds != null) {
            		currentContextStack.push(pushedContextIds);
            		currentContextQuark = ss.getQuarkRelativeAndAdd(rootQuark, currentContextStack.getId());
            	}    
            	
            	final String[] poppedContextIds = statePresentationInfo.getPopContext(eventTypeName,event);
            	if (pushedContextIds != null) {
            		currentContextStack.pop();
            		currentContextQuark = ss.getQuarkRelativeAndAdd(rootQuark, currentContextStack.getId());
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
