/*******************************************************************************
 * Copyright (c) 2013 VMware Inc.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Aaron Spear - Initial implementation
 *
 *******************************************************************************/

package org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider;

import java.util.LinkedHashMap;
import java.util.Stack;

import org.eclipse.linuxtools.internal.tmf.stateflow.core.Attributes;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventType;
import org.eclipse.linuxtools.tmf.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.tmf.core.statesystem.AbstractStateChangeInput;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateChangeInput;
import org.eclipse.linuxtools.tmf.core.statesystem.IStatePresentationInfo;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemContextHierarchyInfo;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemPresentationInfo;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemPresentationInfo.ContextChangeInfo;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystemBuilder;
import org.eclipse.linuxtools.tmf.core.statevalue.ITmfStateValue;
import org.eclipse.linuxtools.tmf.core.statevalue.TmfStateValue;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

/**
 * This state change input plugin takes a set of data structures
 *
 * @author Aaron Spear
 *
 */
public class DataDrivenStateInput extends AbstractStateChangeInput {

	private static final int CURRENT_VERSION_NUMBER = 1;

	public final static String STATE_SYSTEM_ID = "org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider.datadriven";

	/**
	 * copy from the source to the destination any values that are not null.
	 *
	 * @param source
	 * @param dest
	 * @param startingLevel
	 */
	public static void copyContext(final String[] source, final String[] dest, int startingLevel) {
		for (int i = startingLevel; i < source.length; ++i) {
			if (source[i] != null) {
				dest[i] = source[i];
			}
		}
	}

	/**
	 * compare id's
	 *
	 * @param id1
	 * @param id2
	 * @return
	 */
	public static boolean idsAreEqual(final String[] id1, final String[] id2) {
		if (id1.length != id2.length) {
			return false;
		}
		for (int i = 0; i < id1.length; ++i) {
			if (id1[i] != null && id2[i] != null) {
				if (!id1[i].equals(id2[i])) {
					return false;
				}
			} else {
				return false; // one or the other is null
			}
		}
		return true;
	}

	public static class ContextState implements Cloneable {
		private final String[] id;
		private ITmfStateValue stateValue;

		public ContextState(final String[] id, final ITmfStateValue initialStateValue) {
			this.id = id;
			this.stateValue = initialStateValue;
		}

		public String[] getId() {
			return id;
		}

		public ITmfStateValue getStateValue() {
			return stateValue;
		}

		public void setStateValue(ITmfStateValue stateValue) {
			this.stateValue = stateValue;
		}

		boolean idsAreEqual(final ContextState other) {
			if (this == other) {
				return true;
			}
			if (other == null) {
				return false;
			}
			return DataDrivenStateInput.idsAreEqual(this.id, other.id);
		}

		public void copy(ContextState other) {
			// copy the others id values into our array
			copyContext(other.id, id, 0);
			// replace our state with the others state
			stateValue = other.stateValue;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("id=[");
			for (int i = 0; i < id.length; ++i) {
				builder.append(id[i]).append("/");
			}
			builder.append("]");
			if (stateValue == null) {
				builder.append(" state=null");
			} else {
				builder.append(" state=").append(stateValue.toString());
			}
			return builder.toString();
		}

		@Override
		public ContextState clone() {
			return new ContextState(id.clone(), stateValue);
		}
	}

	private static class ContextStack {

		private final ContextState currentState;
		private final int contextLevel;

		private LinkedHashMap<String, ContextStack> contextToChildStackMap = null;
		private Stack<ContextState> stack = null;

		public ContextStack(ContextState initialState, int contextLevel) {
			this.currentState = initialState;
			this.contextLevel = contextLevel;
		}

		public String[] getId() {
			return currentState.getId();
		}

		public ContextState getState() {
			return currentState;
		}

		// public int getContextLevel() {
		// return contextLevel;
		// }

		public synchronized ContextStack getChildContextStack(final String childContext, boolean createIfNotExisting) {

			if (contextToChildStackMap == null && createIfNotExisting) {
				contextToChildStackMap = new LinkedHashMap<String, ContextStack>();
			}

			if (contextToChildStackMap != null) {
				ContextStack child = contextToChildStackMap.get(childContext);
				if (child == null && createIfNotExisting) {

					// clone/generate a valid id for this new child
					String[] newChildId = currentState.getId().clone();
					newChildId[contextLevel + 1] = childContext;

					ContextState newChildState = new ContextState(newChildId, currentState.getStateValue());
					child = new ContextStack(newChildState, contextLevel + 1);
					contextToChildStackMap.put(childContext, child);
				}
				return child;
			} else {
				return null;
			}
		}

		/**
		 * Makes a clone of the current context id/state and pushes it on the
		 * stack. Then changes the id of the current context to newContextId.
		 * Note that the assumption is that the state may be updated
		 * subsequently
		 *
		 * @param newContextIds
		 *            the updated/new context id.
		 * @return the complete id/state that was current before this call
		 */
		public synchronized ContextState push(String[] newContextId) {

			ContextState previousState = currentState.clone();

			// must push a clone since the id value is about to change. strictly
			// speaking only the last level should be a changing, so as a future
			// optimization we could only clone that string
			getStack().push(previousState);

			// now change to the new state. Note that we do need to update the
			// id
			// in the new state to pick up any leading values from the current
			// context
			copyContext(newContextId, currentState.getId(), contextLevel);

			return previousState;
		}

		/**
		 * Update the current state to be the value on the top of the stack.
		 *
		 * @return the previous current value (the value before the pop)
		 */

		public synchronized ContextState pop() {
			ContextState previousState = currentState.clone();

			Stack<ContextState> stack = getStack();
			if (!stack.isEmpty()) {
				ContextState newState = stack.pop();
				currentState.copy(newState);

			} else {
				// this should not happen
				System.err.println("DataDrivenStateInput: Error pop on empty stack for " + toString());
			}
			return previousState;
		}

		/**
		 * access to the stack for this instance in the hierarchy. Note that
		 * this method does lazy creation of the stack object
		 *
		 * @return
		 */
		public synchronized Stack<ContextState> getStack() {
			if (stack == null) {
				stack = new Stack<ContextState>();
			}
			return stack;
		}

		public void dispose() {
			if (contextToChildStackMap != null) {
				for (ContextStack child : contextToChildStackMap.values()) {
					child.dispose();
				}
				contextToChildStackMap.clear();
			}
			if (stack != null) {
				stack.clear();
			}
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("id=[");
			String[] id = currentState.getId();
			for (int i = 0; i < id.length; ++i) {
				builder.append(id[i]).append("/");
			}
			builder.append("]");
			builder.append(" level=").append(contextLevel);
			builder.append(" stack=");
			if (stack != null) {
				for (int s = 0; s < stack.size(); ++s) {
					builder.append(stack.get(s).toString()).append(">");
				}
			} else {
				builder.append("null");
			}

			return builder.toString();
		}
	}

	// ************************************************************************

	private int currentContextQuark = -1;
	private ContextStack currentContextStack = null;
	private int rootQuark = -1;
	private final ContextStack rootContextStack;

	// ************************************************************************

	public DataDrivenStateInput(IStateSystemPresentationInfo statePresentationInfo, ITmfTrace trace,
			Class<? extends ITmfEvent> eventType, String id) {
		super(trace, eventType, id);

		this.statePresentationInfo = statePresentationInfo;

		// the last contextids are sized for the number of contexts we have in
		// the hierarchy
		// currentContextIds = new
		// String[statePresentationInfo.getContextHierarchy().length];

		// FIXME I am not sure what to initialize the context to. maybe this
		// should be trace specific somehow?
		// either that or there should be events in the event stream that set
		// values like the PID and all of that.
		IStateSystemContextHierarchyInfo[] hierarchyInfo = statePresentationInfo.getContextHierarchy();
		String[] rootContextId = new String[hierarchyInfo.length];
		for (int i = 0; i < hierarchyInfo.length; ++i) {
			rootContextId[i] = hierarchyInfo[i].getContextId();
		}

		ContextState rootState = new ContextState(rootContextId, TmfStateValue.nullValue());
		rootContextStack = new ContextStack(rootState, -1);

		currentContextStack = rootContextStack;
	}

	@Override
	public void assignTargetStateSystem(ITmfStateSystemBuilder ssb) {
		/* We can only set up the locations once the state system is assigned */
		super.assignTargetStateSystem(ssb);

		initializeStateSystem(ssb);
	}

	private void initializeStateSystem(ITmfStateSystemBuilder ssb) {
		// add the root level context nodes to the tree
		IStateSystemContextHierarchyInfo[] hierarchy = statePresentationInfo.getContextHierarchy();
		rootQuark = ssb.getQuarkAbsoluteAndAdd(hierarchy[0].getContextId());
	}

	/** fill in any missing context ids using the lastContextIds value. */
	private void completeNewContext(final String[] contextIds) {
		if (currentContextStack != null) {
			boolean gotValidOneInContextIds = false;
			final String[] currentContextIds = currentContextStack.getId();
			for (int i = 0; i < contextIds.length; ++i) {
				if (contextIds[i] == null) {
					if (gotValidOneInContextIds) {
						// you already got one valid value, and now you have a
						// null. That means that this is the level to
						// stop at.
						break;
					}
					contextIds[i] = currentContextIds[i];
				} else {
					gotValidOneInContextIds = true;
				}
			}
		}
	}

	private ContextStack getContextStack(final String[] contextIds) {
		ContextStack returnValue = rootContextStack.getChildContextStack(contextIds[0], true);
		int length = contextIds.length;
		for (int i = 1; i < length; ++i) {
			if (contextIds[i] != null) {
				ContextStack child = returnValue.getChildContextStack(contextIds[i], true);
				returnValue = child;
			} else {
				break;
			}
		}
		return returnValue;
	}

	@Override
	protected void eventHandle(ITmfEvent event) {
		/*
		 * AbstractStateChangeInput should have already checked for the correct
		 * class type
		 */
		if (statePresentationInfo == null) {
			return;
		}

		// get the event
		ITmfEventType eventType = event.getType();
		if (eventType == null) {
			return;
		}
		String eventTypeName = eventType.getName();

		try {
			// NOTE: the ITmfTimestamp values that come on the events could be
			// in any scale. We use nanosecond
			// scale in the state system, so normalize the timestamp from the
			// event before addding the new state value
			final long ts = event.getTimestamp().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();

			// if there is a context that we need to close the state on, this id
			// will contain it
			ContextState contextStateToClose = null;
			ContextState contextStateToRestore = null;
			IStatePresentationInfo previousContextUpdatedState = null;

			// check to see if there is a context switch in this event
			final ContextChangeInfo switchedContextInfo = statePresentationInfo.getSwitchContext(eventTypeName, event);
			if (switchedContextInfo != null) {

				previousContextUpdatedState = switchedContextInfo.getPreviousContextStateValue();

				String[] ids = switchedContextInfo.getId();

				// there is, some of the values may be null, which means to get
				// the values for that from
				// the current context
				completeNewContext(ids);

				// switch the lastContextStack to be the value indicated by this
				// context.
				currentContextStack = getContextStack(ids);

				String[] newId = currentContextStack.getId();

				currentContextQuark = ss.getQuarkRelativeAndAdd(rootQuark, newId);
			}

			// the context may or may not have been switched by the code above.
			// Lets check to see if there
			// are any context pushes in this event. That means to take the
			final ContextChangeInfo pushedContextInfo = statePresentationInfo.getPushContext(eventTypeName, event);
			if (pushedContextInfo != null) {
				previousContextUpdatedState = pushedContextInfo.getPreviousContextStateValue();
				contextStateToClose = currentContextStack.push(pushedContextInfo.getId());
				currentContextQuark = ss.getQuarkRelativeAndAdd(rootQuark, currentContextStack.getId());
			}

			final ContextChangeInfo poppedContextInfo = statePresentationInfo.getPopContext(eventTypeName, event);
			if (poppedContextInfo != null) {
				previousContextUpdatedState = poppedContextInfo.getPreviousContextStateValue();
				// call pop to update the current context value to the previous
				// value on the stack,
				// and then return the previously active context, which we close
				contextStateToClose = currentContextStack.pop();

				// here we are not going to do a state change, but rather we are
				// going to restore the previous context
				contextStateToRestore = currentContextStack.getState();

				// currentContextQuark = ss.getQuarkRelativeAndAdd(rootQuark,
				// currentContextStack.getId());
			}

			// TODO the state below could possibly be for multiple or a single
			// level in the hierarchy
			// we need to figure out an efficient way to determine what could
			// possibly be multiple different
			// states for different levels from the same event. for now we
			// assume it is always for the
			// last level
			if (currentContextQuark != -1) {
				// check to see if there is new state that we can extract for
				// this event
				IStatePresentationInfo newState = statePresentationInfo.getNewState(eventTypeName, event);
				if (newState != null) {

					ITmfStateValue newStateValue = newState.getStateValue();

					// must update the current context with the new state value
					currentContextStack.getState().setStateValue(newStateValue);

					// get/add the status quark for the current context
					int statusQuark = ss.getQuarkRelativeAndAdd(currentContextQuark, Attributes.STATE);
					ss.modifyAttribute(ts, newStateValue, statusQuark);
				}
			}

			if ((contextStateToClose != null) && !contextStateToClose.idsAreEqual(currentContextStack.getState())) {
				// the previous context is not the same as the new context. That
				// means that we need to close the
				// previous context
				int contextToCloseQuark = ss.getQuarkRelative(rootQuark, contextStateToClose.getId());
				try {
					int contextToCloseStatusQuark = ss.getQuarkRelative(contextToCloseQuark, Attributes.STATE);

					ITmfStateValue stateValue;
					if (previousContextUpdatedState != null) {
						stateValue = previousContextUpdatedState.getStateValue();
					} else {
						stateValue = TmfStateValue.nullValue();
					}

					ss.modifyAttribute(ts,stateValue, contextToCloseStatusQuark);
				} catch (AttributeNotFoundException e) {
				}
			}
			if (contextStateToRestore != null) {
				int contextToRestoreQuark = ss.getQuarkRelative(rootQuark, contextStateToRestore.getId());
				try {
					int contextToRestoreStatusQuark = ss.getQuarkRelative(contextToRestoreQuark, Attributes.STATE);
					ss.modifyAttribute(ts, contextStateToRestore.getStateValue(), contextToRestoreStatusQuark);
				} catch (AttributeNotFoundException e) {
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public int getVersion() {
		return CURRENT_VERSION_NUMBER;
	}

	@Override
	public IStateChangeInput getNewInstance() {
		// Aaron FIXME note:
		// I do not quite understand the newly introduced "partial history" functionality and what the implications are for a data
		// driven state system.  In effect, I am not sure what it means yet for this class instance to clone itself.  It is
		// probably OK since StateSystemPresentationInfo is immutable, but I am not honestly sure
		return new DataDrivenStateInput(statePresentationInfo,getTrace(), getTrace().getEventType(),
				DataDrivenStateInput.STATE_SYSTEM_ID);
	}
}
