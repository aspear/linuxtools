package org.eclipse.linuxtools.tmf.core.statesystem;

import org.eclipse.linuxtools.tmf.core.statevalue.ITmfStateValue;

/**
 * Encapsulation of optional presentation for states
 * @author Aaron Spear
 *
 */
public interface IStateSystemPresentationInfo {

    // TODO Information about the hierarchy?


    /**
     * the name of the attribute in the state system used for state
     * @return
     */
    public String getStateAttributeName();

    /**
     * Get the presentation information that should be used for the given state value.
     * @param stateValue
     * @return
     */
    public IStatePresentationInfo getPresentationForStateValue( ITmfStateValue stateValue );

}
