/*
 * @(#)file      ObjectInstance.java
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

// java import
import java.io.Serializable;

// RI import
import javax.management.ObjectName;


/**
 * Used to represent the object name of an MBean and its class name.
 * If the MBean is a Dynamic MBean the class name should be retrieved from the <CODE>MBeanInfo</CODE> it provides.
 *
 * @since-jdkbundle 1.5
 */
public class ObjectInstance implements Serializable   { 


    /* Serial version */
    private static final long serialVersionUID = -4099952623687795850L;

    /**
     * @serial Object name.
     */
    private ObjectName name;
    
    /**
     * @serial Class name.
     */
    private String className;
    
    /**
     * Allows an object instance to be created given a string representation of
     * an object name and the full class name, including the package name.
     *
     * @param objectName  A string representation of the object name.
     * @param className The full class name, including the package
     * name, of the object instance.  If the MBean is a Dynamic MBean
     * the class name corresponds to its {@link
     * DynamicMBean#getMBeanInfo()
     * getMBeanInfo()}<code>.getClassName()</code>.
     *
     * @exception MalformedObjectNameException The string passed as a
     * parameter does not have the right format.
     *
     */        
    public ObjectInstance(String objectName, String className)
	    throws MalformedObjectNameException { 	
	this(new ObjectName(objectName), className);
    } 

    /**
     * Allows an object instance to be created given an object name and
     * the full class name, including the package name.
     *
     * @param objectName  The object name.     
     * @param className  The full class name, including the package
     * name, of the object instance.  If the MBean is a Dynamic MBean
     * the class name corresponds to its {@link
     * DynamicMBean#getMBeanInfo()
     * getMBeanInfo()}<code>.getClassName()</code>.
     * If the MBean is a Dynamic MBean the class name should be retrieved from the <CODE>MBeanInfo</CODE> it provides.
     *
     */
    public ObjectInstance(ObjectName objectName, String className) { 
	if (objectName.isPattern()) {
	    new RuntimeOperationsException(new IllegalArgumentException("Invalid name->"+ objectName.toString()));
	}   
	this.name= objectName;
	this.className= className;
    } 

    
    /**
     * Compares the current object instance with another object instance.
     *
     * @param object  The object instance that the current object instance is to be compared
     * with.
     *
     * @return  True if the two object instances are equal, otherwise false.
     */
    public boolean equals(Object object)  { 
	if (!(object instanceof ObjectInstance)) {
	    return false;
	}    
	ObjectInstance val = (ObjectInstance) object;
	return ((name.equals(val.getObjectName())) && 
		(className.equals(val.getClassName())));
    } 

    public int hashCode() {
	return name.hashCode() ^ className.hashCode();
    }

    /**
     * Returns the object name part.
     *
     * @return the object name.
     */
    public ObjectName getObjectName()  { 
	return name;
    } 
    
    /**
     * Returns the class part.
     *
     * @return the class name.
     */
    public String getClassName()  { 
	return className;
    } 

 }