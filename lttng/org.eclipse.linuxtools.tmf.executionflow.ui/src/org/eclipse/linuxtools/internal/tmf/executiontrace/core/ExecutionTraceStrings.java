/*******************************************************************************
 * Copyright (c) 2012 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 ******************************************************************************/

package org.eclipse.linuxtools.internal.tmf.executiontrace.core;

/**
 * This file defines all the known event and field names for the generic 
 * execution tracer
 *
 * Once again, these should not be externalized, since they need to match
 * exactly what the tracer outputs. If you want to localize them in a view, you
 * should do a mapping in the viewer itself.
 *
 * @author aspear 
 * @note copied from code by alexmont
 *
 */
@SuppressWarnings({"javadoc", "nls"})
public abstract class ExecutionTraceStrings {

    /* Event names */
    public static final String FUNCTION_EVENT_PREFIX = "lttng_ust_java:function_";
    public static final String FUNCTION_ENTRY_EVENT = "lttng_ust_java:function_entry_event";
    public static final String FUNCTION_EXIT_EVENT = "lttng_ust_java:function_exit_event";

    /* Field names */
    public static final String TID_FIELD = "int_tid";   
    public static final String CLASS_FIELD = "string_class";
    public static final String METHOD_FIELD = "string_method";
    public static final String LINE_NUMBER_FIELD = "int_lineno";  
}
