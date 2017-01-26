/*
 * @(#)file      BooleanValueExp.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   4.15
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
 * This class represents a boolean value. A BooleanValueExp may be
 * used anywhere a ValueExp is required.
 * @serial include
 *
 * @since-jdkbundle 1.5
 */
class BooleanValueExp extends QueryEval implements ValueExp { 

    /* Serial version */
    private static final long serialVersionUID = 7754922052666594581L;

    /** 
     * @serial The boolean value 
     */
    private boolean val = false;


    /** Creates a new BooleanValueExp representing the boolean literal <val>.*/
    BooleanValueExp(boolean val) {
	this.val = val;
    }
  
    /**Creates a new BooleanValueExp representing the Boolean object <val>.*/    
    BooleanValueExp(Boolean val) {
	this.val = val.booleanValue();
    }


    /** Returns the  Boolean object representing the value of the BooleanValueExp object.*/
    public Boolean getValue()  { 
	return new Boolean(val);
    } 

    /**
     * Returns the string representing the object.
     */   
    public String toString()  { 
	return String.valueOf(val);
    }     

    /**
     * Applies the ValueExp on a MBean.
     *
     * @param name The name of the MBean on which the ValueExp will be applied.
     *
     * @return  The <CODE>ValueExp</CODE>.
     *
     * @exception BadStringOperationException
     * @exception BadBinaryOpValueExpException
     * @exception BadAttributeValueExpException 
     * @exception InvalidApplicationException
     */   
    public ValueExp apply(ObjectName name) throws BadStringOperationException, BadBinaryOpValueExpException,
	BadAttributeValueExpException, InvalidApplicationException  { 
	return this;
    } 

 }
