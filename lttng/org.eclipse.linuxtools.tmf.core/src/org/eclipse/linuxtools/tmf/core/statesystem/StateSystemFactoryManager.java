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
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

public class StateSystemFactoryManager {

    private static final String STATE_SYSTEM_FACTORY_EXTENSION = "org.eclipse.linuxtools.tmf.ui.statesystemfactory";
    private static final String STATE_SYSTEM_TYPE_CHILD = "type";
    private static final String STATE_SYSTEM_CLASS_ATTRIBUTE = "class";
    private static final String STATE_SYSTEM_ID_ATTRIBUTE = "id";

    private List<IStateSystemFactory>  allStateSystemFactories = null;

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

        List<IStateSystemFactory> returnList = new ArrayList<IStateSystemFactory>(allStateSystemFactories.size());
        for (IStateSystemFactory f : allStateSystemFactories) {
            if (f.canCreateStateSystem(trace)) {
                returnList.add(f);
            }
        }
        return returnList;
    }

    /**
     * get a particular factory by its unique id
     * @param factoryId string unique id for the factory
     * @return the factory that matches the given id or null if not found
     */
    public IStateSystemFactory getFactoryById( final String factoryId ) {

        for (IStateSystemFactory f : allStateSystemFactories) {
            if (f.getStateSystemId().equals(factoryId)) {
                return f;
            }
        }
        return null;
    }

    /**
     * private constructor, this is a singleton
     */
    private StateSystemFactoryManager() {
        loadFactoriesFromExtensionPoint();
    }

    private void loadFactoriesFromExtensionPoint() {
        allStateSystemFactories = new ArrayList<IStateSystemFactory>();
        IConfigurationElement[] configElems = Platform.getExtensionRegistry().getConfigurationElementsFor(STATE_SYSTEM_FACTORY_EXTENSION);
        for (IConfigurationElement configCE : configElems) {
            if (configCE.getName().equals(STATE_SYSTEM_TYPE_CHILD)) {
                //String id = configCE.getAttribute(STATE_SYSTEM_ID_ATTRIBUTE);
                //String klass = configCE.getAttribute(STATE_SYSTEM_CLASS_ATTRIBUTE);
                try {
                    //instantiate the factory class
                    allStateSystemFactories.add((IStateSystemFactory)configCE.createExecutableExtension(STATE_SYSTEM_CLASS_ATTRIBUTE));
                }catch(CoreException e) {
                    e.printStackTrace(); //TODO better logging
                }
            }
        }
    }
}
