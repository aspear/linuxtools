package org.eclipse.linuxtools.internal.tmf.stateflow.core.stateprovider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.linuxtools.tmf.core.TmfCommonConstants;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateChangeInput;
import org.eclipse.linuxtools.tmf.core.statesystem.IStateSystemFactory;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystem;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateSystemBuilder;
import org.eclipse.linuxtools.tmf.core.statesystem.StateSystemManager;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

public class XmlDataDrivenStateInputFactory implements IStateSystemFactory {
	
	public final static String STATE_SYSTEM_ID = "org.eclipse.linuxtools.internal.tmf.executiontrace.core.stateprovider.datadriven";
	/**
     * The file name of the History Tree
     */
    public final static String HISTORY_TREE_FILE_NAME = "stateHistory.ht"; //$NON-NLS-1$
    
    public XmlDataDrivenStateInputFactory() {    	
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
	    
	    //build presentation from the XML.  might throw if the XML is bad.
	    StateSystemPresentationInfo statePresentationInfo = buildStatePresentationInfo(stateSchemaXml);	       
	    
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
        final IStateChangeInput htInput = new DataDrivenStateInput(statePresentationInfo,trace,trace.getEventType(),STATE_SYSTEM_ID);

        ITmfStateSystem ss = StateSystemManager.loadStateHistory(htFile, htInput, false);
        
        //FIXME this is not really elegant, but I have not figured out the right way to do this: in the case that this
        //ss is a load of an existing state system from disk, it would appear that the IStateChangeInput instance is not used,
        //and because of this the state presentation info does not get set in the ss
        if (ss instanceof ITmfStateSystemBuilder) {
        	ITmfStateSystemBuilder ssb = (ITmfStateSystemBuilder)ss;
        	ssb.setStatePresentationInfo(statePresentationInfo);
        }
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
		
		// in either the case where it is a directory (as is done with CTF) or a trace file, we
		// want to check for the schema file in the parent directory
		traceDirectory = traceFile.getParentFile();
		if (traceDirectory == null) {
			traceDirectory = traceFile; //might be OS file system root
		}
		
		return new File(traceDirectory, trace.getName() + ".state-schema.xml");
	}
	
	private StateSystemPresentationInfo buildStatePresentationInfo(File stateSchemaXml) 
			throws TmfTraceException {				
		InputStream xmlInputStream;
		try {
			xmlInputStream = new BufferedInputStream(new FileInputStream(stateSchemaXml));
		} catch (FileNotFoundException e) {
			throw new TmfTraceException(e.getMessage(),e); //this can't happen here anyway
		}
		return XmlStateSystemPresentationInfoBuilder.build(xmlInputStream);
	}
}
