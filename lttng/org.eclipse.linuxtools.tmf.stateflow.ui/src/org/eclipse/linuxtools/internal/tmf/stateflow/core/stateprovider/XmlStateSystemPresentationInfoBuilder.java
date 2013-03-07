package org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider.StateSystemPresentationInfo.ContextInfo;
import org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider.StateSystemPresentationInfo.EventFieldValue;
import org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider.StateSystemPresentationInfo.StateChangeInfo;
import org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider.StateSystemPresentationInfo.StatePresentationInfo;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.statesystem.IStatePresentationInfo;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemContextHierarchyInfo;
import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlStateSystemPresentationInfoBuilder {
	
	public static final String SCHEMA_XML_FILE_EXTENSION 	= ".state-schema.xml";	
	private static final String DEFINE_CONTEXT_ELEMENT 		= "defineContext";
	private static final String SWITCH_CONTEXT_ELEMENT 		= "switchContext";
	private static final String PUSH_CONTEXT_ELEMENT 		= "pushContext";
	private static final String POP_CONTEXT_ELEMENT 		= "popContext";
	private static final String STATE_CHANGE_ELEMENT		= "stateChange";
	private static final String FIELD_ATTRIBUTE 			= "field";	
	private static final String STATE_DECLARATION_ELEMENT 	= "stateDeclaration";		
	
	private Map<String,StateChangeInfo> 		eventTypeToStateChangeInfoMap;
	private Map<String,ContextInfo> 			eventTypeToSwitchContextMap;
	private Map<String,ContextInfo> 			eventTypeToPushContextMap;
	private Map<String,ContextInfo> 			eventTypeToPopContextMap;
	private Map<String,IStatePresentationInfo>	stateStringToStateMap;
	private IStatePresentationInfo[] 			states;
	private IStateSystemContextHierarchyInfo[] 	contextHierarchy;
	
	/**
	 * build a state presentation info on the given stream.  will close the stream when done
	 * @param stateSchemaXmlStream
	 * @return
	 * @throws TmfTraceException
	 */
	public static StateSystemPresentationInfo build(InputStream stateSchemaXmlStream) 
			throws TmfTraceException {		
		XmlStateSystemPresentationInfoBuilder instance = new XmlStateSystemPresentationInfoBuilder();
		return instance.buildStateSystem(stateSchemaXmlStream);
	}
	
	private StateSystemPresentationInfo buildStateSystem(InputStream stateSchemaXmlStream) 
			throws TmfTraceException {
		
		try {
			parseInfoXml(stateSchemaXmlStream);
			stateSchemaXmlStream.close();
		}catch(Exception e) {
			throw new TmfTraceException(e.toString(), e);
		} 			
		
		StateSystemPresentationInfo stateSystemPresentationInfo = new StateSystemPresentationInfo(
				eventTypeToSwitchContextMap,
				eventTypeToPushContextMap,
				eventTypeToPopContextMap,
				eventTypeToStateChangeInfoMap,				
				stateStringToStateMap,
				states,
				contextHierarchy);
		return stateSystemPresentationInfo;
	}
	
	/**
	 * utility used to recursively iterate through a set of nodes and return a list of context heirarchies for them
	 * @param allChildNodes
	 * @return
	 */
	private IStateSystemContextHierarchyInfo[] getContextHierarchy( NodeList defineContextElements ) {
		if ((defineContextElements == null) || defineContextElements.getLength() == 0) {
			return null;
		}
		List<IStateSystemContextHierarchyInfo> returnValue = null;
		int childCount = defineContextElements.getLength();
		for (int i=0;i<childCount;++i) {
			Element c = (Element)defineContextElements.item(i);
			//we got an element of the right type which means we are going to return at least one
			if (returnValue == null) {
				returnValue = new ArrayList<IStateSystemContextHierarchyInfo>();
			}
			String id = c.getAttribute("id");
			String parentId = c.getAttribute("parentId");
			boolean hasState = "true".equals(c.getAttribute("hasState"));
			
			StateSystemContextHierarchyInfo hierarchyInfo = new StateSystemContextHierarchyInfo(id,parentId,hasState);
			returnValue.add(hierarchyInfo);		
			
			//TODO maybe a local map and order them correctly by parent regardless of what the user does?
		}
		if (returnValue != null) {
			return returnValue.toArray(new StateSystemContextHierarchyInfo[0]);
		} else {
			return null;
		}
	}
	
	/** who was the idiot that designed the Java XML interfaces anyway? this stupid boilerplate ought to be
	 * in a helper method somewhere. */
	private static List<Element> getChildElements(Node n, final String elementName) {
		NodeList children = n.getChildNodes();
		List<Element> returnElements = new ArrayList<Element>(children.getLength());
		for (int i=0;i<children.getLength();++i) {
			Node c = children.item(i);
			if (c.getNodeType() == Node.ELEMENT_NODE && c.getNodeName().equals(elementName)) {
				returnElements.add((Element)c);
			}
		}
		return returnElements;
	}
		
	/**
	 * parse XML input stream that contains a description of all states and rules to recreate state.
	 * @param inputStream
	 * @throws IOException
	 */
	public void parseInfoXml(InputStream inputStream) throws IOException {
		eventTypeToStateChangeInfoMap = new HashMap<String,StateChangeInfo>();
		eventTypeToSwitchContextMap = new HashMap<String,ContextInfo>();
		eventTypeToPushContextMap = new HashMap<String,ContextInfo>();
		eventTypeToPopContextMap = new HashMap<String,ContextInfo>();	
				
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
        
        // lets find all context definition elements. There can be multiple elements for a hierarchy assumed
        // to be in order in the file                
       	contextHierarchy = getContextHierarchy(configDoc.getElementsByTagName(DEFINE_CONTEXT_ELEMENT));
       	
       	if (contextHierarchy == null || contextHierarchy.length == 0) {
       		throw new IOException("State XML must have a least one " + DEFINE_CONTEXT_ELEMENT + " element.");
       	}
              
        parseStateDeclarations(configDoc);
        /*        
        <setContext  eventType="com.vmware.vim25.VmStartingEvent">
        	<valueContext id="Datacenter" field="Datacenter" fieldRegex="(.*)"/>
        	<valueContext id="Host"       field="Host"       fieldRegex="(.*)"/>
        	<valueContext id="VM"         field="VM"         fieldRegex="(.*)"/>
		</setContext>
 		*/	
        parseContextElements(configDoc,SWITCH_CONTEXT_ELEMENT,eventTypeToSwitchContextMap);
        parseContextElements(configDoc,PUSH_CONTEXT_ELEMENT,eventTypeToPushContextMap);
        parseContextElements(configDoc,POP_CONTEXT_ELEMENT,eventTypeToPopContextMap);
	
       /*<stateChange eventType="com.vmware.vim25.VmStartingEvent" contextId="VM">
	       <valueState value="Starting"/>
		</stateChange>*/
        NodeList stateChangeNodes = configDoc.getElementsByTagName(STATE_CHANGE_ELEMENT);        
        for (int i = 0; i < stateChangeNodes.getLength(); ++i) {
            Element stateChangeElement = (Element)stateChangeNodes.item(i);
            List<Element> valueStateElements = getChildElements(stateChangeElement,"valueState");
            if (valueStateElements.size() != 1) {
            	throw new IllegalArgumentException("stateChange must one and only one valueState children");            
            }
            String eventType = stateChangeElement.getAttribute("eventType");            
            EventFieldValue valueState = parseFieldValue(valueStateElements.get(0));          
            eventTypeToStateChangeInfoMap.put(eventType,new StateSystemPresentationInfo.StateChangeInfo(valueState));
        }        
	}
	
	private void parseContextElements( Document configDoc, String elementName, Map<String,ContextInfo> nameToContextInfoMap) {
		NodeList addContextNodes = configDoc.getElementsByTagName(elementName);        
        for (int i = 0; i < addContextNodes.getLength(); ++i) {
            Element setContextElement = (Element)addContextNodes.item(i);
            String eventType = setContextElement.getAttribute("eventType");            
            List<Element> valueContextElements = getChildElements(setContextElement,"valueContext");
            //allocate an array of context values that is the same size and order as the hierarchy
            EventFieldValue[] hierarchyContextValues = new EventFieldValue[contextHierarchy.length];
            for (int c=0;c<contextHierarchy.length;++c) {
            	String contextId = contextHierarchy[c].getContextId();
            	for (Element valueContext : valueContextElements) {
		           	 String thisContextId = valueContext.getAttribute("id");
		           	 if (contextId.equals(thisContextId)) {
		           		hierarchyContextValues[c] = parseFieldValue(valueContext);
		           		break;
		           	 }             	  	  
                } 
            }                                                
            nameToContextInfoMap.put(eventType,  new StateSystemPresentationInfo.ContextInfo(hierarchyContextValues));
        }
	}
	
	private void parseStateDeclarations(Document configDoc) {
		
		stateStringToStateMap = new HashMap<String,IStatePresentationInfo>();
		List<StatePresentationInfo> stateItemList = new ArrayList<StatePresentationInfo>();
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
            //TODO support for different contexts for state changes? 
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
            	StatePresentationInfo stateItem;
            	if (rgb == null) {
            		// there is no color defined for this one.  That means that this state should not be displayed, so here we are
            		// going to clobber the index of the state, which means to use a null state value.  the RGB will be ignored in 
            		// this case, but we must create a valid value or SWT will not be happy.
            		stateItem = new StatePresentationInfo(stateName,new RGB(255,255,255),-1);
            	} else {
            		stateItem = new StatePresentationInfo(stateName,rgb,s);
            	}
            	stateItemList.add(stateItem);
            	
            	//extract all possible string values that map to this state, and add them to the map to this stateItem
            	 NodeList valueNodes = stateElem.getElementsByTagName("value");
                 for (int v=0;v<valueNodes.getLength();++v) {
                 	Element valueElem = (Element)valueNodes.item(v);
                 	String value = valueElem.getTextContent();
                 	if (value !=  null) {
                 		stateStringToStateMap.put(value.trim(), stateItem);
                 	}                 	
                 }           	
            }
            
        }
        this.states = stateItemList.toArray(new IStatePresentationInfo[0]);
	}
	
	/*
	 *   <valueEventContext field="string_method" fieldRegex="(.*)"/>
	             <valueState value="1"/>
	 */
	private EventFieldValue parseFieldValue( Element eventFieldValueElement ) {
					
		//first check to see if we have a constant value defined.
		String value = eventFieldValueElement.getAttribute("value");
		if (value != null && !value.isEmpty()) {
			return new EventFieldValue(value);
		}
		
		String fieldName = eventFieldValueElement.getAttribute("field");
		if (fieldName != null && !fieldName.isEmpty()) {		
			//yes there is a field value, so we are going to use it.  there is OPTIONALLY a regex for it.			
			String fieldRegex = eventFieldValueElement.getAttribute("fieldRegex");
			if (fieldRegex != null && fieldRegex.isEmpty()) {
				fieldRegex = null;
			}
			return new EventFieldValue(fieldName,fieldRegex);
		}
		return null;		
	}
}
