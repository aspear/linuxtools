package org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider;

import java.util.List;

import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemContextHierarchyInfo;

/**
 * class used for static description of the context heirarchy used in the state system if available. There may be any number of
 * these at the root level, and then each at the root level may have any number of child contexts.  ID's and names used for these
 * must be unique.
 * @author aspear
 * @since 2.0
 *
 */
public class StateSystemContextHierarchyInfo implements IStateSystemContextHierarchyInfo {
    private final String contextId;
    private final String parentId;
    private final boolean hasState;
   
    /**
     * construct an instance of the context hierarchy info
     * @param contextIdInStateSystem the id that is used in the state system itself for this level in the hierarchy
     * @param parentId 
     * @param stateAttributeName the attribute that should be used to query for state for this type of context.  Note that if
     * this is null, that means that the this level in the hierarchy does not expose any state
     */
    public StateSystemContextHierarchyInfo(String contextId, String parentId,boolean hasState) {
        this.contextId = contextId;
        this.parentId = parentId;
        this.hasState = hasState;
    }

	@Override
	public String getContextId() {
		return contextId;
	}

	@Override
	public String getParentId() {
		return parentId;
	}

	@Override
	public boolean hasState() {
		return hasState;
	}
}
