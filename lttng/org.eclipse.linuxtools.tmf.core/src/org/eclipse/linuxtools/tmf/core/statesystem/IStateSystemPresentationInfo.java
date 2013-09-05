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

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.statevalue.ITmfStateValue;

/**
 * Encapsulation of optional presentation for states
 *
 * @author Aaron Spear
 * @since 2.1
 *
 */
public interface IStateSystemPresentationInfo  {

    // TODO information about the columns/data displayed in the tree?
    public static class ContextChangeInfo {
        private final String[] id;
        public String[] getId() {
            return id;
        }
        private final IStatePresentationInfo previousContextStateValue;
        public ContextChangeInfo(final String[] id, final IStatePresentationInfo previousContextStateValue) {
            this.id = id;
            this.previousContextStateValue = previousContextStateValue;
        }
        public IStatePresentationInfo getPreviousContextStateValue() {
            return previousContextStateValue;
        }
    }

    /**
     * get an array that contains all states for the system. Note that the first
     * state MUST be the special "Unknown" state which results in nothing being
     * plotted.
     *
     * @return
     */
    public IStatePresentationInfo[] getAllStates();

    /**
     * Get the presentation information that should be used for the given state
     * value. Note that A valid presentation should be returned for
     * TmfStateValue.nullValue() (a presentation that is assumed to result in
     * NOTHING being printed, i.e. an unknown state
     *
     * @param stateValue
     *            the value to get the presentation for
     * @return the presentation value
     */
    public IStatePresentationInfo getPresentationForStateValue(ITmfStateValue stateValue);

    /**
     * Get the hierarchy used in this state system. The return value is an array
     * where the 0 item is the root, and increasing items are deeper nodes in
     * the tree. The values are static for this instance. The values of the
     * states and such are all of the same dimension and ordering.
     *
     * @return non null static context hierarchy array
     */
    public IStateSystemContextHierarchyInfo[] getContextHierarchy();

    /**
     * If the event contains a "context switch", then switch to the current
     * context tracked by the given value returned. If the return value is null,
     * then there is no context switch for this event. Otherwise, the string
     * array returned is always the same dimensions as the context hierarchy,
     * but may contain null values for portions. If this is the case, then it is
     * assumed that the portions that are not null in the returned value replace
     * the portions of the current active context. Each complete context value
     * contains a stack of contexts as well, and this event switches to a
     * different stack, like an operating system context switch.
     *
     * Note though that this does not mean that the all context values will be
     * non null. It is supported to have a subset of the context values returned
     * be non null, say for example that only the last context is changed. The
     * implication is that the current context has changed in only the last
     * value, but the others are still their previous value.
     *
     * @param eventTypeName
     *            the name of the type of the event. This is passed in as an
     *            efficiently convenience.
     * @param event
     *            the event itself
     * @return null if no context change, otherwise an array of the same size as
     *         the context hierarchy with a string value for each new context.
     */
    public ContextChangeInfo getSwitchContext(String eventTypeName, ITmfEvent event);

    /**
     * If this routine returns non null, then the String array returned, which
     * is the same dimensions as the context hierarchy, contains a non null
     * string for the context level(s) that should be pushed on the stack as
     * well as the new value(s)
     *
     * @param eventTypeName
     *            the name of the type of the event. This is passed in as an
     *            efficiently convenience.
     * @param event
     *            the event itself
     * @return null if there is no push for this event, otherwise an array of
     *         the same size as the context hierarchy with a value for the
     */
    public ContextChangeInfo getPushContext(String eventTypeName, ITmfEvent event);

    /**
     * If this routine returns non null, then the String array returned, which
     * is the same dimensions as the context hierarchy, contains a non null
     * string value for
     *
     * @param eventTypeName
     * @param event
     * @return
     */
    public ContextChangeInfo getPopContext(String eventTypeName, ITmfEvent event);

    /**
     * If the given event contains a state change for the currently active
     * context, then this routine returns info for the new state, otherwise it
     * returns null.
     *
     * @param eventTypeName
     *            the name of the type of the event. This is passed in as an
     *            efficiently convenience.
     * @param event
     *            the event instance itself
     * @return a value presentation for the new state for the event, null if
     *         none for this event.
     */
    public IStatePresentationInfo getNewState(String eventTypeName, ITmfEvent event);

}
