/*
 * @(#)file      RuntimeMBeanException.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   4.18
 * @(#)lastedit      03/07/15
 *
 * Copyright 2000-2003 Sun Microsystems, Inc.  All rights reserved.
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 * 
 * Copyright 2000-2003 Sun Microsystems, Inc.  Tous droits r�serv�s.
 * Ce logiciel est propriet� de Sun Microsystems, Inc.
 * Distribu� par des licences qui en restreignent l'utilisation. 
 */

package javax.management; 


/**
 * Represents runtime exceptions thrown by MBean methods in
 * the agent. It "wraps" the actual <CODE>java.lang.RuntimeException</CODE> exception thrown.
 * This exception will be built by the MBeanServer when a call to an
 * MBean method throws a runtime exception.
 *
 * @since-jdkbundle 1.5
 */
public class RuntimeMBeanException extends JMRuntimeException   { 
    
    /* Serial version */
    private static final long serialVersionUID = 5274912751982730171L;

    /**
     * @serial The encapsulated {@link RuntimeException}
     */
    private java.lang.RuntimeException runtimeException ;
    

    /**
     * Creates a <CODE>RuntimeMBeanException</CODE> that wraps the actual <CODE>java.lang.RuntimeException</CODE>.
     *
     * @param e the wrapped exception.
     */   
    public RuntimeMBeanException(java.lang.RuntimeException e) {
	super() ;
	runtimeException = e ;
    } 
    
    /**
     * Creates a <CODE>RuntimeMBeanException</CODE> that wraps the actual <CODE>java.lang.RuntimeException</CODE> with
     * a detailed message.
     *
     * @param e the wrapped exception.
     * @param message the detail message.
     */   
    public RuntimeMBeanException(java.lang.RuntimeException e, String message) { 
	super(message) ;
	runtimeException = e ;
    } 
    
    /**
     * Returns the actual {@link RuntimeException} thrown.
     *
     * @return the wrapped {@link RuntimeException}.
     */
    public java.lang.RuntimeException getTargetException()  { 
	return runtimeException ;
    } 

    /**
     * Returns the actual {@link RuntimeException} thrown.
     *
     * @return the wrapped {@link RuntimeException}.
     */
    public Throwable getCause() {
	return runtimeException;
    }
}
