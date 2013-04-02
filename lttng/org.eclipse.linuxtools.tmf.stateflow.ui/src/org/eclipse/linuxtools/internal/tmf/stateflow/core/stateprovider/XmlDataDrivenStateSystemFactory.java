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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider.StateSystemPresentationInfo.ContextInfo;
import org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider.StateSystemPresentationInfo.EventFieldValue;
import org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider.StateSystemPresentationInfo.StateChangeInfo;
import org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider.StateSystemPresentationInfo.StatePresentationInfo;
import org.eclipse.linuxtools.tmf.core.TmfCommonConstants;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateChangeInput;
import org.eclipse.linuxtools.tmf.core.statesystem.IStatePresentationInfo;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemContextHierarchyInfo;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemFactory;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystem;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystemBuilder;
import org.eclipse.linuxtools.tmf.core.statesystem.StateSystemManager;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * IStateSystemFactory implementation that looks for a state-schema.xml file
 * for the given trace, and if it exists, it creates a
 *
 * in the same directory as the trace by default
 * checks to see
 * @author Aaron Spear
 *
 */
public class XmlDataDrivenStateSystemFactory implements IStateSystemFactory {

	public static final String XML_SCHEMA_FILE_EXTENSION = ".state-schema.xml"; //$NON-NLS-1$

	/**
     * The schema file location property of a trace resource.  A derived trace
     * can set this property to utilize a shared schema file location instead of
     * one that is always next to the trace.
     */
    public static final QualifiedName STATE_SCHEMA_FILE_PATH = new QualifiedName("org.eclipse.linuxtools.tmf", "trace.state-schema.file"); //$NON-NLS-1$//$NON-NLS-2$

	/**
	 * The file name of the History Tree
	 */
	public final static String HISTORY_TREE_FILE_NAME = "stateHistory.ht"; //$NON-NLS-1$

	public XmlDataDrivenStateSystemFactory() {
	}

	@Override
	public String getStateSystemId() {
		return DataDrivenStateInput.STATE_SYSTEM_ID;
	}

	@Override
	public boolean canCreateStateSystem(ITmfTrace trace) {
		File stateSchemaXml = createStateSchemaFile(trace);
		return (stateSchemaXml.exists() && stateSchemaXml.isFile());
	}

	private ITmfStateSystem createStateSystem(ITmfTrace trace, StateSystemPresentationInfo statePresentationInfo) throws TmfTraceException {

		IResource resource = trace.getResource();
		String supplDirectory = null;

		try {
			// get the directory where the history file will be stored.
			supplDirectory = resource.getPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER);
		} catch (CoreException e) {
			throw new TmfTraceException(e.toString(), e);
		}

		final File htFile = new File(supplDirectory + File.separator + HISTORY_TREE_FILE_NAME);

		// for the ID of the state system, use the value from DataDrivenStateInput, this value is used in the
		// view
		final IStateChangeInput htInput = new DataDrivenStateInput(statePresentationInfo, trace, trace.getEventType(),
				DataDrivenStateInput.STATE_SYSTEM_ID);

		ITmfStateSystem ss = StateSystemManager.loadStateHistory(htFile, htInput, false);

		// FIXME this is not really elegant, but I have not figured out the
		// right way to do this: in the case that this
		// ss is a load of an existing state system from disk, it would appear
		// that the IStateChangeInput instance is not used,
		// and because of this the state presentation info does not get set in
		// the ss, so we must manually set it here
		if (ss instanceof ITmfStateSystemBuilder) {
			ITmfStateSystemBuilder ssb = (ITmfStateSystemBuilder) ss;
			ssb.setStatePresentationInfo(statePresentationInfo);
		}
		return ss;
	}

	public ITmfStateSystem createStateSystem(ITmfTrace trace, InputStream schemaXmlInputStream) throws TmfTraceException {
		StateSystemPresentationInfo statePresentationInfo = new XmlStateSystemPresentationInfoBuilder().build(schemaXmlInputStream);
		return createStateSystem(trace,statePresentationInfo);
	}


	@Override
	public ITmfStateSystem createStateSystem(ITmfTrace trace) throws TmfTraceException {

		File stateSchemaXml = createStateSchemaFile(trace);
		if (!stateSchemaXml.exists() || !stateSchemaXml.isFile()) {
			return null;
		}
		// the schema file exists! that means we can create a state system for
		// it.
		/* Set up the path to the history tree file we'll use */

		// build presentation from the XML. might throw if the XML is bad.
		StateSystemPresentationInfo statePresentationInfo = buildStatePresentationInfo(stateSchemaXml);
		return createStateSystem(trace,statePresentationInfo);
	}

	protected File createStateSchemaXmlFileNextToTrace(ITmfTrace trace) {

		File traceFile = new File(trace.getPath());

		// in either the case where it is a directory (as is done with CTF) or a
		// trace file, we want to check for the schema file in the parent directory
		File traceDirectory = traceFile.getParentFile();
		if (traceDirectory == null) {
			traceDirectory = traceFile; // no parent, might be OS file system root
		}
		return new File(traceDirectory, trace.getName() + XML_SCHEMA_FILE_EXTENSION);
	}

	/**
	 * create the state schema file. Note that that it may or may not exist...
	 *
	 * @param trace
	 * @return
	 */
	protected File createStateSchemaFile(ITmfTrace trace) {

		// first try to see if the file is next to the trace.  We always use it if so
		File schemaFile = createStateSchemaXmlFileNextToTrace(trace);
		if (schemaFile == null || !schemaFile.exists() || !schemaFile.isFile()) {
			// file doesn't exist next to the trace.  See if this particular trace has had a
			// property set for it giving us the path to the file
			IResource resource = trace.getResource();
			String schemaFilePath = null;
			try {
				// get the directory where the history file will be stored.
				schemaFilePath = resource.getPersistentProperty(STATE_SCHEMA_FILE_PATH);
				if (schemaFilePath != null) {
					File schemaFileFromTraceProperties = new File(schemaFilePath);
					if (schemaFileFromTraceProperties.exists() && schemaFileFromTraceProperties.isFile()) {
						return schemaFileFromTraceProperties;
					}
				}
			} catch (CoreException e) {
			}
		}
		return schemaFile;
	}

	private StateSystemPresentationInfo buildStatePresentationInfo(File stateSchemaXml) throws TmfTraceException {
		InputStream xmlInputStream;
		try {
			xmlInputStream = new BufferedInputStream(new FileInputStream(stateSchemaXml));
		} catch (FileNotFoundException e) {
			throw new TmfTraceException(e.getMessage(), e); // this can't happen
															// here anyway
		}
		return new XmlStateSystemPresentationInfoBuilder().build(xmlInputStream);
	}


private static class XmlStateSystemPresentationInfoBuilder {

	private static final String DEFINE_CONTEXT_ELEMENT 		= "defineContext";
	private static final String SWITCH_CONTEXT_ELEMENT 		= "switchContext";
	private static final String PUSH_CONTEXT_ELEMENT 		= "pushContext";
	private static final String POP_CONTEXT_ELEMENT 		= "popContext";
	private static final String STATE_CHANGE_ELEMENT		= "stateChange";
	private static final String VALUE_PREVIOUS_CONTEXT_STATE_ELEMENT = "valuePreviousContextState";
	//private static final String FIELD_ATTRIBUTE 			= "field";
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
	public XmlStateSystemPresentationInfoBuilder() {
	}

	public StateSystemPresentationInfo build(InputStream stateSchemaXmlStream)
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
            EventFieldValue previousContextStateValue = null;
            //FIXME Not sure what it means to have more than one here.  Maybe one for each level in the hierarchy?
            List<Element> valuePreviousContextStateElements = getChildElements(setContextElement,VALUE_PREVIOUS_CONTEXT_STATE_ELEMENT);
            for (Element valueContext : valuePreviousContextStateElements) {
            	previousContextStateValue = parseFieldValue(valueContext);
	        }

            nameToContextInfoMap.put(eventType,  new StateSystemPresentationInfo.ContextInfo(hierarchyContextValues,previousContextStateValue));
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
            //String context = stateDeclaration.getAttribute("context");
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

}
