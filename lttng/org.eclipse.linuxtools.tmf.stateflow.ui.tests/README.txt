This README file documents instructions on how to get started with the stateflow plugin and its example sources.

Written by: Aaron Spear (aaron@ontherock.com)

At the time of writing, April 11 2013, the sources live in github.  Below are step by step instructions on 

1) clone the repo at https://github.com/aspear/linuxtools .  There is a "state-system-extensions" branch which is what you want

2) create a new eclipse workspace for this work.  I am currently using the latest Juno PDE development environment with the following projects in the workspace it:
/org.eclipse.linuxtools.ctf
/org.eclipse.linuxtools.ctf.core
/org.eclipse.linuxtools.ctf.core.tests
/org.eclipse.linuxtools.ctf.parser
/org.eclipse.linuxtools.lttng.help
/org.eclipse.linuxtools.lttng.releng-site
/org.eclipse.linuxtools.lttng2
/org.eclipse.linuxtools.lttng2.core
/org.eclipse.linuxtools.lttng2.core.tests
/org.eclipse.linuxtools.lttng2.kernel
/org.eclipse.linuxtools.lttng2.kernel.core
/org.eclipse.linuxtools.lttng2.kernel.core.tests
/org.eclipse.linuxtools.lttng2.kernel.ui
/org.eclipse.linuxtools.lttng2.kernel.ui.tests
/org.eclipse.linuxtools.lttng2.ui
/org.eclipse.linuxtools.lttng2.ui.tests
/org.eclipse.linuxtools.tmf
/org.eclipse.linuxtools.tmf.core
/org.eclipse.linuxtools.tmf.core.tests
/org.eclipse.linuxtools.tmf.stateflow.ui
/org.eclipse.linuxtools.tmf.ui
/org.eclipse.linuxtools.tmf.ui.tests

3) you should get a clean build and be able to launch the workbench

4) The org.eclipse.linuxtools.tmf.stateflow.ui.tests project contains sample traces:

- for a prototype "execution trace" (which is a quick and dirty implementation of a Java function tracer built on CTF-UST and the InTrace open source project)
- simple text log file that implements the state of "virtual machines" over time.  

5) importing the execution trace: you should be able to directly import and visualize without any changes, because there is an extension point that contributes a new trace type for it (see the screenshots/execution-trace-import.png on how to import this.  Import 
org.eclipse.linuxtools.tmf.stateflow.ui.tests/tracesets/sample-java-trace/ust/java-sequence-single-thread .  Note btw that the tracesets/sample-java-trace/ust/java-sequence-single-thread.state-schema.xml is left in the directory for reference.  You do not actually need this file for the execution tracer, because there is a derived trace that does the location of the XML schema file in code in the plugin.

6) importing the VM log file: 
You must first add a text parser to your runtime workspace for the log file format.  I included mine in tracesets/sample-state-trace/custom_txt_parsers.xml.  You must shutdown your running copy and then copy this file into <runtime workspace>/.metadata/.plugins/org.eclipse.linuxtools.tmf.ui/custom_txt_parsers.xml.  Note that I am planning on getting a more graceful way to do this setup, but haven't done it yet.  (I have larger plans for more scalable text base trace support, that is I plan on being able to have a text trace that is dynamically appended to)
