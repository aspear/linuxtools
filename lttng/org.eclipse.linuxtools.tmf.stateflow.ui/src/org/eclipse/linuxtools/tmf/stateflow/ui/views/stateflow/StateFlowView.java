/*******************************************************************************
 * Copyright (c) 2012 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.stateflow.ui.views.stateflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.linuxtools.internal.tmf.stateflow.core.Attributes;
import org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider.XmlDataDrivenStateInputFactory;
import org.eclipse.linuxtools.internal.tmf.stateflow.ui.Messages;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTimestamp;
import org.eclipse.linuxtools.tmf.core.event.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.event.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.event.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.tmf.core.exceptions.StateSystemDisposedException;
import org.eclipse.linuxtools.tmf.core.exceptions.StateValueTypeException;
import org.eclipse.linuxtools.tmf.core.exceptions.TimeRangeException;
import org.eclipse.linuxtools.tmf.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.tmf.core.signal.TmfRangeSynchSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalHandler;
import org.eclipse.linuxtools.tmf.core.signal.TmfTimeSynchSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemContextHierarchyInfo;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemPresentationInfo;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystem;
import org.eclipse.linuxtools.tmf.core.statevalue.ITmfStateValue;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.linuxtools.tmf.stateflow.core.trace.CtfExecutionTrace;
import org.eclipse.linuxtools.tmf.stateflow.util.StringUtils;
import org.eclipse.linuxtools.tmf.ui.editors.ITmfTraceEditor;
import org.eclipse.linuxtools.tmf.ui.views.TmfView;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.ITimeGraphRangeListener;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.ITimeGraphSelectionListener;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.ITimeGraphTimeListener;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.TimeGraphCombo;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.TimeGraphRangeUpdateEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.TimeGraphSelectionEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.TimeGraphTimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;

/**
 * The Control Flow view main object
 * 
 */
public class StateFlowView extends TmfView {

	/**
	 * little utility class that has a start and end time and convenience methods
	 * for extending the beginning and end with other intervals
	 * @author Aaron Spear
	 */
	private static class TimeSpan {
		long startTime;
		long endTime;

		public TimeSpan(long startTime, long endTime) {
			this.startTime = startTime;
			this.endTime = endTime;
		}

		public long getStartTime() {
			return startTime;
		}

		public long getEndTime() {
			return endTime;
		}

		public boolean isValid() {
			return startTime < endTime;
		}

		/**
		 * if the other TimeSpan extends past ours on either side (beginning or
		 * end) we extend our time to cover the other
		 * 
		 * @param other
		 *            ignored if null. Otherwise our start and end are extended
		 *            if the other goes past them
		 */
		public void add(TimeSpan other) {
			if (other == null) {
				return;
			}
			if (other.startTime < startTime) {
				startTime = other.startTime;
			}
			if (other.endTime > endTime) {
				endTime = other.endTime;
			}			
		}
	}

	// ------------------------------------------------------------------------
	// Constants
	// ------------------------------------------------------------------------

	/**
	 * View ID.
	 */
	public static final String ID = "org.eclipse.linuxtools.tmf.stateflow.ui.views.stateflow"; //$NON-NLS-1$

	private static final String CONTEXT_COLUMN = Messages.StateFlowView_contextColumn;
	//private static final String NAME_COLUMN = Messages.StateFlowView_nameColumn;
	//private static final String SCOPE_COLUMN = Messages.StateFlowView_scopeColumn;
	//private static final String BIRTH_TIME_COLUMN = Messages.StateFlowView_birthTimeColumn;
	private static final String TRACE_COLUMN = Messages.StateFlowView_traceColumn;

	private final static String[] COLUMN_NAMES = new String[] { CONTEXT_COLUMN, TRACE_COLUMN };
	
	private final static int CONTEXT_COLUMN_INDEX = 0;
	private final static int TRACE_COLUMN_INDEX = 1;

	/**
	 * Redraw state enum
	 */
	private enum State {
		IDLE, BUSY, PENDING
	}

	// ------------------------------------------------------------------------
	// Fields
	// ------------------------------------------------------------------------

	// The timegraph combo
	private TimeGraphCombo fTimeGraphCombo;

	private StateFlowPresentationProvider fStateFlowPresentationProvider;

	// The selected trace
	private ITmfTrace fTrace;

	// The timegraph entry list
	private ArrayList<StateFlowEntry> fEntryList;

	// The trace to entry list hash map
	final private HashMap<ITmfTrace, ArrayList<StateFlowEntry>> fEntryListMap = new HashMap<ITmfTrace, ArrayList<StateFlowEntry>>();

	// The trace to build thread hash map
	final private HashMap<ITmfTrace, BuildThread> fBuildThreadMap = new HashMap<ITmfTrace, BuildThread>();

	// The start time
	private long fStartTime;

	// The end time
	private long fEndTime;

	// The display width
	private final int fDisplayWidth;

	// The zoom thread
	private ZoomThread fZoomThread;

	// The next resource action
	private Action fNextResourceAction;

	// The previous resource action
	private Action fPreviousResourceAction;

	// A comparator class
	private final StateFlowEntryComparator fExecutionFlowEntryComparator = new StateFlowEntryComparator();

	// The redraw state used to prevent unnecessary queuing of display runnables
	private State fRedrawState = State.IDLE;

	// The redraw synchronization object
	final private Object fSyncObj = new Object();

	// ------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------

	private class TreeContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return (ITimeGraphEntry[]) inputElement;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			ITimeGraphEntry entry = (ITimeGraphEntry) parentElement;
			List<? extends ITimeGraphEntry> children = entry.getChildren();
			return children.toArray(new ITimeGraphEntry[children.size()]);
		}

		@Override
		public Object getParent(Object element) {
			ITimeGraphEntry entry = (ITimeGraphEntry) element;
			return entry.getParent();
		}

		@Override
		public boolean hasChildren(Object element) {
			ITimeGraphEntry entry = (ITimeGraphEntry) element;
			return entry.hasChildren();
		}

	}

	private class TreeLabelProvider implements ITableLabelProvider {

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			StateFlowEntry entry = (StateFlowEntry) element;
			if (columnIndex == CONTEXT_COLUMN_INDEX) {
				return entry.getName();
			}else if (columnIndex == TRACE_COLUMN_INDEX) {
				return entry.getTrace().getName();
			}
			/*
			else if (columnIndex == 1) {
				return entry.getContextInfo();
			} else if (columnIndex == 2) {
				return Integer.toHexString(entry.getParentQuark());
			} else if (columnIndex == 3) {
				return Utils.formatTime(entry.getBirthTime(), TimeFormat.CALENDAR, Resolution.NANOSEC);
			} else if (columnIndex == 4) {
				return entry.getTrace().getName();
			}
			*/
			return ""; //$NON-NLS-1$
		}

	}

	private static class StateFlowEntryComparator implements Comparator<ITimeGraphEntry> {

		@Override
		public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
			int result = 0;

			if ((o1 instanceof StateFlowEntry) && (o2 instanceof StateFlowEntry)) {
				StateFlowEntry entry1 = (StateFlowEntry) o1;
				StateFlowEntry entry2 = (StateFlowEntry) o2;
				result = entry1.getTrace().getStartTime().compareTo(entry2.getTrace().getStartTime());
				if (result == 0) {
					result = entry1.getTrace().getName().compareTo(entry2.getTrace().getName());
				}
				if (result == 0) {
					result = (entry1.getNodeQuark() < entry2.getNodeQuark()) ? -1 : entry1.getNodeQuark() > entry2
							.getNodeQuark() ? 1 : 0;
				}
			}

			if (result == 0) {
				result = o1.getStartTime() < o2.getStartTime() ? -1 : o1.getStartTime() > o2.getStartTime() ? 1 : 0;
			}

			return result;
		}
	}

	private class BuildThread extends Thread {
		private final ITmfTrace fBuildTrace;
		private final IProgressMonitor fMonitor;

		public BuildThread(ITmfTrace trace) {
			super("StateFlowView build"); //$NON-NLS-1$
			fBuildTrace = trace;
			fMonitor = new NullProgressMonitor();
		}

		@Override
		public void run() {
			buildEventList(fBuildTrace, fMonitor);
			synchronized (fBuildThreadMap) {
				fBuildThreadMap.remove(this);
			}
		}

		public void cancel() {
			fMonitor.setCanceled(true);
		}
	}

	private class ZoomThread extends Thread {
		private final ArrayList<StateFlowEntry> fZoomEntryList;
		private final long fZoomStartTime;
		private final long fZoomEndTime;
		private final long fResolution;
		private final IProgressMonitor fMonitor;

		public ZoomThread(ArrayList<StateFlowEntry> entryList, long startTime, long endTime) {
			super("StateFlowView zoom"); //$NON-NLS-1$
			fZoomEntryList = entryList;
			fZoomStartTime = startTime;
			fZoomEndTime = endTime;
			fResolution = Math.max(1, (fZoomEndTime - fZoomStartTime) / fDisplayWidth);
			fMonitor = new NullProgressMonitor();
		}

		@Override
		public void run() {
			if (fZoomEntryList == null) {
				return;
			}
			for (StateFlowEntry entry : fZoomEntryList) {
				if (fMonitor.isCanceled()) {
					break;
				}
				zoom(entry, fMonitor);
			}
		}

		private void zoom(StateFlowEntry entry, IProgressMonitor monitor) {
			if (fZoomStartTime <= fStartTime && fZoomEndTime >= fEndTime) {
				entry.setZoomedEventList(null);
			} else {
				List<ITimeEvent> zoomedEventList = getEventList(entry, fZoomStartTime, fZoomEndTime, fResolution,
						monitor);
				if (zoomedEventList != null) {
					entry.setZoomedEventList(zoomedEventList);
				}
			}
			redraw();
			for (StateFlowEntry child : entry.getChildren()) {
				if (fMonitor.isCanceled()) {
					return;
				}
				zoom(child, monitor);
			}
		}

		public void cancel() {
			fMonitor.setCanceled(true);
		}
	}

	// ------------------------------------------------------------------------
	// Constructors
	// ------------------------------------------------------------------------

	/**
	 * Constructor
	 */
	public StateFlowView() {
		super(ID);
		fDisplayWidth = Display.getDefault().getBounds().width;
	}

	// ------------------------------------------------------------------------
	// ViewPart
	// ------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.linuxtools.tmf.ui.views.TmfView#createPartControl(org.eclipse
	 * .swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		fTimeGraphCombo = new TimeGraphCombo(parent, SWT.NONE);

		fTimeGraphCombo.setTreeContentProvider(new TreeContentProvider());

		fTimeGraphCombo.setTreeLabelProvider(new TreeLabelProvider());

		fStateFlowPresentationProvider = new StateFlowPresentationProvider();

		fTimeGraphCombo.setTimeGraphProvider(fStateFlowPresentationProvider);

		fTimeGraphCombo.setTreeColumns(COLUMN_NAMES);

		fTimeGraphCombo.getTimeGraphViewer().addRangeListener(new ITimeGraphRangeListener() {
			@Override
			public void timeRangeUpdated(TimeGraphRangeUpdateEvent event) {
				final long startTime = event.getStartTime();
				final long endTime = event.getEndTime();
				TmfTimeRange range = new TmfTimeRange(new CtfTmfTimestamp(startTime), new CtfTmfTimestamp(endTime));
				TmfTimestamp time = new CtfTmfTimestamp(fTimeGraphCombo.getTimeGraphViewer().getSelectedTime());
				broadcast(new TmfRangeSynchSignal(StateFlowView.this, range, time));
				if (fZoomThread != null) {
					fZoomThread.cancel();
				}
				startZoomThread(startTime, endTime);
			}
		});

		fTimeGraphCombo.getTimeGraphViewer().addTimeListener(new ITimeGraphTimeListener() {
			@Override
			public void timeSelected(TimeGraphTimeEvent event) {
				long time = event.getTime();
				broadcast(new TmfTimeSynchSignal(StateFlowView.this, new CtfTmfTimestamp(time)));
			}
		});

		fTimeGraphCombo.addSelectionListener(new ITimeGraphSelectionListener() {
			@Override
			public void selectionChanged(TimeGraphSelectionEvent event) {
				// ITimeGraphEntry selection = event.getSelection();
			}
		});

		// FIXME not sure what this should be changed to
		// fTimeGraphCombo.getTimeGraphViewer().setTimeCalendarFormat(true);

		// View Action Handling
		makeActions();
		contributeToActionBars();

		IEditorPart editor = getSite().getPage().getActiveEditor();
		if (editor instanceof ITmfTraceEditor) {
			ITmfTrace trace = ((ITmfTraceEditor) editor).getTrace();
			if (trace != null) {
				traceSelected(new TmfTraceSelectedSignal(this, trace));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		fTimeGraphCombo.setFocus();
	}

	// ------------------------------------------------------------------------
	// Signal handlers
	// ------------------------------------------------------------------------

	/**
	 * Handler for the trace selected signal
	 * 
	 * @param signal
	 *            The signal that's received
	 */
	@TmfSignalHandler
	public void traceSelected(final TmfTraceSelectedSignal signal) {
		if (signal.getTrace() == fTrace) {
			return;
		}
		fTrace = signal.getTrace();

		synchronized (fEntryListMap) {
			fEntryList = fEntryListMap.get(fTrace);
			if (fEntryList == null) {
				synchronized (fBuildThreadMap) {
					BuildThread buildThread = new BuildThread(fTrace);
					fBuildThreadMap.put(fTrace, buildThread);
					buildThread.start();
				}
			} else {
				fStartTime = fTrace.getStartTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
				fEndTime = fTrace.getEndTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
				refresh();
			}
		}
	}

	/**
	 * Trace is closed: clear the data structures and the view
	 * 
	 * @param signal
	 *            the signal received
	 */
	@TmfSignalHandler
	public void traceClosed(final TmfTraceClosedSignal signal) {
		synchronized (fBuildThreadMap) {
			BuildThread buildThread = fBuildThreadMap.remove(signal.getTrace());
			if (buildThread != null) {
				buildThread.cancel();
			}
		}
		synchronized (fEntryListMap) {
			fEntryListMap.remove(signal.getTrace());
		}
		if (signal.getTrace() == fTrace) {
			fTrace = null;
			fStartTime = 0;
			fEndTime = 0;
			if (fZoomThread != null) {
				fZoomThread.cancel();
			}
			refresh();
		}
	}

	private void updateStateSystemPresentationInfo(IStateSystemPresentationInfo statePresentationInfo) {
		fStateFlowPresentationProvider.setPresentationData(statePresentationInfo);
		// NOTE THAT THIS CANNOT BE DONE ON THE CALLERS THREAD, WHICH IS A
		// WORKING THREAD.
		// SO, I AM MOVING IT TO BE DONE ON ANY REFRESH/REDRAW
		// fTimeGraphCombo.setTimeGraphProvider(fStateFlowPresentationProvider);
	}

	/**
	 * Handler for the synch signal
	 * 
	 * @param signal
	 *            The signal that's received
	 */
	@TmfSignalHandler
	public void synchToTime(final TmfTimeSynchSignal signal) {
		if (signal.getSource() == this || fTrace == null) {
			return;
		}
		final long time = signal.getCurrentTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();

		String selectedObjectName = null;
		ITmfTrace[] traces;
		if (fTrace instanceof TmfExperiment) {
			TmfExperiment experiment = (TmfExperiment) fTrace;
			traces = experiment.getTraces();
		} else {
			traces = new ITmfTrace[] { fTrace };
		}
		for (ITmfTrace trace : traces) {
			if (selectedObjectName != null) {
				break;
			}

			// this view only supports traces that have data driven state
			// systems with the associated
			// state presstate systems that we know.

			ITmfStateSystem ss = trace.getStateSystem(XmlDataDrivenStateInputFactory.STATE_SYSTEM_ID);
			if (ss != null) {
				IStateSystemPresentationInfo statePresentationInfo = ss.getStatePresentationInfo();
				if (statePresentationInfo != null) {
					updateStateSystemPresentationInfo(statePresentationInfo);
				}
			}

			// FIXME this does not work at all
			/*if (trace instanceof CtfExecutionTrace) {
				System.err.println("FIXME synchToTime is broken");
				CtfExecutionTrace ctfKernelTrace = (CtfExecutionTrace) trace;
				ITmfStateSystem ssq = ctfKernelTrace.getStateSystem(CtfExecutionTrace.STATE_ID);
				if (time >= ssq.getStartTime() && time <= ssq.getCurrentEndTime()) {
					List<Integer> currentMethodQuarks = ssq.getQuarks(Attributes.THREADS, "*"); //$NON-NLS-1$
					for (int currentMethodQuark : currentMethodQuarks) {
						try {

							ITmfStateInterval currentMethodInterval = ssq.querySingleState(time, currentMethodQuark);
							int currentMethodState = currentMethodInterval.getStateValue().unboxInt();
							if (currentMethodState > 0) {
								// there is a valid state for this method at
								// this point in time
								String currentMethodName = ssq.getAttributeName(currentMethodQuark);
								int statusQuark = ssq.getQuarkAbsolute(Attributes.METHODS, currentMethodName,
										Attributes.STATE);
								ITmfStateInterval statusInterval = ssq.querySingleState(time, statusQuark);
								if (statusInterval.getStartTime() == time) {
									selectedObjectName = currentMethodName;
									break;
								}
							}
						} catch (AttributeNotFoundException e) {
							e.printStackTrace();
						} catch (TimeRangeException e) {
							e.printStackTrace();
						} catch (StateValueTypeException e) {
							e.printStackTrace();
						} catch (StateSystemDisposedException e) {
							// Ignored 
						}
					}
				}
			}	*/		
		}
		final String finalSelectedObjectName = selectedObjectName;

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (fTimeGraphCombo.isDisposed()) {
					return;
				}
				fTimeGraphCombo.setTimeGraphProvider(fStateFlowPresentationProvider);
				fTimeGraphCombo.getTimeGraphViewer().setSelectedTime(time, true);
				startZoomThread(fTimeGraphCombo.getTimeGraphViewer().getTime0(), fTimeGraphCombo.getTimeGraphViewer()
						.getTime1());

				if (finalSelectedObjectName != null) {
					for (Object element : fTimeGraphCombo.getTimeGraphViewer().getExpandedElements()) {
						if (element instanceof StateFlowEntry) {
							StateFlowEntry entry = (StateFlowEntry) element;
							if (entry.getName().equals(finalSelectedObjectName)) {
								fTimeGraphCombo.setSelection(entry);
								break;
							}
						}
					}
				}
			}
		});
	}

	/**
	 * Handler for the range sync signal
	 * 
	 * @param signal
	 *            The signal that's received
	 */
	@TmfSignalHandler
	public void synchToRange(final TmfRangeSynchSignal signal) {
		if (signal.getSource() == this || fTrace == null) {
			return;
		}
		if (signal.getCurrentRange().getIntersection(fTrace.getTimeRange()) == null) {
			return;
		}
		final long startTime = signal.getCurrentRange().getStartTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE)
				.getValue();
		final long endTime = signal.getCurrentRange().getEndTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE)
				.getValue();
		final long time = signal.getCurrentTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (fTimeGraphCombo.isDisposed()) {
					return;
				}
				fTimeGraphCombo.setTimeGraphProvider(fStateFlowPresentationProvider);
				fTimeGraphCombo.getTimeGraphViewer().setStartFinishTime(startTime, endTime);
				fTimeGraphCombo.getTimeGraphViewer().setSelectedTime(time, false);
				startZoomThread(startTime, endTime);
			}
		});
	}

	private void buildEventListForCtfExecutionTrace(CtfExecutionTrace ctfExecutionTrace, IProgressMonitor monitor,
			ArrayList<StateFlowEntry> rootList) {
		fStartTime = Long.MAX_VALUE;
		fEndTime = Long.MIN_VALUE;

		ArrayList<StateFlowEntry> entryList = new ArrayList<StateFlowEntry>();
		ITmfStateSystem ssq = ctfExecutionTrace.getStateSystem(CtfExecutionTrace.STATE_ID);
		if (!ssq.waitUntilBuilt()) {
			return;
		}
		long start = ssq.getStartTime();
		long end = ssq.getCurrentEndTime() + 1;
		fStartTime = Math.min(fStartTime, start);
		fEndTime = Math.max(fEndTime, end);

		// FIXME there must be some better way to iterate and get the quarks
		// than what I am doing here

		List<Integer> threadQuarks = ssq.getQuarks(Attributes.THREADS, "*"); //$NON-NLS-1$
		for (Integer threadQuark : threadQuarks) {
			String threadName = ssq.getAttributeName(threadQuark);

			if (threadName.equals(Attributes.STATE)) {
				continue;
			}

			// munge together a flow entry for the thread itself. Note
			// that the parent of this one is -1
			StateFlowEntry threadEntry = new StateFlowEntry(threadQuark, ctfExecutionTrace, threadName, "", -1, start,
					start, end);
			entryList.add(threadEntry);
			threadEntry.addEvent(new TimeEvent(threadEntry, start, end - start));

			List<Integer> methodQuarks = ssq.getQuarks(Attributes.THREADS, threadName, "*");
			for (Integer methodQuark : methodQuarks) {
				String methodName = ssq.getAttributeName(methodQuark);

				// methodName here includes the full class and everything. split
				// it up so that we have class and method separate.
				String className = "";
				String execName = methodName;

				// TODO make this data driven, coming from the state system
				// perhaps
				int methodDotIndex = methodName.lastIndexOf('.');
				if (methodDotIndex != -1) {
					int packageDotIndex = methodName.lastIndexOf('.', methodDotIndex - 1);
					if (packageDotIndex != -1) {
						execName = methodName.substring(packageDotIndex + 1);
						className = methodName.substring(0, packageDotIndex);
					} else {
						execName = methodName.substring(methodDotIndex + 1);
						className = methodName.substring(0, methodDotIndex);
					}
				}

				long birthTime = -1;
				List<Integer> statusQuarks = ssq
						.getQuarks(Attributes.THREADS, threadName, methodName, Attributes.STATE);
				for (Integer statusQuark : statusQuarks) {
					if (monitor.isCanceled()) {
						return;
					}
					String fullName = ssq.getFullAttributePath(statusQuark);

					try {

						List<ITmfStateInterval> methodIntervals = ssq.queryHistoryRange(statusQuark, start, end - 1); // use
																														// monitor
																														// when
																														// available
																														// in
																														// api
						if (monitor.isCanceled()) {
							return;
						}

						// here we have a list of all instances of the method
						// being called for this thread. We want to create ONE
						// ExecutionFlowEntry
						// for all of these calls so that it is drawn in the
						// same row on the graph. Lets collect the start and end
						// times of the method
						// calls and scope the control flow entry to that. The
						// actual states (running, not running) are done via the
						// events contained
						// within the entry
						long methodStartTime = end;
						long methodEndTime = start;
						long methodBirthTime = -1;

						for (ITmfStateInterval methodInterval : methodIntervals) {
							if (monitor.isCanceled()) {
								return;
							}

							methodStartTime = Math.min(methodStartTime, methodInterval.getStartTime());
							methodEndTime = Math.max(methodEndTime, methodInterval.getEndTime() + 1);

							if (methodBirthTime == -1) {
								methodBirthTime = methodStartTime;
							}
						}
						StateFlowEntry entry = new StateFlowEntry(methodQuark, ctfExecutionTrace, execName, className,
								threadQuark, methodBirthTime, methodStartTime, methodEndTime);
						entryList.add(entry);
						entry.addEvent(new TimeEvent(entry, methodStartTime, methodEndTime - methodStartTime));

					} catch (AttributeNotFoundException e) {
						e.printStackTrace();
					} catch (TimeRangeException e) {
						e.printStackTrace();
					} // catch (StateValueTypeException e) {
						// e.printStackTrace();
						// }
					catch (StateSystemDisposedException e) {
						/* Ignored */
					}

				}
			}
		}
		buildTree(entryList, rootList);
	}

	private void buildEventListForDataDrivenTrace(ITmfTrace trace, IProgressMonitor monitor,
			ArrayList<StateFlowEntry> rootList) {

		ITmfStateSystem ssq = trace.getStateSystem(XmlDataDrivenStateInputFactory.STATE_SYSTEM_ID);
		if (ssq == null) {
			return;
		}
		IStateSystemPresentationInfo statePresentationInfo = ssq.getStatePresentationInfo();
		if (statePresentationInfo == null) {
			return;
		}
		// NOTE: not sure what happens here with multiple traces...
		updateStateSystemPresentationInfo(statePresentationInfo);

		fStartTime = Long.MAX_VALUE;
		fEndTime = Long.MIN_VALUE;

		ArrayList<StateFlowEntry> entryList = new ArrayList<StateFlowEntry>();

		if (!ssq.waitUntilBuilt()) {
			return;
		}
		long startTime = ssq.getStartTime();
		long endTime = ssq.getCurrentEndTime() + 1;
		fStartTime = Math.min(fStartTime, startTime);
		fEndTime = Math.max(fEndTime, endTime);
				
		//FIXME
		//long traceStartTime = trace.getStartTime().getValue();
 		//long traceEndTime = trace.getEndTime().getValue();
        //System.out.println("traceStart='" + traceStartTime + "' traceEndTime='"+traceEndTime+"'" + " ssqStart='" + startTime + "' ssqEndTime='" + endTime + "' fStartTime='" + fStartTime + "' fEndTime='" + fEndTime + "'");
         	                
		final IStateSystemContextHierarchyInfo[] contextHierarchy = statePresentationInfo.getContextHierarchy();

		// the root quark is named after the root context id name
		int rootQuark = -1;
		try {
			rootQuark = ssq.getQuarkAbsolute(contextHierarchy[0].getContextId());
		} catch (AttributeNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		String[] path = new String[2];
		path[0] = contextHierarchy[0].getContextId();
		path[1] = "*";

		List<Integer> childObjectQuarks = ssq.getQuarks(path);

		for (int childObjectQuark : childObjectQuarks) {
			if (monitor.isCanceled()) {
				return;
			}
			recursivelyAddFlowEvents(0, statePresentationInfo, contextHierarchy, trace, entryList, ssq, monitor, path,
					childObjectQuark, rootQuark, startTime, endTime);
		}

		buildTree(entryList, rootList);
	}

	/**
	 * for the given starting object indicated by objectQuark, check to see if
	 * that object has a state quark, and if it does, then add a flow event for
	 * it. If not, search for all children of the object and recurse to cover
	 * them.
	 * 
	 * @param hierarchyLevel
	 *            the current level in the hiearchy. an index into
	 *            contextHierarchy
	 * @param contextHierarchy
	 *            the fixed hierarchy for these flow events
	 * @param trace
	 *            the trace to handle the events for
	 * @param entryList
	 *            the list to add any StateFlowEntry's to
	 * @param ssq
	 *            the state system to query from
	 * @param monitor
	 *            the monitor to use for progress reporting/checking
	 * @param path
	 *            the current path to use for querying for child objects.
	 *            Assumed to always end with "*"
	 * @param objectQuark
	 * @param objectParentQuark
	 * @param startTime
	 * @param endTime
	 */
	private TimeSpan recursivelyAddFlowEvents(int hierarchyLevel,
			final IStateSystemPresentationInfo statePresentationInfo,
			final IStateSystemContextHierarchyInfo[] contextHierarchy, final ITmfTrace trace,
			final ArrayList<StateFlowEntry> entryList, final ITmfStateSystem ssq, final IProgressMonitor monitor,
			String[] path, int objectQuark, int objectParentQuark, long startTime, long endTime) {

		if (objectQuark == -1) {
			return null;
		}
		if (monitor.isCanceled()) {
			return null;
		}

		TimeSpan thisLevelsTimeSpan = new TimeSpan(startTime, endTime);

		String objectName = ssq.getAttributeName(objectQuark);
		
		//FIXME remove this
		System.out.println(String.format("recursivelyAddFlowEvents object=%-20s parent=%02d quark=%02d path='%s'",objectName,objectParentQuark,objectQuark,StringUtils.join(path,"/")));
		
		boolean addedStateAtThisLevel = false;

		// hierarchy level will be -1 the very first time, which is the root
		// node that we do not display
		if (hierarchyLevel >= 0) {
			boolean hasState = contextHierarchy[hierarchyLevel].hasState();

			// check if there is state at this level, and get the attribute and
			// add it if so
			if (hasState) {
				try {
					int objectStateQuark = ssq.getQuarkRelative(objectQuark, Attributes.STATE);
					thisLevelsTimeSpan.add(addStateFlowEventForObject(objectQuark, objectStateQuark, trace, objectName,
							objectParentQuark, entryList, ssq, monitor, startTime, endTime));
					addedStateAtThisLevel = true;
				} catch (AttributeNotFoundException e) {
				}
			}
		}

		// go the next level in the hierarchy if there is one.
		hierarchyLevel++;
		if (hierarchyLevel < contextHierarchy.length) {

			// there is another level. so here we are going to build the
			// appropriate path for the next level,
			// query for all children and recurse on the children to handle the
			// state for them
			path = Arrays.copyOf(path, path.length + 1);
			path[path.length - 2] = objectName; // replaces the previous *
			path[path.length - 1] = "*";

			List<Integer> childObjectQuarks = ssq.getQuarks(path);

			for (int childObjectQuark : childObjectQuarks) {
				if (monitor.isCanceled()) {
					return null;
				}
				thisLevelsTimeSpan.add(recursivelyAddFlowEvents(hierarchyLevel, statePresentationInfo,
						contextHierarchy, trace, entryList, ssq, monitor, path, childObjectQuark, objectQuark,
						startTime, endTime));
			}
		}
		// add a flow entry to cover this level in the hierarchy so that the
		// hierarchy is displayed, but only if there was no state added.
		if (!addedStateAtThisLevel) {
			StateFlowEntry threadEntry = new StateFlowEntry(objectQuark, trace, objectName, "", objectParentQuark,
					thisLevelsTimeSpan.getStartTime(), thisLevelsTimeSpan.getStartTime(),
					thisLevelsTimeSpan.getEndTime());
			entryList.add(threadEntry);
			threadEntry.addEvent(new TimeEvent(threadEntry, thisLevelsTimeSpan.getStartTime(), thisLevelsTimeSpan
					.getEndTime() - thisLevelsTimeSpan.getStartTime()));
		}

		return thisLevelsTimeSpan;
	}

	private TimeSpan addStateFlowEventForObject(int objectQuark, int objectStatusQuark, ITmfTrace trace,
			String objectName, int objectParentQuark, ArrayList<StateFlowEntry> entryList, ITmfStateSystem ssq,
			IProgressMonitor monitor, long start, long end) {
		try {
			
			//FIXME remove this
    		System.out.println(String.format("addStateFlowEventForObject object=%-20s parent=%02d quark=%02d",objectName,objectParentQuark,objectQuark));
    		
			// TODO use monitor when available in api
			List<ITmfStateInterval> objectStateIntervals = ssq.queryHistoryRange(objectStatusQuark, start, end - 1);
			if (monitor.isCanceled()) {
				return null;
			}

			// here we have a list of all state instances for this object. We
			// want to create ONE StateFlowEntry
			// for all of these calls so that it is drawn in the same row on the
			// graph. Lets collect the start and end times of the states
			// and scope the StateFlowEntry to that. The actual state values are
			// done via the events contained within the entry
			long objectStartTime = end;
			long objectEndTime = start;
			long objectBirthTime = -1;
			StateFlowEntry entry = null;
			for (ITmfStateInterval objectStateInterval : objectStateIntervals) {
				if (monitor.isCanceled()) {
					return null;
				}
				
				objectStartTime = Math.min(objectStartTime, objectStateInterval.getStartTime());
				objectEndTime = Math.max(objectEndTime, objectStateInterval.getEndTime() + 1);
				if (objectBirthTime == -1) {
					objectBirthTime = objectStartTime;
				}
				
				//ITmfStateValue stateValue = objectStateInterval.getStateValue();
					
				long startTime = objectStateInterval.getStartTime();
                long endTime = objectStateInterval.getEndTime() + 1;
                if (entry == null) {
                	//FIXME remove this
            		//System.out.println(String.format("create StateFlowEntry object=%-20s parent=%02d quark=%02d %s",objectName,objectParentQuark,objectQuark,stateValue.toString()));
            		
                    entry = new StateFlowEntry(objectQuark, trace, objectName, null, objectParentQuark,
        					objectBirthTime, startTime, endTime);
                    entryList.add(entry);
                }
                entry.addEvent(new TimeEvent(entry, startTime, endTime - startTime));
	             
			}
			return new TimeSpan(objectStartTime, objectEndTime);

		} catch (AttributeNotFoundException e) {
			e.printStackTrace();
		} catch (TimeRangeException e) {
			e.printStackTrace();
		} catch (StateSystemDisposedException e) {
			/* Ignored */
		}
		return null;
	}

	/**
	 * iterate all events in the given trace and create StateFlowEntriesd as
	 * appropriate depending on the event schema
	 * 
	 * @param trace
	 * @param monitor
	 */
	private void buildEventList(final ITmfTrace trace, IProgressMonitor monitor) {
		fStartTime = Long.MAX_VALUE;
		fEndTime = Long.MIN_VALUE;
		ITmfTrace[] traces;
		if (trace instanceof TmfExperiment) {
			TmfExperiment experiment = (TmfExperiment) trace;
			traces = experiment.getTraces();
		} else {
			traces = new ITmfTrace[] { trace };
		}
		ArrayList<StateFlowEntry> rootList = new ArrayList<StateFlowEntry>();
		for (ITmfTrace aTrace : traces) {
			if (monitor.isCanceled()) {
				return;
			}
			//if (aTrace instanceof CtfExecutionTrace) {
			//	CtfExecutionTrace ctfExecutionTrace = (CtfExecutionTrace) aTrace;
			//	buildEventListForCtfExecutionTrace(ctfExecutionTrace, monitor, rootList);
			//} else {
				buildEventListForDataDrivenTrace(aTrace, monitor, rootList);
			//}
			Collections.sort(rootList, fExecutionFlowEntryComparator);
			synchronized (fEntryListMap) {
				fEntryListMap.put(trace, (ArrayList<StateFlowEntry>) rootList.clone());
			}
			if (trace == fTrace) {
				refresh();
			}
		}
		for (StateFlowEntry entry : rootList) {
			if (monitor.isCanceled()) {
				return;
			}
			buildStatusEvents(trace, entry, monitor);
		}
	}

	private static void buildTree(ArrayList<StateFlowEntry> entryList, ArrayList<StateFlowEntry> rootList) {

		// iterate all nodes and check to see if they need to be added as a
		// child
		// underneath another node.
		for (StateFlowEntry entry : entryList) {
			boolean root = true;
			// if the entry has a parent quark that is -1 that means it is a top
			// level node
			int entryQuark = entry.getNodeQuark();
			int entryParentQuark = entry.getParentQuark();
			if (entryParentQuark != -1) {
				// the entry has a parent. search all nodes for this parent and
				// then
				// add the entry as a child of that parent
				for (StateFlowEntry parent : entryList) {
					if (parent.getNodeQuark() == entryParentQuark && entry.getStartTime() >= parent.getStartTime()
							&& entry.getStartTime() <= parent.getEndTime()) {
						parent.addChild(entry);
						root = false;
						break;
					}
				}
				if (root == true) {
					System.err.println("entry '" + entry.getName() + "' parentQuark=" + entry.getParentQuark()
							+ " not found");
				}
			}
			if (root) {
				rootList.add(entry);
			}
		}
	}

	private void buildStatusEvents(ITmfTrace trace, StateFlowEntry entry, IProgressMonitor monitor) {
		ITmfStateSystem ssq = entry.getTrace().getStateSystem(XmlDataDrivenStateInputFactory.STATE_SYSTEM_ID);
		long start = ssq.getStartTime();
		long end = ssq.getCurrentEndTime() + 1;
		long resolution = Math.max(1, (end - start) / fDisplayWidth);
		List<ITimeEvent> eventList = getEventList(entry, entry.getStartTime(), entry.getEndTime(), resolution, monitor);
		if (monitor.isCanceled()) {
			return;
		}
		entry.setEventList(eventList);
		if (trace == fTrace) {
			redraw();
		}
		for (ITimeGraphEntry child : entry.getChildren()) {
			if (monitor.isCanceled()) {
				return;
			}
			buildStatusEvents(trace, (StateFlowEntry) child, monitor);
		}
	}

	private static List<ITimeEvent> getEventList(StateFlowEntry entry, long startTime, long endTime, long resolution,
			IProgressMonitor monitor) {
		startTime = Math.max(startTime, entry.getStartTime());
		endTime = Math.min(endTime, entry.getEndTime());
		if (endTime <= startTime) {
			return null;
		}
		ITmfStateSystem ssq = entry.getTrace().getStateSystem(XmlDataDrivenStateInputFactory.STATE_SYSTEM_ID);
		
		long ssqStartTime = ssq.getStartTime();
		long ssqEndTime = ssq.getCurrentEndTime();
		startTime = ssqStartTime;
		endTime = ssqEndTime; //FIXME
				
		List<ITimeEvent> eventList = null;
		int currentQuark = -1;
		try {
			currentQuark = entry.getNodeQuark();
			int statusQuark = ssq.getQuarkRelative(entry.getNodeQuark(), Attributes.STATE);			
			List<ITmfStateInterval> statusIntervals = ssq.queryHistoryRange(statusQuark, startTime, endTime - 1,
					resolution, monitor);
			
			//FIXME
			//System.out.println("Querying for quark=" + entry.getNodeQuark() + "statusQuark=" + statusQuark + " intervals=" + statusIntervals.size() + " between startTime=" + startTime + " endTime=" + (endTime - 1) );
			//if (statusIntervals.size() > 1) {
			//	System.out.println("This entry is greater than one interval "+ entry);
			//}
			
			eventList = new ArrayList<ITimeEvent>(statusIntervals.size());
			long lastEndTime = -1;
			for (ITmfStateInterval statusInterval : statusIntervals) {
				if (monitor.isCanceled()) {
					return null;
				}
				long time = statusInterval.getStartTime();
				long duration = statusInterval.getEndTime() - time + 1;
				int stateIndex = 0;
				try {
					stateIndex = statusInterval.getStateValue().unboxInt();
				} catch (StateValueTypeException e) {
					e.printStackTrace();
				}

				if (lastEndTime != time && lastEndTime != -1) {
					eventList.add(new StateFlowEvent(entry, lastEndTime, time - lastEndTime, -1));
				}
				eventList.add(new StateFlowEvent(entry, time, duration, stateIndex));
				lastEndTime = time + duration;
			}
		} catch (AttributeNotFoundException e) {
			// System.err.println("attribute not found in quark " + currentQuark
			// + " " + ssq.getAttributeName(currentQuark));
			// e.printStackTrace();
		} catch (TimeRangeException e) {
			e.printStackTrace();
		} catch (StateSystemDisposedException e) {
			/* Ignored */
		}
		return eventList;
	}

	private void refresh() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (fTimeGraphCombo.isDisposed()) {
					return;
				}
				ITimeGraphEntry[] entries = null;
				synchronized (fEntryListMap) {
					fEntryList = fEntryListMap.get(fTrace);
					if (fEntryList == null) {
						fEntryList = new ArrayList<StateFlowEntry>();
					}
					entries = fEntryList.toArray(new ITimeGraphEntry[0]);
				}
				Arrays.sort(entries, fExecutionFlowEntryComparator);
				fTimeGraphCombo.setTimeGraphProvider(fStateFlowPresentationProvider);
				fTimeGraphCombo.setInput(entries);
				fTimeGraphCombo.getTimeGraphViewer().setTimeBounds(fStartTime, fEndTime);

				long timestamp = fTrace == null ? 0 : fTrace.getCurrentTime()
						.normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
				long startTime = fTrace == null ? 0 : fTrace.getCurrentRange().getStartTime()
						.normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
				long endTime = fTrace == null ? 0 : fTrace.getCurrentRange().getEndTime()
						.normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
				startTime = Math.max(startTime, fStartTime);
				endTime = Math.min(endTime, fEndTime);
				fTimeGraphCombo.getTimeGraphViewer().setSelectedTime(timestamp, false);
				fTimeGraphCombo.getTimeGraphViewer().setStartFinishTime(startTime, endTime);

				for (TreeColumn column : fTimeGraphCombo.getTreeViewer().getTree().getColumns()) {
					column.pack();
				}

				startZoomThread(startTime, endTime);
			}
		});
	}

	private void redraw() {
		synchronized (fSyncObj) {
			if (fRedrawState == State.IDLE) {
				fRedrawState = State.BUSY;
			} else {
				fRedrawState = State.PENDING;
				return;
			}
		}
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (fTimeGraphCombo.isDisposed()) {
					return;
				}
				fTimeGraphCombo.redraw();
				fTimeGraphCombo.update();
				synchronized (fSyncObj) {
					if (fRedrawState == State.PENDING) {
						fRedrawState = State.IDLE;
						redraw();
					} else {
						fRedrawState = State.IDLE;
					}
				}
			}
		});
	}

	private void startZoomThread(long startTime, long endTime) {
		if (fZoomThread != null) {
			fZoomThread.cancel();
		}
		fZoomThread = new ZoomThread(fEntryList, startTime, endTime);
		fZoomThread.start();
	}

	private void makeActions() {
		fPreviousResourceAction = fTimeGraphCombo.getTimeGraphViewer().getPreviousItemAction();
		fPreviousResourceAction.setText(Messages.StateFlowView_previousProcessActionNameText);
		fPreviousResourceAction.setToolTipText(Messages.StateFlowView_previousProcessActionToolTipText);
		fNextResourceAction = fTimeGraphCombo.getTimeGraphViewer().getNextItemAction();
		fNextResourceAction.setText(Messages.StateFlowView_nextProcessActionNameText);
		fNextResourceAction.setToolTipText(Messages.StateFlowView_nextProcessActionToolTipText);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(fTimeGraphCombo.getTimeGraphViewer().getShowLegendAction());
		manager.add(new Separator());
		manager.add(fTimeGraphCombo.getTimeGraphViewer().getResetScaleAction());
		manager.add(fTimeGraphCombo.getTimeGraphViewer().getPreviousEventAction());
		manager.add(fTimeGraphCombo.getTimeGraphViewer().getNextEventAction());
		manager.add(fPreviousResourceAction);
		manager.add(fNextResourceAction);
		manager.add(fTimeGraphCombo.getTimeGraphViewer().getZoomInAction());
		manager.add(fTimeGraphCombo.getTimeGraphViewer().getZoomOutAction());
		manager.add(new Separator());
	}
}
