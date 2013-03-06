package org.eclipse.linuxtools.internal.tmf.stateflow.ui;

import org.eclipse.osgi.util.NLS;

@SuppressWarnings("javadoc")
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.linuxtools.internal.tmf.stateflow.ui.messages"; //$NON-NLS-1$

    public static String StateFlowView_birthTimeColumn;
    public static String StateFlowView_contextColumn;
    public static String StateFlowView_nameColumn;
    public static String StateFlowView_scopeColumn;
    public static String StateFlowView_traceColumn;

    public static String StateFlowView_stateTypeName;
    public static String StateFlowView_nextProcessActionNameText;
    public static String StateFlowView_nextProcessActionToolTipText;
    public static String StateFlowView_previousProcessActionNameText;
    public static String StateFlowView_previousProcessActionToolTipText;

    public static String StateFlowView_attributeSyscallName;
    public static String StateFlowView_attributeCpuName;

    public static String ResourcesView_stateTypeName;
    public static String ResourcesView_nextResourceActionNameText;
    public static String ResourcesView_nextResourceActionToolTipText;
    public static String ResourcesView_previousResourceActionNameText;
    public static String ResourcesView_previousResourceActionToolTipText;
    public static String ResourcesView_attributeCpuName;
    public static String ResourcesView_attributeIrqName;
    public static String ResourcesView_attributeSoftIrqName;
    public static String ResourcesView_attributeHoverTime;
    public static String ResourcesView_attributeTidName;
    public static String ResourcesView_attributeProcessName;
    public static String ResourcesView_attributeSyscallName;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
