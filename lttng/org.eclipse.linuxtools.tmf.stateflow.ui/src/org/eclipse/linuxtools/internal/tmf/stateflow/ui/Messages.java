package org.eclipse.linuxtools.internal.tmf.stateflow.ui;

import org.eclipse.osgi.util.NLS;

@SuppressWarnings("javadoc")
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.linuxtools.internal.tmf.stateflow.ui.messages"; //$NON-NLS-1$

    //public static String StateFlowView_birthTimeColumn;
    public static String StateFlowView_contextColumn;
    //public static String StateFlowView_nameColumn;
    //public static String StateFlowView_scopeColumn;
    public static String StateFlowView_traceColumn;

    public static String StateFlowView_stateTypeName;
    public static String StateFlowView_nextObjectActionNameText;
    public static String StateFlowView_nextObjectActionToolTipText;
    public static String StateFlowView_previousObjectActionNameText;
    public static String StateFlowView_previousObjectActionToolTipText;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
