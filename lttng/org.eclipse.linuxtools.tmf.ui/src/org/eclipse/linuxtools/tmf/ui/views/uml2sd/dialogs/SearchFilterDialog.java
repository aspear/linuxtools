/**********************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * Copyright (c) 2011, 2012 Ericsson.
 * 
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 * Bernd Hufmann - Updated for TMF
 **********************************************************************/
package org.eclipse.linuxtools.tmf.ui.views.uml2sd.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.linuxtools.internal.tmf.ui.TmfUiPlugin;
import org.eclipse.linuxtools.tmf.ui.views.uml2sd.SDView;
import org.eclipse.linuxtools.tmf.ui.views.uml2sd.core.AsyncMessage;
import org.eclipse.linuxtools.tmf.ui.views.uml2sd.core.AsyncMessageReturn;
import org.eclipse.linuxtools.tmf.ui.views.uml2sd.core.GraphNode;
import org.eclipse.linuxtools.tmf.ui.views.uml2sd.core.Lifeline;
import org.eclipse.linuxtools.tmf.ui.views.uml2sd.core.Stop;
import org.eclipse.linuxtools.tmf.ui.views.uml2sd.core.SyncMessage;
import org.eclipse.linuxtools.tmf.ui.views.uml2sd.core.SyncMessageReturn;
import org.eclipse.linuxtools.tmf.ui.views.uml2sd.handlers.provider.ISDFindProvider;
import org.eclipse.linuxtools.tmf.ui.views.uml2sd.handlers.provider.ISDGraphNodeSupporter;
import org.eclipse.linuxtools.tmf.ui.views.uml2sd.util.SDMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;

/**
 * This is the common dialog to define Find and/or Filter Criteria(s)
 */
public class SearchFilterDialog extends Dialog {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------
    /**
     * The find criteria property name
     */
    protected static final String FIND_CRITERIA = "findCriteria"; //$NON-NLS-1$
    /**
     * The find expression list property name
     */
    protected static final String FIND_EXPRESSION_LIST = "findExpressionList"; //$NON-NLS-1$
    /**
     * The filter criteria poperty name 
     */
    protected static final String FILTER_CRITERIA = "filterCriteria"; //$NON-NLS-1$
    /**
     * The filter expression list property name
     */
    protected static final String FILTER_EXPRESSION_LIST = "filterExpressionList"; //$NON-NLS-1$
    /**
     * The maximum number of expressions stored.
     */
    protected static final int MAX_EXPRESSION_LIST = 7;

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * The sequence diagram view reference.
     */
    protected SDView fSdView = null;

    /**
     * The tab with the controls for a Criteria
     */
    protected TabFolder fTabFolder = null;

    /**
     * The Criteria updated by this dialog
     */
    protected Criteria fCriteria = null;

    /**
     * The find/filter provider telling which graph nodes are supported
     */
    protected ISDGraphNodeSupporter fProvider = null;

    /**
     * The okText is the text for the Ok button and title is the title of the dialog.<br>
     * Both depend (okText and title (below)) on the usage that is done of this dialog 
     * (find or filter).
     */
    protected String fOkText;
    
    /**
     * The title is the title of the dialog.<br>
     * Both depend (okText and title) on the usage that is done of this dialog 
     * (find or filter).
     */
    protected String fTitle;

    /**
     * List of string expressions that have been searched already
     */
    protected String[] fExpressionList;

    /**
     * find is true if the dialog is for the find feature and false for filter feature
     */
    protected boolean fIsFind;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------
    /**
     * Standard constructor
     * 
     * @param view A sequence diagram view reference
     * @param provider A graph node supporter provider
     * @param filter A flag to indicate filtering (true) or finding (false)  
     * @param style Style bits
     */
    public SearchFilterDialog(SDView view, ISDGraphNodeSupporter provider, boolean filter, int style) {
        super(view.getSDWidget().getShell());
        setShellStyle(SWT.DIALOG_TRIM | style);
        fProvider = provider;
        fSdView = view;
        fIsFind = !filter;
    }
    
    // ------------------------------------------------------------------------
    // Methods
    // ------------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public Control createDialogArea(Composite arg0) {
        if (fIsFind) {
            fExpressionList = TmfUiPlugin.getDefault().getDialogSettings().getArray(FIND_EXPRESSION_LIST);
        } else {
            fExpressionList = TmfUiPlugin.getDefault().getDialogSettings().getArray(FILTER_EXPRESSION_LIST);
        }
        if (fExpressionList == null) {
            fExpressionList = new String[0];
        }
        return new TabContents(arg0, fProvider, getButton(IDialogConstants.OK_ID), fExpressionList);
    }

    /**
     * Open the dialog box
     */
    @Override
    public int open() {
        create();

        if (fCriteria == null) {
            loadCriteria();
        }

        if (fCriteria == null) {
            fCriteria = new Criteria();
            fCriteria.setLifeLineSelected(fProvider.isNodeSupported(ISDGraphNodeSupporter.LIFELINE));
            fCriteria.setSyncMessageSelected(fProvider.isNodeSupported(ISDGraphNodeSupporter.SYNCMESSAGE));
            fCriteria.setSyncMessageReturnSelected(fProvider.isNodeSupported(ISDGraphNodeSupporter.SYNCMESSAGERETURN));
            fCriteria.setAsyncMessageSelected(fProvider.isNodeSupported(ISDGraphNodeSupporter.ASYNCMESSAGE));
            fCriteria.setAsyncMessageReturnSelected(fProvider.isNodeSupported(ISDGraphNodeSupporter.ASYNCMESSAGERETURN));
            fCriteria.setStopSelected(fProvider.isNodeSupported(ISDGraphNodeSupporter.STOP));
        }
        copyFromCriteria(fCriteria);

        if (fOkText != null) {
            getButton(IDialogConstants.OK_ID).setText(fOkText);
        } else {
            getButton(IDialogConstants.OK_ID).setText(SDMessages._21);
        }

        if (fIsFind) {
            getButton(IDialogConstants.CANCEL_ID).setText(SDMessages._22);
        }

        Button okButton = getButton(IDialogConstants.OK_ID);
        ((TabContents) getDialogArea()).setOkButton(okButton);
        if (fCriteria == null || !((fCriteria.getExpression() != null && !fCriteria.getExpression().equals("")) && //$NON-NLS-1$
                (fCriteria.isAsyncMessageReturnSelected() || fCriteria.isAsyncMessageSelected() || fCriteria.isLifeLineSelected() || fCriteria.isStopSelected() || fCriteria.isSyncMessageReturnSelected() || fCriteria.isSyncMessageSelected()))) {
            okButton.setEnabled(false);
        }

        if (fTitle != null) {
            getShell().setText(fTitle);
        } else {
            getShell().setText(SDMessages._24);
        }

        getShell().pack();
        getShell().setLocation(getShell().getDisplay().getCursorLocation());

        fCriteria = null;
        return super.open();
    }

    /**
	 * Loads criteria from the dialog settings which are saved in the workspace.
	 */
    @SuppressWarnings("rawtypes")
    protected void loadCriteria() {

        String CRITERIA = FIND_CRITERIA;
        if (!fIsFind) {
            CRITERIA = FILTER_CRITERIA;
        }

        DialogSettings section = (DialogSettings) TmfUiPlugin.getDefault().getDialogSettings().getSection(CRITERIA);
        List selection = fSdView.getSDWidget().getSelection();
        if ((selection == null || selection.size() != 1) || (!fIsFind)) {
            if (section != null) {
                fCriteria = new Criteria();
                fCriteria.load(section);
            }
        } else {
            GraphNode gn = (GraphNode) selection.get(0);
            fCriteria = new Criteria();
            fCriteria.setExpression(gn.getName());
            fCriteria.setCaseSenstiveSelected(true);
            if (gn instanceof Lifeline && fProvider.isNodeSupported(ISDGraphNodeSupporter.LIFELINE)) {
                fCriteria.setLifeLineSelected(true);
            } else if (gn instanceof SyncMessageReturn && fProvider.isNodeSupported(ISDGraphNodeSupporter.SYNCMESSAGERETURN)) {
                fCriteria.setSyncMessageReturnSelected(true);
            } else if ((gn instanceof SyncMessageReturn || gn instanceof SyncMessage) && fProvider.isNodeSupported(ISDGraphNodeSupporter.SYNCMESSAGE)) {
                fCriteria.setSyncMessageSelected(true);
            } else if (gn instanceof AsyncMessageReturn && fProvider.isNodeSupported(ISDGraphNodeSupporter.ASYNCMESSAGERETURN)) {
                fCriteria.setAsyncMessageReturnSelected(true);
            } else if ((gn instanceof AsyncMessageReturn || gn instanceof AsyncMessage) && fProvider.isNodeSupported(ISDGraphNodeSupporter.ASYNCMESSAGE)) {
                fCriteria.setAsyncMessageSelected(true);
            } else if (gn instanceof Stop && fProvider.isNodeSupported(ISDGraphNodeSupporter.STOP)) {
                fCriteria.setStopSelected(true);
            }
        }
    }

    /**
     * Called when the dialog box ok button is pressed and calls back 
     * the appropriate action provider (ISDFilterProvider or ISDFindProvider).
     * 
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    public void okPressed() {
        copyToCriteria();
        if (!fIsFind) {
            saveCriteria();
            super.close(); // Filter is modal
        }
        if ((fProvider != null) && (fProvider instanceof ISDFindProvider) && fIsFind) {
            boolean result = ((ISDFindProvider) fProvider).find(fCriteria);
            TabContents content = getTabContents();
            content.setResult(result);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#cancelPressed()
     */
    @Override
    public void cancelPressed() {
        if (fIsFind) {
            copyToCriteria();
            if (fProvider instanceof ISDFindProvider) {
                ((ISDFindProvider) fProvider).cancel();
            }
            saveCriteria();
        }
        super.cancelPressed();
    }

    /**
	 * Saves the criteria to the dialog settings within the workspace.
	 */
    public void saveCriteria() {
        String CRITERIA = FIND_CRITERIA;
        String EXPRESSION_LIST = FIND_EXPRESSION_LIST;
        if (!fIsFind) {
            CRITERIA = FILTER_CRITERIA;
            EXPRESSION_LIST = FILTER_EXPRESSION_LIST;
        }
        DialogSettings settings = (DialogSettings) TmfUiPlugin.getDefault().getDialogSettings();
        DialogSettings section = (DialogSettings) settings.getSection(CRITERIA);
        if (section == null) {
            section = (DialogSettings) settings.addNewSection(CRITERIA);
        }
        fCriteria.save(section);

        if (fCriteria.getExpression().length() > 0) {
            ArrayList<String> list = new ArrayList<String>();
            for (int i = 0; i < fExpressionList.length; i++) {
                list.add(fExpressionList[i]);
            }
            // Remove the used expression if one from the dropdown list
            list.remove(fCriteria.getExpression());
            // Put the new expression at the beginning
            list.add(0, fCriteria.getExpression());
            // Fill in the expressionList, truncating to MAX_EXPRESSION_LIST
            int size = Math.min(list.size(), MAX_EXPRESSION_LIST);
            String[] temp = new String[size];
            for (int i = 0; i < size; i++) {
                temp[i] = (String) list.get(i);
            }
            fExpressionList = temp;
            settings.put(EXPRESSION_LIST, fExpressionList);
        }
    }

    /**
	 * Returns the criteria
	 * 
	 * @return the criteria
	 */
    public Criteria getCriteria() {
        return fCriteria;
    }

    /**
     * Sets the criteria.
     * 
     * @param criteria the criteria to set.
     */
    public void setCriteria(Criteria criteria) {
        fCriteria = criteria;
    }

    /**
     * Get the current end-user settings from the dialog to a Criteria
     */
    public void copyToCriteria() {
        fCriteria = new Criteria();
        TabContents content = getTabContents();
        fCriteria.setLifeLineSelected(content.isLifelineButtonSelected());
        fCriteria.setSyncMessageSelected(content.isSynMessageButtonSelected());
        fCriteria.setSyncMessageReturnSelected(content.isSynMessageReturnButtonSelected());
        fCriteria.setAsyncMessageSelected(content.isAsynMessageButtonSelected());
        fCriteria.setAsyncMessageReturnSelected(content.isAsynMessageReturnButtonSelected());
        fCriteria.setStopSelected(content.isStopButtonSelected());
        fCriteria.setCaseSenstiveSelected(content.isCaseSensitiveSelected());
        fCriteria.setExpression(content.getSearchText());
    }

    /**
     * Returns the tab content reference.
     * 
     * @return the tab content
     */
    protected TabContents getTabContents() {
        TabContents content = null;
        if (fTabFolder == null) {
            content = (TabContents) getDialogArea();
        } else {
            content = (TabContents) fTabFolder.getSelection()[0].getControl();
        }
        return content;
    }

    /**
     * Initialize the dialog with the settings of an existing Criteria<br>
     * Criteria must not be null and the TabContents must have been created
     * 
     * @param from the criteria to copy from
     */
    public void copyFromCriteria(Criteria from) {
        TabContents content = getTabContents();
        content.setLifelineButtonSelection(from.isLifeLineSelected());
        content.setSynMessageButtonSelection(from.isSyncMessageSelected());
        content.setSynMessageReturnButtonSelection(from.isSyncMessageReturnSelected());
        content.setAsynMessageButtonSelection(from.isAsyncMessageSelected());
        content.setAsynMessageReturnButtonSelection(from.isSyncMessageReturnSelected());
        content.setStopButtonSelection(from.isStopSelected());
        content.setCaseSensitiveSelection(from.isCaseSenstiveSelected());
        if (from.getExpression() != null) {
            content.setSearchText(from.getExpression());
        }
    }

    /**
     * Sets the text to be used for the ok button
     * 
     * @param okText text to set
     */
    public void setOkText(String okText) {
        fOkText = okText;
    }

    /**
     * Sets the title to be used for the dialog box.
     * 
     * @param title The title to set
     */
    public void setTitle(String title) {
        fTitle = title;
    }
}