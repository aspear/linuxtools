package org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventType;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/*
 * <contextHierarchy>Data Center/Cluster/Host/VM</contextHierarchy>

<stateDeclaration context="VM" parentInherits="false">
	<state name="Unknown" rgb="0,0,0">
		<value>0</value>
		<value>1</value>
	</state>
	<state name="Powering Up" rgb="0,20,0">
		<value>2</value>
	</state>
	<state name="Running" rgb="0,200,0">
		<value>3</value>
	</state>
</stateDeclaration> 

<stateChange context="VM" eventType="VmPowerEvent" eventFieldToExtractStateFrom="message" eventFieldToExtractStateFromRegex="blahblah(\d+)blahblah"/>
<stateChange context="VM" eventType="VmRunningEvent" eventFieldToExtractStateFrom="message" eventFieldToExtractStateFromRegex="blahblah(\d+)blahblah"/>

 */
public class StateInfo {
	
	private static final String CONTEXT_HIERARCHY_ELEMENT = "contextHierarchy";
	private static final String STATE_DECLARATION_ELEMENT = "stateDeclaration";		
	
	/**
	 * class to represent the logic to extract a value from an event field.  The 
	 * value can be hard coded (in which case the event fields don't matter), or can come from a 
	 * regex executed on the field
	 * @author aspear
	 *
	 */
	private static class EventFieldValue {
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
	
	private static class StateChangeInfo {
		private final String eventType;
	    private final EventFieldValue stateValue;
		
		public StateChangeInfo(String eventType,EventFieldValue stateValue) {
			this.eventType = eventType;
		
			this.stateValue = stateValue;
		}		
	
		public String getState( ITmfEvent event ) {
			return stateValue.getValue(event);          
		}
	}
	
	private static class ContextChangeInfo {
		private final String eventType;
		private final EventFieldValue parentContextValue;
		private final EventFieldValue thisContextValue;
		
		public ContextChangeInfo(String eventType,EventFieldValue parentContextValue,EventFieldValue thisContextValue) {
			this.eventType = eventType;
			this.parentContextValue = parentContextValue;
			this.thisContextValue = thisContextValue;
		}		
				
		public String getParent( ITmfEvent event ) {
			return parentContextValue.getValue(event);          
		}
		public String getContext( ITmfEvent event ) {
			return thisContextValue.getValue(event);          
		}
	}
	
	
	private Map<String,StateChangeInfo> eventTypeToStateChangeInfoMap = new HashMap<String,StateChangeInfo>();
	private Map<String,ContextChangeInfo> eventTypeToContextChangeInfoMap = new HashMap<String,ContextChangeInfo>();
	private Map<String,StateItem> stateValueToStateMap = new HashMap<String,StateItem>();
	
	private StateItem[] states = null;
	
	/**
	 * parse XML input stream that contains a description of all states and rules to recreate state.
	 * @param inputStream
	 * @throws IOException
	 */
	public void parseInfoText(InputStream inputStream) throws IOException {
		Map<String,StateChangeInfo> localEventTypeToStateChangeInfoMap = new HashMap<String,StateChangeInfo>();
		 Map<String,ContextChangeInfo> localEventTypeToContextChangeInfoMap = new HashMap<String,ContextChangeInfo>();
		
		
		DocumentBuilderFactory docBldrFactory;
        DocumentBuilder docBldr;
        Document configDoc;

        docBldrFactory = DocumentBuilderFactory.newInstance();
        try {
            docBldr = docBldrFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
             throw new IOException(e);
        }
        
        try {
			configDoc = docBldr.parse(inputStream);
		} catch (SAXException e) {
			 throw new IOException(e);
		}
        
        //NodeList hierarchyNodes = configDoc.getElementsByTagName(CONTEXT_HIERARCHY_ELEMENT);
        //if ((hierarchyNodes == null) || (hierarchyNodes.getLength() > 1)) {
        //   throw new IOException("There can be only one " + CONTEXT_HIERARCHY_ELEMENT);
        //}        
        //Element hierarchyNode = (Element) hierarchyNodes.item(0);
        //Element nameElement = (Element) format.getElementsByTagName("Name").item(0);
        //String contextHierarchy = hierarchyNode.getTextContent();
       
        parseStateDeclarations(configDoc);
        
        //<stateChange context="VM" eventType="VmPowerEvent" eventFieldToExtractStateFrom="message" eventFieldToExtractStateFromRegex="blahblah(\d+)blahblah"/>
        
        NodeList addContextNodes = configDoc.getElementsByTagName("addContext");        
        for (int i = 0; i < addContextNodes.getLength(); ++i) {
            Element addContextElement = (Element)addContextNodes.item(i);
                                 
            String eventType = addContextElement.getAttribute("eventType");            
            EventFieldValue valueParentContext = parseFieldValue(addContextElement,"valueParentContext");
            EventFieldValue valueCurrentContext = parseFieldValue(addContextElement,"valueCurrentContext");
            localEventTypeToContextChangeInfoMap.put(eventType,  new ContextChangeInfo(eventType,valueParentContext,valueCurrentContext));
        }
        
       /*<stateChange context="Method" 
	             eventType="lttng_ust_java:function_entry_event">
	             <valueParentContext field="int_tid" fieldRegex="(.*)"/>
	             <valueEventContext field="string_method" fieldRegex="(.*)"/>
	             <valueState value="1"/>
		</stateChange> */
        NodeList stateChangeNodes = configDoc.getElementsByTagName("stateChange");        
        for (int i = 0; i < stateChangeNodes.getLength(); ++i) {
            Element stateChangeElement = (Element)stateChangeNodes.item(i);
                       
            String eventType = stateChangeElement.getAttribute("eventType");            
            EventFieldValue valueState = parseFieldValue(stateChangeElement,"valueState");          
            localEventTypeToStateChangeInfoMap.put(eventType,new StateChangeInfo(eventType,valueState));
        }
        
        // update the members with the new maps
        this.eventTypeToStateChangeInfoMap = localEventTypeToStateChangeInfoMap;
        this.eventTypeToContextChangeInfoMap = localEventTypeToContextChangeInfoMap;	
	}
	
	private void parseStateDeclarations(Document configDoc) {
		
		Map<String,StateItem> localStateValueToStateMap = new HashMap<String,StateItem>();
		List<StateItem> stateItemList = new ArrayList<StateItem>();
		 /*
        <stateDeclaration context="VM" parentInherits="false">
    	<state name="Unknown" rgb="0,0,0">
    		<value>0</value>
    		<value>1</value>
    	</state>
    	<state name="Powering Up" rgb="0,20,0">
    		<value>2</value>
    	</state>
    	<state name="Running" rgb="0,200,0">
    		<value>3</value>
    	</state>
    	</stateDeclaration>
         */ 
        NodeList stateDeclarationNodes = configDoc.getElementsByTagName(STATE_DECLARATION_ELEMENT);        
        for (int i = 0; i < stateDeclarationNodes.getLength(); ++i) {
            Element stateDeclaration = (Element)stateDeclarationNodes.item(i);
            String context = stateDeclaration.getAttribute("context");
            NodeList stateNodes = stateDeclaration.getElementsByTagName("state");
            for (int s=0;s<stateNodes.getLength();++s) {
            	Element stateElem = (Element)stateNodes.item(s);
            	String stateName = stateElem.getAttribute("name");
            	RGB rgb = null;
            	String[] rgbString = stateElem.getAttribute("rgb").split(",");
            	if (rgbString.length == 3) {
            		//TODO error checking
            		rgb = new RGB(Integer.parseInt(rgbString[0]),Integer.parseInt(rgbString[1]),Integer.parseInt(rgbString[2]));
            	}
            	StateItem stateItem = new StateItem(rgb,stateName);
            	stateItemList.add(stateItem);
            	
            	//extract all possible values and add them to the map to this stateItem
            	 NodeList valueNodes = stateElem.getElementsByTagName("value");
                 for (int v=0;v<valueNodes.getLength();++v) {
                 	Element valueElem = (Element)stateNodes.item(v);
                 	String value = valueElem.getTextContent();
                 	if (value !=  null) {
                 		localStateValueToStateMap.put(value.trim(), stateItem);
                 	}                 	
                 }           	
            }
            
        }
        this.stateValueToStateMap = localStateValueToStateMap;
        this.states = stateItemList.toArray(new StateItem[0]);
	}
	
	/*
	 *   <valueEventContext field="string_method" fieldRegex="(.*)"/>
	             <valueState value="1"/>
	 */
	private EventFieldValue parseFieldValue( Element parentElement, String childElementName ) {
		NodeList valueContextNodes = parentElement.getElementsByTagName(childElementName);     
		if (valueContextNodes != null && valueContextNodes.getLength() > 0) {
			Element eventFieldValueElement = (Element)valueContextNodes.item(0);
			
			//first check to see if we have a constant value defined.
			Attr valueNode = eventFieldValueElement.getAttributeNode("value");
			if (valueNode != null) {
				String value = valueNode.getNodeValue();
				return new EventFieldValue(value);
			}
				
			Attr fieldNode = eventFieldValueElement.getAttributeNode("field");
			if (fieldNode != null) {
				//yes there is a field value, so we are going to use it.  there is OPTIONALLY a regex for it.
				String fieldName = fieldNode.getNodeValue();
				String fieldRegex = null;
				Attr fieldRegexNode = eventFieldValueElement.getAttributeNode("field");
				if (fieldRegexNode != null) {
					fieldRegex = fieldRegexNode.getNodeValue();
				}
				return new EventFieldValue(fieldName,fieldRegex);
			}
			// TODO logging
		}
        return null;
	}
	
	/**
	 * get all states supported for this context
	 * @return
	 */
	public final StateItem[] getAllStates() {
		return states;
	}
	
	/** iff the given event contains context changes a string array is returned
	 * that contains 0 the parent, 1, the current context.  If the event does not
	 * have context info then null is returned.  It is possible to get one or the 
	 * other of the parent and current context as null values, but not both
	 * @param event
	 * @return
	 */
	public String[] getContext( ITmfEvent event ) {
		
		ITmfEventType eventType = event.getType();
		if (eventType == null) {
			return null;
		}
		
		String eventTypeName = eventType.getName();
		
		// first check to see if there is any change in the contexts for this event type
		ContextChangeInfo contextChangeInfo = eventTypeToContextChangeInfoMap.get(eventTypeName);
		if (contextChangeInfo != null) {
			String parent = contextChangeInfo.getParent(event);
			String current = contextChangeInfo.getContext(event);
			if (parent != null || current != null) {
				return new String[]{parent,current};
			}
		}		
		return null;
	}
	
	/**
	 * get a new state for the given event if there is one
	 * @param ev
	 * @return
	 */
	public StateItem getNewState( ITmfEvent event ) {

		ITmfEventType eventType = event.getType();
		if (eventType == null) {
			return null;
		}		
		String eventTypeName = eventType.getName();		
		StateChangeInfo stateChangeInfo = eventTypeToStateChangeInfoMap.get(eventTypeName);
		if (stateChangeInfo != null) {
			String newStateString = stateChangeInfo.getState(event);
			if (newStateString != null) {
				return stateValueToStateMap.get(newStateString);
			}
		}	
		return null;
	}
}
