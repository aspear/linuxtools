/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Jeff Briggs, Henry Hughes, Ryan Morse
 *******************************************************************************/

package org.eclipse.linuxtools.internal.systemtap.ui.graphing.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.linuxtools.internal.systemtap.ui.graphing.Localization;
import org.eclipse.linuxtools.systemtap.graphingapi.ui.charts.AbstractChartBuilder;
import org.eclipse.linuxtools.systemtap.graphingapi.ui.widgets.ExceptionErrorDialog;
import org.eclipse.linuxtools.systemtap.structures.listeners.ITabListener;
import org.eclipse.linuxtools.systemtap.ui.graphing.GraphDisplaySet;
import org.eclipse.linuxtools.systemtap.ui.graphing.views.GraphSelectorEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

/**
 * This action is designed to allow for saving of the graph in the active window.
 * It will let the user select the location to save the image, and then save it as
 * a jpg image.
 * @author Ryan Morse
 */
public class SaveGraphImageAction extends Action implements IWorkbenchWindowActionDelegate {
	@Override
	public void init(IWorkbenchWindow window) {
		fWindow = window;
	}

	/**
	 * This is the main method of the action.  It handles getting the active graph,
	 * prompting the user for a location to save the image to, and then actually doing
	 * the save.
	 * @param act The action that fired this method.
	 */
	@Override
	public void run(IAction act) {
		AbstractChartBuilder g = getGraph();
		try {
			PlatformUI.getWorkbench().getDisplay().update();
		} catch(SWTException swte) {
			ExceptionErrorDialog.openError(Localization.getString("SaveGraphImageAction.UnableToSaveGraph"), swte); //$NON-NLS-1$
		}
		if(null == g) {
			displayError(Localization.getString("SaveGraphImageAction.CanNotGetGraph")); //$NON-NLS-1$
			return;
		}

		ImageData image = getImage(g);
		if(null == image) {
			displayError(Localization.getString("SaveGraphImageAction.CanNotCreateImage")); //$NON-NLS-1$
			return;
		}

		String path = getFile();
		if(null == path) {
			return;
		}

		save(image, path);
	}

	/**
	 * This method retreives the active graph from the GraphSelectorView.  If no
	 * graph is active it will return null.
	 * @return The IGraph in tha active display set.
	 */
	public AbstractChartBuilder getGraph() {
		IViewPart ivp = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(GraphSelectorEditor.ID);
		AbstractChartBuilder g = null;

		GraphDisplaySet gds = ((GraphSelectorEditor)ivp).getActiveDisplaySet();
		if(null != gds) {
			g = gds.getActiveGraph();
		}
		return g;
	}

	/**
	 * This method converts the Graph into an actual image.
	 * @param canvas The graph that needs to be converted to an image
	 * @return The Image that was generated by the supplied Graph.
	 */
	public ImageData getImage(AbstractChartBuilder canvas) {
		GC gc = new GC(canvas);
		Image image = new Image(canvas.getDisplay(), canvas.getSize().x, canvas.getSize().y);
		gc.copyArea(image, 0, 0);
		gc.dispose();

		ImageData data = image.getImageData();
		image.dispose();
		return data;
	}

	/**
	 * This method will display a dialog box for the user to select a
	 * location to save the graph image.
	 * @return The String location selected to save the image to.
	 */
	public String getFile() {
		FileDialog dialog= new FileDialog(fWindow.getShell(), SWT.SAVE);
		dialog.setText(Localization.getString("SaveGraphImageAction.NewFile")); //$NON-NLS-1$
		return dialog.open();
	}

	/**
	 * This method will perform the save operation to store the generated
	 * image as an image file on the computer
	 * @param image The image data generated from the graph
	 * @param path The location to create the new file in and save to.
	 */
	public void save(ImageData image, String path) {
		ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] {image};
		loader.save(path, SWT.IMAGE_JPEG);
	}

	/**
	 * This method will display the error message to the user in the case
	 * that something went wrong.
	 * @param message The message that should be shown in the error dialog.
	 */
	private void displayError(String message) {
		MessageDialog.openWarning(fWindow.getShell(), Localization.getString("SaveGraphImageAction.Problem"), message); //$NON-NLS-1$
	}

	/**
	 * This method is used to generate the checks to see it this button
	 * should be enabled or not.
	 */
	@Override
	public void selectionChanged(IAction a, ISelection s) {
		action = a;
		action.setEnabled(false);
		buildEnablementChecks();
	}

	/**
	 * This method is used to generate the checks to see it this button
	 * should be enabled or not.
	 */
	private void buildEnablementChecks() {
		IViewPart ivp = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(GraphSelectorEditor.ID);
		if(null != ivp) {
			final GraphSelectorEditor gsv = (GraphSelectorEditor)ivp;
			gsv.addTabListener(new ITabListener() {
				@Override
				public void tabClosed() {
					if(null == gsv.getActiveDisplaySet() || null == gsv.getActiveDisplaySet().getActiveGraph()) {
						action.setEnabled(false);
					}
				}

				@Override
				public void tabOpened() {
					gsv.getActiveDisplaySet().addTabListener(new ITabListener() {
						@Override
						public void tabClosed() {
							if(null == gsv.getActiveDisplaySet().getActiveGraph()) {
								action.setEnabled(false);
							}
						}

						@Override
						public void tabOpened() {
							if(null != gsv.getActiveDisplaySet().getActiveGraph()) {
								action.setEnabled(true);
							}
						}

						@Override
						public void tabChanged() {
							if(null == gsv.getActiveDisplaySet() || null == gsv.getActiveDisplaySet().getActiveGraph()) {
								action.setEnabled(false);
							} else {
								action.setEnabled(true);
							}
						}
					});
				}

				@Override
				public void tabChanged() {
					if(null == gsv.getActiveDisplaySet() || null == gsv.getActiveDisplaySet().getActiveGraph()) {
						action.setEnabled(false);
					} else {
						action.setEnabled(true);
					}
				}
			});
		}
	}

	/**
	 * Removes all internal references in this class.  Nothing should make any references
	 * to anyting in this class after calling the dispose method.
	 */
	@Override
	public void dispose() {
		fWindow = null;
		action = null;
	}

	private IWorkbenchWindow fWindow;
	private IAction action;
}
