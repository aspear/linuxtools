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

package org.eclipse.linuxtools.tmf.executionflow.ui.views.executionflow;

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
import org.eclipse.linuxtools.internal.tmf.executionflow.ui.Messages;
import org.eclipse.linuxtools.internal.tmf.executiontrace.core.Attributes;
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
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystem;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;
import org.eclipse.linuxtools.tmf.executiontrace.core.trace.CtfExecutionTrace;
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
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.widgets.Utils;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.widgets.Utils.Resolution;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.widgets.Utils.TimeFormat;
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
public class ExecutionFlowView extends TmfView {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /**
     * View ID.
     */
    public static final String ID = "org.eclipse.linuxtools.tmf.executionflow.ui.views.executionflow"; //$NON-NLS-1$

    private static final String CONTEXT_COLUMN        = Messages.ExecutionFlowView_contextColumn;
    private static final String NAME_COLUMN     = Messages.ExecutionFlowView_nameColumn;
    private static final String SCOPE_COLUMN     = Messages.ExecutionFlowView_scopeColumn;
    private static final String BIRTH_TIME_COLUMN = Messages.ExecutionFlowView_birthTimeColumn;
    private static final String TRACE_COLUMN      = Messages.ExecutionFlowView_traceColumn;

    private final String[] COLUMN_NAMES = new String[] {
    		CONTEXT_COLUMN,
    		NAME_COLUMN,
            SCOPE_COLUMN,
            BIRTH_TIME_COLUMN,
            TRACE_COLUMN
    };

    /**
     * Redraw state enum
     */
    private enum State { IDLE, BUSY, PENDING }

    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    // The timegraph combo
    private TimeGraphCombo fTimeGraphCombo;

    // The selected trace
    private ITmfTrace fTrace;

    // The timegraph entry list
    private ArrayList<ExecutionFlowEntry> fEntryList;

    // The trace to entry list hash map
    final private HashMap<ITmfTrace, ArrayList<ExecutionFlowEntry>> fEntryListMap = new HashMap<ITmfTrace, ArrayList<ExecutionFlowEntry>>();

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
    private final ExecutionFlowEntryComparator fExecutionFlowEntryComparator = new ExecutionFlowEntryComparator();

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
            ExecutionFlowEntry entry = (ExecutionFlowEntry) element;
            if (columnIndex == 0) {
                return entry.getName();
            } else if (columnIndex == 1) {
                return entry.getContextInfo();
            } else if (columnIndex == 2) {
                return Integer.toHexString(entry.getParentQuark());
            } else if (columnIndex == 3) {
                return Utils.formatTime(entry.getBirthTime(), TimeFormat.ABSOLUTE, Resolution.NANOSEC);
            } else if (columnIndex == 4) {
                return entry.getTrace().getName();
            }
            return ""; //$NON-NLS-1$
        }

    }

    private static class ExecutionFlowEntryComparator implements Comparator<ITimeGraphEntry> {

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            int result = 0;

            if ((o1 instanceof ExecutionFlowEntry) && (o2 instanceof ExecutionFlowEntry)) {
                ExecutionFlowEntry entry1 = (ExecutionFlowEntry) o1;
                ExecutionFlowEntry entry2 = (ExecutionFlowEntry) o2;
                result = entry1.getTrace().getStartTime().compareTo(entry2.getTrace().getStartTime());
                if (result == 0) {
                    result = entry1.getTrace().getName().compareTo(entry2.getTrace().getName());
                }
                if (result == 0) {
                	
                    result = (entry1.getNodeQuark() < entry2.getNodeQuark()) ? -1 : entry1.getNodeQuark() > entry2.getNodeQuark() ? 1 : 0;
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
            super("ExecutionFlowView build"); //$NON-NLS-1$
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
        private final ArrayList<ExecutionFlowEntry> fZoomEntryList;
        private final long fZoomStartTime;
        private final long fZoomEndTime;
        private final long fResolution;
        private final IProgressMonitor fMonitor;

        public ZoomThread(ArrayList<ExecutionFlowEntry> entryList, long startTime, long endTime) {
            super("ExecutionFlowView zoom"); //$NON-NLS-1$
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
            for (ExecutionFlowEntry entry : fZoomEntryList) {
                if (fMonitor.isCanceled()) {
                    break;
                }
                zoom(entry, fMonitor);
            }
        }

        private void zoom(ExecutionFlowEntry entry, IProgressMonitor monitor) {
            if (fZoomStartTime <= fStartTime && fZoomEndTime >= fEndTime) {
                entry.setZoomedEventList(null);
            } else {
                List<ITimeEvent> zoomedEventList = getEventList(entry, fZoomStartTime, fZoomEndTime, fResolution, monitor);
                if (zoomedEventList != null) {
                    entry.setZoomedEventList(zoomedEventList);
                }
            }
            redraw();
            for (ExecutionFlowEntry child : entry.getChildren()) {
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
    public ExecutionFlowView() {
        super(ID);
        fDisplayWidth = Display.getDefault().getBounds().width;
    }

    // ------------------------------------------------------------------------
    // ViewPart
    // ------------------------------------------------------------------------

    /* (non-Javadoc)
     * @see org.eclipse.linuxtools.tmf.ui.views.TmfView#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent) {
        fTimeGraphCombo = new TimeGraphCombo(parent, SWT.NONE);

        fTimeGraphCombo.setTreeContentProvider(new TreeContentProvider());

        fTimeGraphCombo.setTreeLabelProvider(new TreeLabelProvider());

        fTimeGraphCombo.setTimeGraphProvider(new ExecutionFlowPresentationProvider());

        fTimeGraphCombo.setTreeColumns(COLUMN_NAMES);

        fTimeGraphCombo.getTimeGraphViewer().addRangeListener(new ITimeGraphRangeListener() {
            @Override
            public void timeRangeUpdated(TimeGraphRangeUpdateEvent event) {
                final long startTime = event.getStartTime();
                final long endTime = event.getEndTime();
                TmfTimeRange range = new TmfTimeRange(new CtfTmfTimestamp(startTime), new CtfTmfTimestamp(endTime));
                TmfTimestamp time = new CtfTmfTimestamp(fTimeGraphCombo.getTimeGraphViewer().getSelectedTime());
                broadcast(new TmfRangeSynchSignal(ExecutionFlowView.this, range, time));
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
                broadcast(new TmfTimeSynchSignal(ExecutionFlowView.this, new CtfTmfTimestamp(time)));
            }
        });

        fTimeGraphCombo.addSelectionListener(new ITimeGraphSelectionListener() {
            @Override
            public void selectionChanged(TimeGraphSelectionEvent event) {
                //ITimeGraphEntry selection = event.getSelection();
            }
        });

        fTimeGraphCombo.getTimeGraphViewer().setTimeCalendarFormat(true);

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

    /* (non-Javadoc)
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
     * @param signal the signal received
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

        String selectedMethodName = null;
        ITmfTrace[] traces;
        if (fTrace instanceof TmfExperiment) {
            TmfExperiment experiment = (TmfExperiment) fTrace;
            traces = experiment.getTraces();
        } else {
            traces = new ITmfTrace[] { fTrace };
        }
        for (ITmfTrace trace : traces) {
            if (selectedMethodName != null) {
                break;
            }
            // FIXME this does not work at all
            if (trace instanceof CtfExecutionTrace) {
            	System.err.println("FIXME syntToTime is broken");
                CtfExecutionTrace ctfKernelTrace = (CtfExecutionTrace) trace;
                ITmfStateSystem ssq = ctfKernelTrace.getStateSystem(CtfExecutionTrace.STATE_ID);
                if (time >= ssq.getStartTime() && time <= ssq.getCurrentEndTime()) {
                    List<Integer> currentMethodQuarks = ssq.getQuarks(Attributes.THREADS, "*");  //$NON-NLS-1$
                    for (int currentMethodQuark : currentMethodQuarks) {
                        try {
                        	
                        	ITmfStateInterval currentMethodInterval = ssq.querySingleState(time, currentMethodQuark);
                            int currentMethodState = currentMethodInterval.getStateValue().unboxInt();
                            if (currentMethodState > 0) {
                            	//there is a valid state for this method at this point in time
                            	String currentMethodName = ssq.getAttributeName(currentMethodQuark);
                                int statusQuark = ssq.getQuarkAbsolute(Attributes.METHODS,currentMethodName, Attributes.STATUS);
                                ITmfStateInterval statusInterval = ssq.querySingleState(time, statusQuark);
                                if (statusInterval.getStartTime() == time) {
                                	selectedMethodName = currentMethodName;
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
                            /* Ignored */
                        }
                    }
                }
            }
        }
        final String finalSelectedMethodName = selectedMethodName;

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (fTimeGraphCombo.isDisposed()) {
                    return;
                }
                fTimeGraphCombo.getTimeGraphViewer().setSelectedTime(time, true);
                startZoomThread(fTimeGraphCombo.getTimeGraphViewer().getTime0(), fTimeGraphCombo.getTimeGraphViewer().getTime1());

                if (finalSelectedMethodName != null) {
                    for (Object element : fTimeGraphCombo.getTimeGraphViewer().getExpandedElements()) {
                        if (element instanceof ExecutionFlowEntry) {
                            ExecutionFlowEntry entry = (ExecutionFlowEntry) element;
                            if (entry.getName().equals(finalSelectedMethodName)) {
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
        final long startTime = signal.getCurrentRange().getStartTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
        final long endTime = signal.getCurrentRange().getEndTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
        final long time = signal.getCurrentTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (fTimeGraphCombo.isDisposed()) {
                    return;
                }
                fTimeGraphCombo.getTimeGraphViewer().setStartFinishTime(startTime, endTime);
                fTimeGraphCombo.getTimeGraphViewer().setSelectedTime(time, false);
                startZoomThread(startTime, endTime);
            }
        });
    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------

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
        ArrayList<ExecutionFlowEntry> rootList = new ArrayList<ExecutionFlowEntry>();
        for (ITmfTrace aTrace : traces) {
            if (monitor.isCanceled()) {
                return;
            }
            if (aTrace instanceof CtfExecutionTrace) {
                ArrayList<ExecutionFlowEntry> entryList = new ArrayList<ExecutionFlowEntry>();
                CtfExecutionTrace ctfExecutionTrace = (CtfExecutionTrace) aTrace;
                ITmfStateSystem ssq = ctfExecutionTrace.getStateSystem(CtfExecutionTrace.STATE_ID);
                if (!ssq.waitUntilBuilt()) {
                    return;
                }
                long start = ssq.getStartTime();
                long end = ssq.getCurrentEndTime() + 1;
                fStartTime = Math.min(fStartTime, start);
                fEndTime = Math.max(fEndTime, end);
                
                //FIXME there must be some better way to iterate and get the quarks than what I am doing here
                
                List<Integer> threadQuarks = ssq.getQuarks(Attributes.THREADS, "*"); //$NON-NLS-1$
                for (Integer threadQuark : threadQuarks) {
                	String threadName = ssq.getAttributeName(threadQuark);
                	                	
                	//munge together a flow entry for the thread itself.  Note that the parent of this one is -1
                	ExecutionFlowEntry threadEntry = new ExecutionFlowEntry(threadQuark, ctfExecutionTrace, threadName,"",-1, start, start, end);
                    entryList.add(threadEntry);
                    threadEntry.addEvent(new TimeEvent(threadEntry, start, end - start));
                    
                	List<Integer> methodQuarks = ssq.getQuarks(Attributes.THREADS, threadName,"*");
	                for (Integer methodQuark : methodQuarks) {
	                	String methodName = ssq.getAttributeName(methodQuark);	                	
	                	List<Integer> statusQuarks = ssq.getQuarks(Attributes.THREADS, threadName, methodName, Attributes.STATUS);
	                	for (Integer statusQuark : statusQuarks) {
		                    if (monitor.isCanceled()) {
		                        return;
		                    }		                    
		                    String fullName = ssq.getFullAttributePath(statusQuark);
		                        
		                    try {
		                       
		                        List<ITmfStateInterval> methodIntervals = ssq.queryHistoryRange(statusQuark, start, end - 1); // use monitor when available in api
		                        if (monitor.isCanceled()) {
		                            return;
		                        }
		                        long birthTime = -1;
		                        for (ITmfStateInterval methodInterval : methodIntervals) {
		                            if (monitor.isCanceled()) {
		                                return;
		                            }
		                            birthTime = -1;
		                            if (!methodInterval.getStateValue().isNull()) {
		                            	
		                            	byte type =  methodInterval.getStateValue().getType();
		                            	if (type== 0) {
		                            		// methodName here includes the full class and everything.  split it up so that we have class and method separate.
		                            		String className = "";
		                            		String execName = methodName;
		                            		
		                            		// TODO make this data driven, coming from the state system perhaps
		                            		int methodDotIndex = methodName.lastIndexOf('.');
		                            		if (methodDotIndex != -1) {
		                            			int packageDotIndex = methodName.lastIndexOf('.', methodDotIndex-1);
		                            			if (packageDotIndex != -1) {
			                            			execName = methodName.substring(packageDotIndex+1);
			                            			className = methodName.substring(0, packageDotIndex);
		                            			} else {
		                            				execName = methodName.substring(methodDotIndex+1);
			                            			className = methodName.substring(0, methodDotIndex);
		                            			}
		                            		}
		                            					                                
			                                long startTime = methodInterval.getStartTime();
			                                long endTime = methodInterval.getEndTime() + 1;
			                                if (birthTime == -1) {
			                                    birthTime = startTime;
			                                }
			                               
			                                ExecutionFlowEntry entry = new ExecutionFlowEntry(methodQuark, ctfExecutionTrace, execName, className,threadQuark, birthTime, startTime, endTime);
			                                entryList.add(entry);
			                                entry.addEvent(new TimeEvent(entry, startTime, endTime - startTime));
		                            	 }
		                            } 
		                        }
		                    } catch (AttributeNotFoundException e) {
		                        e.printStackTrace();
		                    } catch (TimeRangeException e) {
		                        e.printStackTrace();
		                    } //catch (StateValueTypeException e) {
		                      //  e.printStackTrace();
		                    //}
		                    catch (StateSystemDisposedException e) {
		                        /* Ignored */
		                    }
	                		
	                	}
	                }
                }
                buildTree(entryList, rootList);
            }
            Collections.sort(rootList, fExecutionFlowEntryComparator);
            synchronized (fEntryListMap) {
                fEntryListMap.put(trace, (ArrayList<ExecutionFlowEntry>) rootList.clone());
            }
            if (trace == fTrace) {
                refresh();
            }
        }
        for (ExecutionFlowEntry entry : rootList) {
            if (monitor.isCanceled()) {
                return;
            }
            buildStatusEvents(trace, entry, monitor);
        }
    }

    private static void buildTree(ArrayList<ExecutionFlowEntry> entryList,
            ArrayList<ExecutionFlowEntry> rootList) {
    	
    	// iterate all nodes and check to see if they need to be added as a child
    	// underneath another node.
        for (ExecutionFlowEntry entry : entryList) {
            boolean root = true;            
            // if the entry has a parent quark that is -1 that means it is a top level node
            int entryQuark = entry.getNodeQuark();
            int entryParentQuark = entry.getParentQuark();
            if ( entryParentQuark != -1) {
            	// the entry has a parent.  search all nodes for this parent and then 
            	// add the entry as a child of that parent
                for (ExecutionFlowEntry parent : entryList) {
                    if (parent.getNodeQuark() == entryParentQuark && 
                            entry.getStartTime() >= parent.getStartTime() &&
                            entry.getStartTime() <= parent.getEndTime()) {
                        parent.addChild(entry);
                        root = false;
                        break;
                    }
                }
            }
            if (root) {
                rootList.add(entry);
            }
        }
    }

    private void buildStatusEvents(ITmfTrace trace, ExecutionFlowEntry entry, IProgressMonitor monitor) {
        ITmfStateSystem ssq = entry.getTrace().getStateSystem(CtfExecutionTrace.STATE_ID);
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
            buildStatusEvents(trace, (ExecutionFlowEntry) child, monitor);
        }
    }

    private static List<ITimeEvent> getEventList(ExecutionFlowEntry entry,
            long startTime, long endTime, long resolution,
            IProgressMonitor monitor) {
        startTime = Math.max(startTime, entry.getStartTime());
        endTime = Math.min(endTime, entry.getEndTime());
        if (endTime <= startTime) {
            return null;
        }
        ITmfStateSystem ssq = entry.getTrace().getStateSystem(CtfExecutionTrace.STATE_ID);
        List<ITimeEvent> eventList = null;
        try {
            int statusQuark = ssq.getQuarkRelative(entry.getNodeQuark(), Attributes.STATUS);
            List<ITmfStateInterval> statusIntervals = ssq.queryHistoryRange(statusQuark, startTime, endTime - 1, resolution, monitor);
            eventList = new ArrayList<ITimeEvent>(statusIntervals.size());
            long lastEndTime = -1;
            for (ITmfStateInterval statusInterval : statusIntervals) {
                if (monitor.isCanceled()) {
                    return null;
                }
                long time = statusInterval.getStartTime();
                long duration = statusInterval.getEndTime() - time + 1;
                int status = -1;
                try {
                    status = statusInterval.getStateValue().unboxInt();
                } catch (StateValueTypeException e) {
                    e.printStackTrace();
                }
                if (lastEndTime != time && lastEndTime != -1) {
                    eventList.add(new ExecutionFlowEvent(entry, lastEndTime, time - lastEndTime, 0));
                }
                eventList.add(new ExecutionFlowEvent(entry, time, duration, status));
                lastEndTime = time + duration;
            }
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
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
                        fEntryList = new ArrayList<ExecutionFlowEntry>();
                    }
                    entries = fEntryList.toArray(new ITimeGraphEntry[0]);
                }
                Arrays.sort(entries, fExecutionFlowEntryComparator);
                fTimeGraphCombo.setInput(entries);
                fTimeGraphCombo.getTimeGraphViewer().setTimeBounds(fStartTime, fEndTime);

                long timestamp = fTrace == null ? 0 : fTrace.getCurrentTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
                long startTime = fTrace == null ? 0 : fTrace.getCurrentRange().getStartTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
                long endTime = fTrace == null ? 0 : fTrace.getCurrentRange().getEndTime().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
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
        fPreviousResourceAction.setText(Messages.ExecutionFlowView_previousProcessActionNameText);
        fPreviousResourceAction.setToolTipText(Messages.ExecutionFlowView_previousProcessActionToolTipText);
        fNextResourceAction = fTimeGraphCombo.getTimeGraphViewer().getNextItemAction();
        fNextResourceAction.setText(Messages.ExecutionFlowView_nextProcessActionNameText);
        fNextResourceAction.setToolTipText(Messages.ExecutionFlowView_nextProcessActionToolTipText);
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
