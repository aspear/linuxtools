/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.internal.perf.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.linuxtools.internal.perf.PerfPlugin;
import org.eclipse.linuxtools.internal.perf.ReportComparisonData;
import org.eclipse.linuxtools.internal.perf.SourceDisassemblyData;
import org.eclipse.linuxtools.internal.perf.StatData;
import org.eclipse.linuxtools.internal.perf.handlers.PerfStatDiffMenuAction;
import org.eclipse.linuxtools.internal.perf.handlers.PerfStatDiffMenuAction.PerfCachedData;
import org.eclipse.linuxtools.internal.perf.handlers.PerfStatDiffMenuAction.Type;
import org.junit.Test;

public class DataManipulatorTest {

	@Test
	public void testEchoSourceDisassemblyData() {
		final IPath path = new Path("/a/b/c/"); //$NON-NLS-1$

		StubSourceDisassemblyData sdData = new StubSourceDisassemblyData(
				"disassembly data", path); //$NON-NLS-1$
		sdData.parse();

		String expected = "perf annotate -i " + path.toOSString() + "perf.data"; //$NON-NLS-1$

		assertEquals(expected, sdData.getPerfData().trim());
	}
	@Test
	public void testEchoStatData() {
		final String binary = "a/b/c.out";
		final String[] args = new String[] { "arg1", "arg2", "arg3" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		final int runCount = 3;

		StubStatData sData = new StubStatData(
				"stat data", binary, args, runCount, null); //$NON-NLS-1$
		sData.parse();

		String expected = "perf stat -r " + runCount + " " + binary; //$NON-NLS-1$
		for (int i = 0; i < args.length; i++) {
			expected += " " + args[i]; //$NON-NLS-1$
		}

		assertEquals(expected, sData.getPerfData().trim());
	}
	@Test
	public void testEchoStatDataEvents() {
		final String binary = "a/b/c.out";
		final String[] args = new String[] { "arg1", "arg2", "arg3" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		final String[] events = new String[] { "event1", "event2", "event3" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		final int runCount = 3;

		StubStatData sData = new StubStatData(
				"stat data", binary, args, runCount, events); //$NON-NLS-1$
		sData.parse();

		String expected = "perf stat -r " + runCount; //$NON-NLS-1$
		for(String event : events){
			expected += " -e " + event; //$NON-NLS-1$
		}

		expected = expected + " " + binary; //$NON-NLS-1$
		for (int i = 0; i < args.length; i++) {
			expected += " " + args[i]; //$NON-NLS-1$
		}

		assertEquals(expected, sData.getPerfData().trim());
	}

	@Test
	public void testEchoReportDiffData() {
		File oldData = new File("perf.old.data"); //$NON-NLS-1$
		File newData = new File("perf.data"); //$NON-NLS-1$
		StubReportDiffData diffData = new StubReportDiffData("title", //$NON-NLS-1$
				oldData, newData);
		diffData.parse();

		String expected = "perf diff " + oldData.getAbsolutePath()  //$NON-NLS-1$
				+ " " + newData.getAbsolutePath();  //$NON-NLS-1$

		assertEquals(expected, diffData.getPerfData().trim());
	}

	@Test
	public void testPerfDataFile() {
		String dataTitle = "title";
		String dataID = "id";
		String data = "perf stat data stub file\n";

		PerfStatDiffMenuAction action = new PerfStatDiffMenuAction(Type.PERF_DIFF, "0");
		PerfCachedData dataFile = action.new PerfCachedData(dataID, dataTitle);

		// put test data on cache
		PerfPlugin.getDefault().cacheData("id", data);

		assertEquals("title", dataFile.getTitle());
		assertEquals(data, dataFile.getPerfData());

		// remove test data from cache
		PerfPlugin.getDefault().removeCachedData("id");
	}

	/**
	 * Used for testing SourceDisassemblyData
	 */
	private class StubSourceDisassemblyData extends SourceDisassemblyData {

		public StubSourceDisassemblyData(String title, IPath workingDir) {
			super(title, workingDir);
		}

		@Override
		public String[] getCommand(String workingDir) {
			List<String> ret = new ArrayList<String>();
			// return the same command with 'echo' prepended
			ret.add("echo"); //$NON-NLS-1$
			ret.addAll(Arrays.asList(super.getCommand(workingDir)));
			return ret.toArray(new String[0]);
		}
	}

	/**
	 * Used for testing StatData
	 */
	private class StubStatData extends StatData {

		public StubStatData(String title, String cmd, String[] args,
				int runCount, String[] events) {
			super(title, null, cmd, args, runCount, events);
		}

		@Override
		public String[] getCommand(String command, String[] args) {
			// return the same command with 'echo' prepended
			List<String> ret = new ArrayList<String>();
			ret.add("echo"); //$NON-NLS-1$
			ret.addAll(Arrays.asList(super.getCommand(command, args)));
			return ret.toArray(new String[0]);
		}

		@Override
		public void parse() {
			String[] cmd = getCommand(getProgram(), getArguments());
			// echo will print to standard out
			performCommand(cmd, 1);
		}
	}

	/**
	 * Used for testing ReportComparisonData
	 */
	private class StubReportDiffData extends ReportComparisonData{

		public StubReportDiffData(String title, File oldFile, File newFile) {
			super(title, oldFile, newFile);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected String[] getCommand() {
			// return the same command with 'echo' prepended
			List<String> ret = new ArrayList<String>();
			ret.add("echo"); //$NON-NLS-1$
			ret.addAll(Arrays.asList(super.getCommand()));
			return ret.toArray(new String[0]);
		}

	}

}
