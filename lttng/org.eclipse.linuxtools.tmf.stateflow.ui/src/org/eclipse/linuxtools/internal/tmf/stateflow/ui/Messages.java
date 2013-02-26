package org.eclipse.linuxtools.internal.tmf.stateflow.ui;

import org.eclipse.osgi.util.NLS;

@SuppressWarnings("javadoc")
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.linuxtools.internal.tmf.stateflow.ui.messages"; //$NON-NLS-1$

    public static String ExecutionFlowView_birthTimeColumn;
    public static String ExecutionFlowView_contextColumn;
    public static String ExecutionFlowView_nameColumn;
    public static String ExecutionFlowView_scopeColumn;
    public static String ExecutionFlowView_traceColumn;

    public static String ExecutionFlowView_stateTypeName;
    public static String ExecutionFlowView_nextProcessActionNameText;
    public static String ExecutionFlowView_nextProcessActionToolTipText;
    public static String ExecutionFlowView_previousProcessActionNameText;
    public static String ExecutionFlowView_previousProcessActionToolTipText;

    public static String ExecutionFlowView_attributeSyscallName;
    public static String ExecutionFlowView_attributeCpuName;

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
