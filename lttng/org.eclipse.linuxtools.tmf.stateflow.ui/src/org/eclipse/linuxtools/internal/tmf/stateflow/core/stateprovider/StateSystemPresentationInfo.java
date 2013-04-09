/*******************************************************************************
 * Copyright (c) 2013 VMware Inc.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.statesystem.IStatePresentationInfo;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemContextHierarchyInfo;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemPresentationInfo;
import org.eclipse.linuxtools.tmf.core.statevalue.ITmfStateValue;
import org.eclipse.linuxtools.tmf.core.statevalue.TmfStateValue;
import org.eclipse.swt.graphics.RGB;

/**
 * Implementation of IStateSystemPresentationInfo that is driven by a set of
 * data structures that are supplied by a builder which takes them from a particular
 * format (e.g. XML)
 * @author Aaron Spear
 */
public class StateSystemPresentationInfo implements IStateSystemPresentationInfo {

	/**
	 * class to represent the logic to extract a value from an event field.  The
	 * value can be hard coded (in which case the event fields don't matter), or can come from a
	 * regex executed on the field
	 * @author aspear
	 *
	 */
	static class EventFieldValue {
		private final String fieldName;
	    private final Pattern fieldRegexPattern;
		private final String fixedValue;

		/**
		 * construct an EventValue that uses a fieldName and optional regex
		 * @param fieldName the field name to extract from the event
		 * @param fieldRegex if null, then the field value in its entirety is returned
		 */
		public EventFieldValue(String fieldName, String fieldRegex) {
			if (fieldName == null || fieldName.isEmpty()) {
				throw new IllegalArgumentException("You must provide a valid value for fieldName");
			}
			this.fieldName = fieldName;
			if (fieldRegex != null) {
				this.fieldRegexPattern = Pattern.compile(fieldRegex);
			}else {
				this.fieldRegexPattern = null;
			}
			this.fixedValue = null;
		}
		/**
		 * a fixed value to return
		 * @param fixedValue
		 */
		public EventFieldValue(String fixedValue) {
			fieldName = null;
			fieldRegexPattern = null;
			this.fixedValue = fixedValue;
		}

		/**
		 * use regex if applicable to extract the state value from the given field
		 * @param fieldValue the input field
		 * @return extracted state value.  either the output of the regex or the field itself
		 */
		public String getValue( ITmfEvent event ) {
            if (fixedValue != null) {
            	return fixedValue;
            }
            // need to extract teh field value
    		ITmfEventField field = event.getContent().getField(fieldName);
    		if (field == null) {
    			//this is an error condition TODO logging
    			return null;
    		}

    		// TODO smarter type conversion?
    		String fieldValueString = field.getValue().toString();
    		if (fieldValueString == null) {
    			// this is an error condition indicating that the state value didn't match what we expected in the regex
    			// TODO logging
    			return null;
    		}

			if (fieldRegexPattern != null) {
            	Matcher matcher = fieldRegexPattern.matcher(fieldValueString);
            	if (matcher.matches()) {
                    return fieldValueString.substring(matcher.start(1), matcher.end(1));
            	}
            	return null;
            } else {
            	return fieldValueString.trim();
            }
		}
	}

	static class StateChangeInfo {
	    private final EventFieldValue stateValue;

		public StateChangeInfo(final EventFieldValue stateValue) {
			this.stateValue = stateValue;
		}

		public String getState( ITmfEvent event ) {
			return stateValue.getValue(event);
		}
	}

	static class ContextInfo {
		private final EventFieldValue[] hierarchyContextValues;
		private final EventFieldValue previousContextStateValue;

		public ContextInfo(final EventFieldValue[] hierarchyContextValues, final EventFieldValue previousContextStateValue ) {
			this.hierarchyContextValues = hierarchyContextValues;
			this.previousContextStateValue = previousContextStateValue;
		}

		public String[] getContext( ITmfEvent event ) {
			// if this even gets called then that means that this event has some context context, so allocate a new context
			String[] context = new String[hierarchyContextValues.length];
			for (int c=0;c<hierarchyContextValues.length;++c) {
				EventFieldValue contextValue = hierarchyContextValues[c];
				if (contextValue != null) {
					context[c] = hierarchyContextValues[c].getValue(event);
				} else {
					context[c] = null;
				}
			}
			return context;
		}
		public String getPreviousContextStateValue(ITmfEvent event ) {
			if (previousContextStateValue != null) {
				return previousContextStateValue.getValue(event);
			}
			return null;
		}
	}

	static class StatePresentationInfo implements IStatePresentationInfo {

		private final RGB 			stateColor;
		private final String 			stateString;
		private ITmfStateValue 	stateValue;

		public StatePresentationInfo(String stateString, RGB stateColor, int stateIndex) {
			this.stateString = stateString;
			this.stateColor = stateColor;
			if (stateIndex >= 0) {
				this.stateValue = TmfStateValue.newValueInt(stateIndex);
			} else {
				this.stateValue = TmfStateValue.nullValue();
			}
		}

		@Override
		public String getStateString() {
			return stateString;
		}

		@Override
		public RGB getStateColor() {
			return stateColor;
		}

		@Override
		public ITmfStateValue getStateValue() {
	    	return stateValue;
		}
	}

	private final Map<String,ContextInfo> 			eventTypeToSwitchContextMap;
	private final Map<String,ContextInfo> 			eventTypeToPushContextMap;
	private final Map<String,ContextInfo> 			eventTypeToPopContextMap;

	private final Map<String,StateChangeInfo> 			eventTypeToStateChangeInfoMap;
	private final Map<String,IStatePresentationInfo>	stateStringToStateMap;
	private final IStatePresentationInfo[] 				states;
	private final IStateSystemContextHierarchyInfo[] 	contextHierarchy;

	public StateSystemPresentationInfo(

			Map<String,ContextInfo> 			eventTypeToSwitchContextMap,
			Map<String,ContextInfo> 			eventTypeToPushContextMap,
			Map<String,ContextInfo> 			eventTypeToPopContextMap,
			Map<String,StateChangeInfo> 		eventTypeToStateChangeInfoMap,
			Map<String,IStatePresentationInfo>	stateStringToStateMap,
			IStatePresentationInfo[] 			states,
			IStateSystemContextHierarchyInfo[]  contextHierarchy) {

		this.eventTypeToSwitchContextMap = eventTypeToSwitchContextMap;
		this.eventTypeToPushContextMap   = eventTypeToPushContextMap;
		this.eventTypeToPopContextMap    = eventTypeToPopContextMap;
		this.eventTypeToStateChangeInfoMap = eventTypeToStateChangeInfoMap;
		this.stateStringToStateMap = stateStringToStateMap;
		this.states = states;
		this.contextHierarchy = contextHierarchy;
	}

	@Override
	public IStatePresentationInfo getPresentationForStateValue(ITmfStateValue stateValue) {
		return stateStringToStateMap.get(stateValue.toString());
	}

	@Override
	public IStateSystemContextHierarchyInfo[] getContextHierarchy() {
		return contextHierarchy;
	}

	@Override
	public IStatePresentationInfo[] getAllStates() {
		return states;
	}

	private ContextChangeInfo getContextChangeInfo(final ContextInfo contextChangeInfo, final String eventTypeName, final ITmfEvent event ) {
		if (contextChangeInfo != null) {
			String[] id = contextChangeInfo.getContext(event);
			if (id != null) {
				String updatedPreviousContextStateString = contextChangeInfo.getPreviousContextStateValue(event);
				return new ContextChangeInfo(id,stateStringToStateMap.get(updatedPreviousContextStateString));
			}
		}
		return null;
	}

	@Override
	public ContextChangeInfo getSwitchContext( String eventTypeName, ITmfEvent event ) {
		return getContextChangeInfo(eventTypeToSwitchContextMap.get(eventTypeName),eventTypeName,event);
	}
	@Override
	public ContextChangeInfo getPushContext(String eventTypeName, ITmfEvent event) {
		return getContextChangeInfo(eventTypeToPushContextMap.get(eventTypeName),eventTypeName,event);
	}

	@Override
	public ContextChangeInfo getPopContext(String eventTypeName, ITmfEvent event) {
		return getContextChangeInfo(eventTypeToPopContextMap.get(eventTypeName),eventTypeName,event);
	}

	@Override
	public IStatePresentationInfo getNewState( String eventTypeName, ITmfEvent event ) {
		StateChangeInfo stateChangeInfo = eventTypeToStateChangeInfoMap.get(eventTypeName);
		if (stateChangeInfo != null) {
			// get the string for the new state
			String newStateString = stateChangeInfo.getState(event);
			if (newStateString != null) {
				return stateStringToStateMap.get(newStateString);
			}
		}
		return null;
	}

}
