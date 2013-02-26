package org.eclipse.linuxtools.internal.tmf.executiontrace.core.stateprovider;

import java.io.File;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.linuxtools.tmf.core.TmfCommonConstants;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateChangeInput;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemFactory;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystem;
import org.eclipse.linuxtools.tmf.core.statesystem.StateSystemManager;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

public class DataDrivenStateInputFactory implements IStateSystemFactory {
	
	public final static String STATE_SYSTEM_ID = "org.eclipse.linuxtools.internal.tmf.executiontrace.core.stateprovider.datadriven";
	/**
     * The file name of the History Tree
     */
    public final static String HISTORY_TREE_FILE_NAME = "stateHistory.ht"; //$NON-NLS-1$
    
    public DataDrivenStateInputFactory() {    	
    }
    
	@Override
	public String getStateSystemId() {
		return STATE_SYSTEM_ID;
	}
	
	@Override
	public boolean canCreateStateSystem(ITmfTrace trace) {		
		File stateSchemaXml = createStateSchemaFile(trace);
	    return (stateSchemaXml.exists() && stateSchemaXml.isFile());
	}
	
	@Override
	public ITmfStateSystem createStateSystem(ITmfTrace trace)
			throws TmfTraceException {
		
		File stateSchemaXml = createStateSchemaFile(trace);
	    if (!stateSchemaXml.exists() || !stateSchemaXml.isFile()) {
	    	return null;
	    }
	    //the schema file exists!  that means we can create a state system for it.
	    /* Set up the path to the history tree file we'll use */
        IResource resource = trace.getResource();
        String supplDirectory = null;

        try {
            // get the directory where the history file will be stored.
            supplDirectory = resource.getPersistentProperty(TmfCommonConstants.TRACE_SUPPLEMENTARY_FOLDER);
        } catch (CoreException e) {
            throw new TmfTraceException(e.toString(), e);
        }

        final File htFile = new File(supplDirectory + File.separator + HISTORY_TREE_FILE_NAME);        
        
        // NOTE: I have no idea what id to use for the state input, so using system id        
        final IStateChangeInput htInput = new DataDrivenStateInput(trace,trace.getEventType(),STATE_SYSTEM_ID);

        ITmfStateSystem ss = StateSystemManager.loadStateHistory(htFile, htInput, false);
        return ss;	    
	}
	
	/**
	 * create the state schema file.  Note that that it may or may not exist...
	 * @param trace
	 * @return
	 */
	private File createStateSchemaFile(ITmfTrace trace) {

		File traceDirectory;
		File traceFile = new File(trace.getPath());
		if (traceFile.isDirectory()) {
			traceDirectory = traceFile;
		} else {
			traceDirectory = traceFile.getParentFile();
		}
		return new File(traceDirectory, trace.getName() + ".state-schema.xml");
	}


}
