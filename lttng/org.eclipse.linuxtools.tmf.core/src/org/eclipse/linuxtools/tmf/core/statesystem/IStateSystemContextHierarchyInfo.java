package org.eclipse.linuxtools.tmf.core.statesystem;

import java.util.List;

/**
 * class used for static description of the context heirarchy used in the state system if available. There may be any number of
 * these at the root level, and then each at the root level may have any number of child contexts.  ID's and names used for these
 * must be unique.
 * @author aspear
 * @since 2.0
 *
 */
public interface IStateSystemContextHierarchyInfo {

    public String getContextIdInStateSystem();
    public String getContextDisplayName();
    public String getStateSystemAttributeName();
    List<IStateSystemContextHierarchyInfo>  getChildContexts();
}
