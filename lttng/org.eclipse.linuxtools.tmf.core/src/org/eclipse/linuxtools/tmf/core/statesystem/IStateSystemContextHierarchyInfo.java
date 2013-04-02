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

/**
 * class used for static description of the context heirarchy used in the state system if available. There may be any number of
 * these at the root level, and then each at the root level may have any number of child contexts.  ID's and names used for these
 * must be unique.
 * @author aspear
 * @since 2.0
 *
 */
public interface IStateSystemContextHierarchyInfo {

    /**
     * @return the string id that corresponds to this context
     */
    public String getContextId();

    /**
     * the id of the parent context.  If this is null, there is no parent and it is the root context (there
     * should only be one for a given trace)
     * @return may be null only for the root node, otherwise non null parent node id
     */
    public String getParentId();

    /**
     * @return true if this level in the hierarchy has state, otherwise false.
     */
    public boolean hasState();
}
