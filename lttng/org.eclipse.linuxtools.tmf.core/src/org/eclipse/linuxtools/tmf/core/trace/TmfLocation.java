/*******************************************************************************
 * Copyright (c) 2009, 2010, 2012 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *   Francois Chouinard - Updated as per TMF Trace Model 1.0
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.trace;

import java.lang.reflect.Method;

/**
 * <b><u>TmfLocation</u></b>
 * <p>
 * A convenience implementation on of ITmfLocation. The generic class (L) must
 * be comparable.
 */
public class TmfLocation<L extends Comparable<L>> implements ITmfLocation<L> {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    static public final TmfLocation<Boolean> NULL_LOCATION = new TmfLocation<Boolean>();
    
    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private L fLocation;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Default constructor (for the 'null' location)
     */
    private TmfLocation() {
        fLocation = null;
    }

    /**
     * Standard constructor.
     * 
     * @param location the trace location
     */
    public TmfLocation(L location) {
        fLocation = location;
    }

    /**
     * Copy constructor
     * 
     * @param other the original location
     */
    public TmfLocation(TmfLocation<L> location) {
        fLocation = location.fLocation;
    }

    // ------------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------------

    /* (non-Javadoc)
     * @see org.eclipse.linuxtools.tmf.core.trace.ITmfLocation#getLocation()
     */
    @Override
    public L getLocation() {
        return fLocation;
    }

    // ------------------------------------------------------------------------
    // Cloneable
    // ------------------------------------------------------------------------

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    @SuppressWarnings("unchecked")
    public TmfLocation<L> clone() {
        TmfLocation<L> clone = null;
        try {
            clone = (TmfLocation<L>) super.clone();
            if (fLocation != null) {
                Class<?> clazz = fLocation.getClass();
                Method method = clazz.getMethod("clone", new Class[0]);
                Object copy = method.invoke(this.fLocation, new Object[0]);
                clone.fLocation = (L) copy;
            } else {
                clone.fLocation = null;
            }
        } catch (CloneNotSupportedException e) {
        } catch (NoSuchMethodException e) {
        } catch (Exception e) {
            throw new InternalError(e.toString());
        }
        return clone;
    }

    // ------------------------------------------------------------------------
    // Object
    // ------------------------------------------------------------------------

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fLocation != null) ? fLocation.hashCode() : 0);
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TmfLocation<L> other = (TmfLocation<L>) obj;
        if (fLocation == null) {
            if (other.fLocation != null)
                return false;
        } else if (!fLocation.equals(other.fLocation))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TmfLocation [fLocation=" + fLocation + "]";
    }

}
