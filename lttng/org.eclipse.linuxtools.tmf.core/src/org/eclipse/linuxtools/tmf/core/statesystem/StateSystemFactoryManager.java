/*******************************************************************************
 * Copyright (c) 2013 VMware Inc.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.statesystem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

/**
 * Singleton class that manages loading of instances of IStateSystemFactory that are provided
 * by plugins through an extension point.  It does loading of interfaces on demand as needed,
 * trying to defer plugin loading if possible
 * @author Aaron Spear
 * @since 2.0
 */
public class StateSystemFactoryManager {

    private static final String STATE_SYSTEM_FACTORY_EXTENSION = "org.eclipse.linuxtools.tmf.ui.statesystemfactory"; //$NON-NLS-1$
    private static final String STATE_SYSTEM_TYPE_CHILD = "type"; //$NON-NLS-1$
    private static final String STATE_SYSTEM_CLASS_ATTRIBUTE = "class"; //$NON-NLS-1$
    private static final String STATE_SYSTEM_ID_ATTRIBUTE = "state_system_id"; //$NON-NLS-1$

    private static class FactoryContainer {
        IConfigurationElement configElement=null;
        IStateSystemFactory   stateSystemFactory=null;

        FactoryContainer(IConfigurationElement configElement) {
            this.configElement = configElement;
            stateSystemFactory=null;
        }
        /**
         * demand loads the factory from the configuration element as needed
         * @return
         */
        IStateSystemFactory getFactory() {
            if (stateSystemFactory == null) {
                try {
                    //instantiate the factory class
                    stateSystemFactory = ((IStateSystemFactory)configElement.createExecutableExtension(STATE_SYSTEM_CLASS_ATTRIBUTE));
                }catch(CoreException e) {
                    e.printStackTrace(); //TODO better logging
                }
            }
            return stateSystemFactory;
        }
    }

    private LinkedHashMap<String,FactoryContainer> idToFactoryMap = null;

    private static final StateSystemFactoryManager INSTANCE = new StateSystemFactoryManager();

    /**
     * @return singleton StateSystemFactoryManager
     */
    public static StateSystemFactoryManager getInstance() {
        return  INSTANCE;
    }

    /**
     * call to get a list of factories that are applicable for the given trace.  The
     * list has been qualified to ones that work for the given trace
     * @param trace the trace for which to query for available state system factories
     * @return a list of factories that are applicable.  Will be an an empty list if none.
     */
    public List<IStateSystemFactory> getFactoriesForTrace( ITmfTrace trace ) {

        loadExtensions();

        List<IStateSystemFactory> returnList = new ArrayList<IStateSystemFactory>(idToFactoryMap.size());

        for (Entry<String,FactoryContainer> entry : idToFactoryMap.entrySet()) {
            IStateSystemFactory f = entry.getValue().getFactory();
            if (f != null && f.canCreateStateSystem(trace)) {
                returnList.add(f);
            }
        }
        return returnList;
    }

    /**
     * get a particular factory by its unique id.
     * @param factoryId string unique id for the factory
     * @return the factory that matches the given id or null if not found
     */
    public IStateSystemFactory getFactoryById( final String factoryId ) {

        loadExtensions();

        FactoryContainer factoryContainer = idToFactoryMap.get(factoryId);
        if (factoryContainer != null) {
            return factoryContainer.getFactory();
        }
        return null;
    }

    /**
     * private constructor, this is a singleton
     */
    private StateSystemFactoryManager() {
    }

    /**
     * iterates all config elements that implement the given extension point cataloging them
     * Note that actual creation of the extension interface is done on demand
     */
    private synchronized void loadExtensions() {

        if (idToFactoryMap != null) {
            //already loaded
            return;
        }

        IConfigurationElement[] configElems = Platform.getExtensionRegistry().getConfigurationElementsFor(STATE_SYSTEM_FACTORY_EXTENSION);

        idToFactoryMap = new LinkedHashMap<String,FactoryContainer>(configElems.length);

        for (IConfigurationElement configCE : configElems) {
            if (configCE.getName().equals(STATE_SYSTEM_TYPE_CHILD)) {
                String id = configCE.getAttribute(STATE_SYSTEM_ID_ATTRIBUTE);
                idToFactoryMap.put(id, new FactoryContainer(configCE));
            }
        }
    }
}
