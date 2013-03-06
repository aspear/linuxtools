package org.eclipse.linuxtools.tmf.core.statesystem;

import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.statevalue.ITmfStateValue;

/**
 * Encapsulation of optional presentation for states
 * @author Aaron Spear
 * @since 2.0
 *
 */
public interface IStateSystemPresentationInfo {

    // TODO information about the columns/data displayed in the tree?

    /**
     * Get the hierarchy used in this state system. The return value is an array where the
     * 0 item is the root, and increasing items are deeper nodes in the tree.
     * @return non null static context hierarchy array
     */
    public IStateSystemContextHierarchyInfo[] getContextHierarchy();


    /**
     * get an array that contains all states for the system.  Note that the first state MUST be the special "Unknown"
     * state which results in nothing being plotted.
     * @return
     */
    public  IStatePresentationInfo[] getAllStates();

    /**
     * Get the presentation information that should be used for the given state value.  Note that
     * A valid presentation should be returned for TmfStateValue.nullValue() (a presentation that
     * is assumed to result in NOTHING being printed, i.e. an unknown state
     * @param stateValue the value to get the presentation for
     * @return the presentation value
     */
    public IStatePresentationInfo getPresentationForStateValue( ITmfStateValue stateValue );

    /** If and only if the given event contains context changes a string array is returned
     * that contains the String context ids.  If the return value is non null, then the size
     * of the array and ordering will always be the same as that returned by
     * {@link #getContextHierarchy()}.  Note though that this does not mean that the all
     * context values will be non null.  It is supported to have a subset of the context
     * values returned be non null, say for example that only the last context is changed.
     * The implication is that the current context has changed in only the last value, but
     * the others are still their previous value.
     * @param eventTypeName the name of the type of the event.  This is passed in as an
     * efficiently convenience.
     * @param event the event itself
     * @return null if no context change, otherwise an array of the same size as the context
     * hierarchy with a string value for each new context.
     */
    public String[] getContext( String eventTypeName, ITmfEvent event );

    /**
     * get a new state for the given event if there is one.  If there is not, returns null.
     * @param eventTypeName the name of the type of the event.  This is passed in as an
     * efficiently convenience.
     * @param event the event instance itself
     * @return a value presentation for the new state for the event, null if none for this event.
     */
    public IStatePresentationInfo getNewState( String eventTypeName, ITmfEvent event );

}
