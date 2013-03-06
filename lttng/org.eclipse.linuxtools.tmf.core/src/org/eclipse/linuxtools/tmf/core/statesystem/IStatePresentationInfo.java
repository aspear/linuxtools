package org.eclipse.linuxtools.tmf.core.statesystem;

import org.eclipse.linuxtools.tmf.core.statevalue.ITmfStateValue;
import org.eclipse.swt.graphics.RGB;

/**
 * optional interface that describes presentation information for one particular state
 *
 * NOTE: this interface is nearly the functionality of the StateItem class, which is in the GUI plugin.
 * perhaps that concrete class could be moved here and used instead?  (yes, coupling presentation with
 * implementation is sticky...)
 *
 * @author Aaron Spear
 * @since 2.0
 */
public interface IStatePresentationInfo {

    /**
     * A presentation string for the state
     * @return
     */
    public String getStateString();

    /**
     * A preferred color for the state.  May or may not be applicable depending on the presentation
     * @return
     */
    public RGB getStateColor();

    /**
     * The value that is inserted into the state system for this particular state
     * @return
     */
    public ITmfStateValue getStateValue();
}
