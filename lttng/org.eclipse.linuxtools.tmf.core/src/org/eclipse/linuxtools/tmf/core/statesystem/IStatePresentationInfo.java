package org.eclipse.linuxtools.tmf.core.statesystem;

import org.eclipse.swt.graphics.RGB;

/**
 * optional interface that describes presentation information for one particular state
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
}
