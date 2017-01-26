/*
 * @(#)file      GaugeMonitor.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.64
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

package javax.management.monitor;


// java imports
//
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

// RI imports
//
import javax.management.ObjectName;
import javax.management.MBeanNotificationInfo;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

/**
 * Defines a monitor MBean designed to observe the values of a gauge attribute.
 * <P>
 * A gauge monitor observes an attribute that is continuously variable with time.
 * A gauge monitor sends notifications as follows:
 * <UL>
 * <LI> if the attribute value is increasing and becomes equal to or greater than the high threshold value,
 *      a {@link MonitorNotification#THRESHOLD_HIGH_VALUE_EXCEEDED threshold high notification} is sent.
 *      The notify high flag must be set to <CODE>true</CODE>
 *      <BR>Subsequent crossings of the high threshold value do not cause further notifications unless
 *      the attribute value becomes equal to or less than the low threshold value.
 * <LI> if the attribute value is decreasing and becomes equal to or less than the low threshold value,
 *      a {@link MonitorNotification#THRESHOLD_LOW_VALUE_EXCEEDED threshold low notification} is sent.
 *      The notify low flag must be set to <CODE>true</CODE>.
 *      <BR>Subsequent crossings of the low threshold value do not cause further notifications unless
 *      the attribute value becomes equal to or greater than the high threshold value.
 * </UL>
 * This provides a hysteresis mechanism to avoid repeated triggering of notifications when
 * the attribute value makes small oscillations around the high or low threshold value.
 * <P>
 * If the gauge difference mode is used, the value of the derived gauge is calculated
 * as the difference between the observed gauge values for two successive observations.
 * <BR>The derived gauge value (V[t]) is calculated using the following method:
 * <UL>
 * <LI>V[t] = gauge[t] - gauge[t-GP]
 * </UL>
 * This implementation of the gauge monitor requires the observed attribute to be of the type integer or floating-point
 * (<CODE>Byte</CODE>, <CODE>Integer</CODE>, <CODE>Short</CODE>, <CODE>Long</CODE>, <CODE>Float</CODE>, <CODE>Double</CODE>).
 *
 * @version     1.64     07/15/03
 * @author      Sun Microsystems, Inc
 *
 * @since-jdkbundle 1.5
 */
public class GaugeMonitor extends Monitor implements GaugeMonitorMBean {


    /*
     * ------------------------------------------
     *  PRIVATE VARIABLES
     * ------------------------------------------
     */

    /**
     * Gauge high threshold.
     * <BR>The default value is a null Integer object.
     */
    private Number highThreshold = new Integer(0);

    /**
     * Gauge low threshold.
     * <BR>The default value is a null Integer object.
     */
    private Number lowThreshold = new Integer(0);

    /**
     * Flag indicating if the gauge monitor notifies when exceeding the high threshold.
     * <BR>The default value is set to <CODE>false</CODE>.
     */
    private boolean notifyHigh = false;

    /**
     * Flag indicating if the gauge monitor notifies when exceeding the low threshold.
     * <BR>The default value is set to <CODE>false</CODE>.
     */
    private boolean notifyLow = false;

    /**
     * Flag indicating if the gauge difference mode is used.
     * If the gauge difference mode is used, the derived gauge is the difference between two consecutive observed values.
     * Otherwise, the derived gauge is directly the value of the observed attribute.
     * <BR>The default value is set to <CODE>false</CODE>.
     */
    private boolean differenceMode = false;

    /**
     * Derived gauges.
     * <BR>Each element in this array corresponds to an observed object in the list.
     */
    private transient Number derivedGauge[] = new Number[capacityIncrement];

    /**
     * Derived gauge timestamps.
     * <BR>Each element in this array corresponds to an observed object in the list.
     */
    private transient long derivedGaugeTimestamp[] = new long[capacityIncrement];

    /**
     * Scan gauge values captured by the previous observation.
     * <BR>Each element in this array corresponds to an observed object in the list.
     */
    private transient Number previousScanGauge[] = new Number[capacityIncrement];

    /**
     * This attribute is used to handle the hysteresis mechanism.
     * <BR>Each element in this array corresponds to an observed object in the list.
     */
    private transient int status[] = new int[capacityIncrement];

    /**
     * This attribute is used to keep the derived gauge type.
     * <BR>Each element in this array corresponds to an observed object in the list.
     */
    private transient int type[] = new int[capacityIncrement];

    // Flags needed to implement the hysteresis mechanism.
    //
    private static final int RISING             = 0;
    private static final int FALLING            = 1;
    private static final int RISING_OR_FALLING  = 2;

    // Flags needed to keep trace of the derived gauge type.
    // Integer + floating-point types are allowed.
    //
    private static final int INTEGER    = 0;
    private static final int BYTE       = 1;
    private static final int SHORT      = 2;
    private static final int LONG       = 3;
    private static final int FLOAT      = 4;
    private static final int DOUBLE     = 5;

    // New flags defining possible gauge monitor errors.
    // Flag denoting that a notification has occured after changing the high/low threshold.
    // This flag is used to check that the high/low threshold type is the same as the gauge and that the
    // threshold high value is greater than the threshold low value at the first notification time.
    //
    private static final int THRESHOLD_ERROR_NOTIFIED   = 16;

    /**
     * Timer.
     */
    private transient Timer timer = null;



    // TRACES & DEBUG
    //---------------

    String makeDebugTag() {
        return "GaugeMonitor";
    }

    /*
     * ------------------------------------------
     *  CONSTRUCTORS
     * ------------------------------------------
     */

    /**
     * Default constructor.
     */
    public GaugeMonitor() {
      dbgTag = makeDebugTag();
    }

    /*
     * ------------------------------------------
     *  PUBLIC METHODS
     * ------------------------------------------
     */

    /**
     * Starts the gauge monitor.
     */
    public void start() {

        if (isTraceOn()) {
            trace("start", "start the gauge monitor");
        }

	synchronized(this) {
	    if (isActive) {
		if (isTraceOn()) {
		    trace("start", "the gauge monitor is already activated");
		}

		return;
	    }

            isActive = true;

            // Reset values.
            //
            for (int i = 0; i < elementCount; i++) {
                status[i] = RISING_OR_FALLING;
                previousScanGauge[i] = null;
            }

            // Start the AlarmClock.
            //
            timer = new Timer();
            timer.schedule(new GaugeAlarmClock(this), getGranularityPeriod(), getGranularityPeriod());
            }
        }

    /**
     * Stops the gauge monitor.
     */
    /* This method is not synchronized, because if it were there could
       be a deadlock with a thread that attempted to get the lock on
       the monitor before being interrupted or noticing that it had
       been interrupted.  */
    public void stop() {
	trace("stop", "stop the gauge monitor");

	synchronized(this) {
	    if (!isActive) {
        if (isTraceOn()) {
		    trace("stop", "the counter monitor is already started");
        }

		return;
	    }

            isActive = false;

            // Stop the AlarmClock.
            //
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            }
        }

    // GETTERS AND SETTERS
    //--------------------
    
    /**
     * Sets the granularity period (in milliseconds).
     * <BR>The default value of the granularity period is 10 seconds.
     *
     * @param period The granularity period value.
     * @exception java.lang.IllegalArgumentException The granularity
     * period is less than or equal to zero.
     *
     * @see Monitor#setGranularityPeriod(long)
     */
    public void setGranularityPeriod(long period) throws java.lang.IllegalArgumentException { 
        synchronized(this) {
        super.setGranularityPeriod(period);
        
        // Reschedule timer task if timer is already running
        //
	    if (isActive()) {
          timer.cancel();
          timer = new Timer();
          timer.schedule(new GaugeAlarmClock(this), getGranularityPeriod(), getGranularityPeriod());
        }
    }
    }

    /**
     * Gets the derived gauge of the specified object, if this object is
     * contained in the set of observed MBeans, or <code>null</code> otherwise.
     *
     * @param object the name of the MBean.
     *
     * @return The derived gauge of the specified object.
     *
     * @since JMX 1.2
     */
    public Number getDerivedGauge(ObjectName object) { 
        synchronized(this) {
        int index = indexOf(object);
        if (index != -1) 
            return derivedGauge[index];
        else 
            return null;
    } 
    }
    
    /**
     * Gets the derived gauge timestamp of the specified object, if
     * this object is contained in the set of observed MBeans, or
     * <code>null</code> otherwise.
     *
     * @param object the name of the MBean.
     *
     * @return The derived gauge timestamp of the specified object.
     *
     * @since JMX 1.2
     */
    public long getDerivedGaugeTimeStamp(ObjectName object) { 
        synchronized(this) {
        int index = indexOf(object);
        if (index != -1) 
            return derivedGaugeTimestamp[index];
        else 
            return 0;
    } 
    }

    /**
     * Returns the derived gauge of the first object in the set of observed MBeans.
     *
     * @return The derived gauge.
     * @deprecated As of JMX 1.2, replaced by {@link #getDerivedGauge(ObjectName)}
     */
    public Number getDerivedGauge() {
        return derivedGauge[0];
    }

    /**
     * Gets the derived gauge timestamp of the first object in the set of observed MBeans.
     *
     * @return The derived gauge timestamp.
     * @deprecated As of JMX 1.2, replaced by {@link #getDerivedGaugeTimeStamp(ObjectName)}
     */
    public long getDerivedGaugeTimeStamp() {
        return derivedGaugeTimestamp[0];
    }

    /**
     * Gets the high threshold value common to all observed MBeans.
     *
     * @return The high threshold value.
     */
    public Number getHighThreshold() {
        return highThreshold;
    }

    /**
     * Gets the low threshold value common to all observed MBeans.
     *
     * @return The low threshold value.
     */
    public Number getLowThreshold() {
        return lowThreshold;
    }

    /**
     * Sets the high and the low threshold values common to all observed MBeans.
     *
     * @param highValue The high threshold value.
     * @param lowValue The low threshold value.
     * @exception java.lang.IllegalArgumentException The specified high/low threshold is null
     * or the low threshold is greater than the high threshold
     * or the high threshold and the low threshold are not of the same type.
     */
    public void setThresholds(Number highValue, Number lowValue) throws java.lang.IllegalArgumentException {

        if ((highValue == null) || (lowValue == null)) {
            throw new java.lang.IllegalArgumentException("The threshold values cannot be null.");
        }
        if (highValue.getClass() != lowValue.getClass()) {
            throw new java.lang.IllegalArgumentException("The high and the low thresholds must be of the same type.");
        }

        if (isFirstStrictlyGreaterThanLast(lowValue, highValue, highValue.getClass().getName())) {
            throw new java.lang.IllegalArgumentException("The threshold high value must be greater than or equal to threshold low value.");
        }

        synchronized(this) {
        highThreshold = highValue;
        lowThreshold = lowValue;
        for (int i = 0; i < elementCount; i++) {
            resetAlreadyNotified(i, THRESHOLD_ERROR_NOTIFIED);
        
            // Reset values.
            //
            status[i] = RISING_OR_FALLING;
        }
    }
    }

    /**
     * Gets the high notification's on/off switch value common to all
     * observed MBeans.
     *
     * @see #setNotifyHigh
     *
     * @return <CODE>true</CODE> if the gauge monitor notifies when
     * exceeding the high threshold, <CODE>false</CODE> otherwise.
     */
    public boolean getNotifyHigh() {
        return notifyHigh;
    }

    /**
     * Sets the high notification's on/off switch value common to all
     * observed MBeans.
     *
     * @param value The high notification's on/off switch value.
     *
     * @see #getNotifyHigh
     */
    public void setNotifyHigh(boolean value) {
	synchronized(this) {
        notifyHigh = value;
    }
    }

    /**
     * Gets the low notification's on/off switch value common to all
     * observed MBeans.
     *
     * @return <CODE>true</CODE> if the gauge monitor notifies when
     * exceeding the low threshold, <CODE>false</CODE> otherwise.
     *
     * @see #setNotifyLow
     */
    public boolean getNotifyLow() {
        return notifyLow;
    }

    /**
     * Sets the low notification's on/off switch value common to all
     * observed MBeans.
     *
     * @param value The low notification's on/off switch value.
     *
     * @see #getNotifyLow
     */
    public void setNotifyLow(boolean value) {
	synchronized(this) {
        notifyLow = value;
    }
    }

    /**
     * Gets the difference mode flag value common to all observed MBeans.
     *
     * @return <CODE>true</CODE> if the difference mode is used,
     * <CODE>false</CODE> otherwise.
     *
     * @see #setDifferenceMode
     */
    public boolean getDifferenceMode() {
        return differenceMode;
    }

    /**
     * Sets the difference mode flag value common to all observed MBeans.
     *
     * @param value The difference mode flag value.
     *
     * @see #getDifferenceMode
     */
    public void setDifferenceMode(boolean value) {
        synchronized(this) {
        differenceMode = value;

        // Reset values.
        //
        for (int i = 0; i < elementCount; i++) {
            status[i] = RISING_OR_FALLING;
            previousScanGauge[i] = null;
        }
    }
    }

   /**
     * Returns a <CODE>NotificationInfo</CODE> object containing the name of the Java class of the notification
     * and the notification types sent by the gauge monitor.
     */
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types  = { MonitorNotification.RUNTIME_ERROR,
                            MonitorNotification.OBSERVED_OBJECT_ERROR,
                            MonitorNotification.OBSERVED_ATTRIBUTE_ERROR,
                            MonitorNotification.OBSERVED_ATTRIBUTE_TYPE_ERROR,
                            MonitorNotification.THRESHOLD_ERROR,
                            MonitorNotification.THRESHOLD_HIGH_VALUE_EXCEEDED,
                            MonitorNotification.THRESHOLD_LOW_VALUE_EXCEEDED};
        MBeanNotificationInfo[] notifsInfo = { new MBeanNotificationInfo(types, "javax.management.monitor.MonitorNotification",
                                                                         "Notifications sent by the GaugeMonitor MBean")};
        return notifsInfo;
    }

    /*
     * ------------------------------------------
     *  PRIVATE METHODS
     * ------------------------------------------
     */

    /**
     * Updates the derived gauge and the derived gauge timestamp attributes
     * of the observed object at the specified index.
     *
     * @param scanGauge The value of the observed attribute.
     * @param index The index of the observed object.
     * @return <CODE>true</CODE> if the derived gauge value is valid, <CODE>false</CODE> otherwise.
     * The derived gauge value is invalid when the differenceMode flag is set to <CODE>true</CODE> and it is
     * the first notification (so we haven't 2 consecutive values to update the derived gauge).
     */
    private boolean updateDerivedGauge(Object scanGauge, int index) {

        boolean is_derived_gauge_valid;

        synchronized(this) {
        derivedGaugeTimestamp[index] = System.currentTimeMillis();
        
        // The gauge difference mode is used.
        //
        if (differenceMode == true) {
            
            // The previous scan gauge has been initialized.
            //
            if (previousScanGauge[index] != null) {
                setDerivedGaugeWithDifference((Number)scanGauge, index);
                is_derived_gauge_valid = true;
            } 
            // The previous scan gauge has not been initialized.
            // We cannot update the derived gauge...
            //
            else {
                is_derived_gauge_valid = false;
            }
            previousScanGauge[index] = (Number)scanGauge;
        } 
        // The gauge difference mode is not used.
        //
        else {
            derivedGauge[index] = (Number)scanGauge;
            is_derived_gauge_valid = true;
        }
	}

        return is_derived_gauge_valid;
    }

    /**
     * Updates the notification attribute of the observed object at the
     * specified index and notifies the listeners only once if the notify flag
     * is set to <CODE>true</CODE>. 
     * @param index The index of the observed object.
     */
    private void updateNotifications(int index) {
	boolean sendNotify = false;
	String notifType = null;
	long timeStamp = 0;
	String msg = null;
	Object derGauge = null;
	Object trigger = null;
                        
        // Send high notification if notifyHigh is true.
        // Send low notification if notifyLow is true.
        //
	synchronized(this) {
        if (status[index] == RISING_OR_FALLING) {
            if (isFirstGreaterThanLast(derivedGauge[index], highThreshold, type[index])) {
                if (notifyHigh == true) {
			sendNotify = true;
			notifType = MonitorNotification.THRESHOLD_HIGH_VALUE_EXCEEDED;
			timeStamp = derivedGaugeTimestamp[index];
			msg = "";
			derGauge = derivedGauge[index];
			trigger = highThreshold;

//                     sendNotification(MonitorNotification.THRESHOLD_HIGH_VALUE_EXCEEDED, derivedGaugeTimestamp[index],
//                                      "", derivedGauge[index], highThreshold, index);
                }
                status[index] = FALLING;
		} else if (isFirstGreaterThanLast(lowThreshold, derivedGauge[index], type[index])) {
                if (notifyLow == true) {
			sendNotify = true;
			notifType = MonitorNotification.THRESHOLD_LOW_VALUE_EXCEEDED;
			timeStamp = derivedGaugeTimestamp[index];
			msg ="";
			derGauge = derivedGauge[index];
			trigger = lowThreshold;

// 			sendNotification(MonitorNotification.THRESHOLD_LOW_VALUE_EXCEEDED, derivedGaugeTimestamp[index], 
// 					 "", derivedGauge[index], lowThreshold, index);
                }
                status[index] = RISING;
            }
	    } else {
            if (status[index] == RISING) {
                if (isFirstGreaterThanLast(derivedGauge[index], highThreshold, type[index])) {
                    if (notifyHigh == true) {
			    sendNotify = true;
			    notifType = MonitorNotification.THRESHOLD_HIGH_VALUE_EXCEEDED;
			    timeStamp = derivedGaugeTimestamp[index];
			    msg = "";
			    derGauge = derivedGauge[index];
			    trigger = highThreshold;

		// 	    sendNotification(MonitorNotification.THRESHOLD_HIGH_VALUE_EXCEEDED, derivedGaugeTimestamp[index], 
// 					     "", derivedGauge[index], highThreshold, index);
                    }
                    status[index] = FALLING;
                }
		} else if (status[index] == FALLING) {
                if (isFirstGreaterThanLast(lowThreshold, derivedGauge[index], type[index])) {
                    if (notifyLow == true) {
			    sendNotify = true;
			    notifType = MonitorNotification.THRESHOLD_LOW_VALUE_EXCEEDED;
			    timeStamp = derivedGaugeTimestamp[index];
			    msg = "";
			    derGauge = derivedGauge[index];
			    trigger = lowThreshold;;

// 			    sendNotification(MonitorNotification.THRESHOLD_LOW_VALUE_EXCEEDED, derivedGaugeTimestamp[index], 
// 					     "", derivedGauge[index], lowThreshold, index);
                    }
                    status[index] = RISING;
                }
            }
        }
    }
        
	if (sendNotify) {
	    sendNotification(notifType, timeStamp, msg, derGauge, trigger, index);
	}
    }
        
    /**
     * Tests if the threshold high and threshold low are both of the same type as the gauge.
     * Both integer and floating-point types are allowed.
     *
     * @param index The index of the observed object.
     * @return <CODE>true</CODE> if type is the same, <CODE>false</CODE> otherwise.
     */
    private boolean isThresholdTypeValid(int index) {
        
        boolean is_same_type = false;
                
        synchronized(this) {
        switch(type[index]) {
        case INTEGER:
            if ((highThreshold instanceof Integer) && (lowThreshold instanceof Integer))
                is_same_type = true;
            break;
        case BYTE:
            if ((highThreshold instanceof Byte) && (lowThreshold instanceof Byte))
                is_same_type = true;
            break;
        case SHORT:
            if ((highThreshold instanceof Short) && (lowThreshold instanceof Short))
                is_same_type = true;
            break;
        case LONG:
            if ((highThreshold instanceof Long) && (lowThreshold instanceof Long))
                is_same_type = true;
            break;
        case FLOAT:
            if ((highThreshold instanceof Float) && (lowThreshold instanceof Float))
                is_same_type = true;
            break;
        case DOUBLE:
            if ((highThreshold instanceof Double) && (lowThreshold instanceof Double))
                is_same_type = true;
            break;
        default:
            // Should never occured...
            if (isDebugOn()) {
                debug("isThresholdTypeValid", "the threshold type is invalid");
            }
            break;
        }
	}

        return is_same_type;
    }  
    
    /**
     * Sets the derived gauge when the differenceMode flag is set to <CODE>true</CODE>.
     * Both integer and floating-point types are allowed.
     *
     * @param scanGauge The value of the observed attribute.
     * @param index The index of the observed object.
     */
    private void setDerivedGaugeWithDifference(Number scanGauge, int index) { 
	synchronized(this) {
        switch (type[index]) {
        case INTEGER:
            derivedGauge[index] = new Integer(((Integer)scanGauge).intValue() - 
                                              ((Integer)previousScanGauge[index]).intValue());
            break;
        case BYTE:
            derivedGauge[index] = new Byte((byte)(((Byte)scanGauge).byteValue() - 
                                                  ((Byte)previousScanGauge[index]).byteValue()));
            break;
        case SHORT:
            derivedGauge[index] = new Short((short)(((Short)scanGauge).shortValue() - 
                                                    ((Short)previousScanGauge[index]).shortValue()));
            break;
        case LONG:
            derivedGauge[index] = new Long(((Long)scanGauge).longValue() - 
                                           ((Long)previousScanGauge[index]).longValue());
            break;
        case FLOAT:
            derivedGauge[index] = new Float(((Float)scanGauge).floatValue() - 
                                            ((Float)previousScanGauge[index]).floatValue());
            break;
        case DOUBLE:
            derivedGauge[index] = new Double(((Double)scanGauge).doubleValue() - 
                                             ((Double)previousScanGauge[index]).doubleValue());
        default:
            // Should never occured...
            if (isDebugOn()) {
                debug("setDerivedGaugeWithDifference", "the threshold type is invalid");
            }
            break;
        }
    } 
    }

    /**
     * Tests if the first specified Number is greater than or equal to the last.
     * Both integer and floating-point types are allowed.
     *
     * @param greater The first Number to compare with the second.
     * @param less The second Number to compare with the first.
     * @param type The number type.
     * @return <CODE>true</CODE> if the first specified Number is greater than or equal to the last, <CODE>false</CODE> otherwise.
     */
    private boolean isFirstGreaterThanLast(Number greater, Number less, int type) {

        boolean is_greater = false;

        switch(type) {
        case INTEGER:
        case BYTE:
        case SHORT:
        case LONG:
            if (greater.longValue() >= less.longValue())
                is_greater = true;
            break;
        case FLOAT:
        case DOUBLE:
            if (greater.doubleValue() >= less.doubleValue())
                is_greater = true;
            break;
        default:
            // Should never occured...
            if (isDebugOn()) {
                debug("isFirstGreaterThanLast", "the threshold type is invalid");
            }
            break;
        }
        return is_greater;
    }
    
    /**
     * Tests if the first specified Number is strictly greater than the last.
     * Both integer and floating-point types are allowed.
     *
     * @param greater The first Number to compare with the second.
     * @param less The second Number to compare with the first.
     * @param class The number class name.
     * @return <CODE>true</CODE> if the first specified Number is strictly greater than the last, <CODE>false</CODE> otherwise.
     */
    private boolean isFirstStrictlyGreaterThanLast(Number greater, Number less, String className) {
        
        boolean is_greater = false;
        
        if (className.equals("java.lang.Integer") ||
            className.equals("java.lang.Byte") ||
            className.equals("java.lang.Short") ||
            className.equals("java.lang.Long")) {
            
            if (greater.longValue() > less.longValue())
                is_greater = true;
        }
        else if (className.equals("java.lang.Float") ||
                 className.equals("java.lang.Double")) {
            
            if (greater.doubleValue() > less.doubleValue())
                is_greater = true;
        }
        else {            
            // Should never occured...
            if (isDebugOn()) {
                debug("isFirstStrictlyGreaterThanLast", "the threshold type is invalid");
            }
        }
        return is_greater;
    } 

    /*
     * ------------------------------------------
     *  PACKAGE METHODS
     * ------------------------------------------
     */

    /**
     * This method is called by the gauge monitor each time
     * the granularity period has been exceeded.
     * @param index The index of the observed object.
     */
    void notifyAlarmClock(int index) {
	boolean sendNotify = false;
	String notifType = null;
	long timeStamp = 0;
	String msg = null;
	Object derGauge = null;
	Object trigger = null;

        Object  scan_gauge = null;
        String  notif_type = null;
        
	synchronized(this) {        
        try {
            if (isActive()) {
                
                // Check if the observed object and observed attribute are valid.
                //
                
                // Check that neither the observed object nor the observed attribute are null.
                // If the observed object or observed attribute is null, this means that
                // the monitor started before a complete initialization and nothing is done.
                //
                if ((getObservedObject(index) == null) || (getObservedAttribute() == null)) {
                    return;
                }
                
                // Check that the observed object is registered in the MBean server and
                // that the observed attribute belongs to the observed object.
                //
                try {
                    scan_gauge = server.getAttribute(getObservedObject(index), getObservedAttribute());
		    if (scan_gauge == null)
			return;
                } catch (NullPointerException np_ex) {
                    if ((alreadyNotifieds[index] & RUNTIME_ERROR_NOTIFIED) != RESET_FLAGS_ALREADY_NOTIFIED) {
                        return;
                    } else {
                        notif_type = MonitorNotification.RUNTIME_ERROR;
                        setAlreadyNotified(index, RUNTIME_ERROR_NOTIFIED);
                        throw new MonitorSettingException("The gauge monitor must be registered in the MBean server.");
                    }
                } catch (InstanceNotFoundException inf_ex) {
                    if ((alreadyNotifieds[index] & OBSERVED_OBJECT_ERROR_NOTIFIED) != RESET_FLAGS_ALREADY_NOTIFIED) {
                        return;
                    } else {
                        notif_type = MonitorNotification.OBSERVED_OBJECT_ERROR;
                        setAlreadyNotified(index, OBSERVED_OBJECT_ERROR_NOTIFIED);
                        throw new MonitorSettingException("The observed object must be registered in the MBean server.");
                    }
                } catch (AttributeNotFoundException anf_ex) {
                    if ((alreadyNotifieds[index] & OBSERVED_ATTRIBUTE_ERROR_NOTIFIED) != RESET_FLAGS_ALREADY_NOTIFIED) {
                        return;
                    } else {
                        notif_type = MonitorNotification.OBSERVED_ATTRIBUTE_ERROR;
                        setAlreadyNotified(index, OBSERVED_ATTRIBUTE_ERROR_NOTIFIED);
                        throw new MonitorSettingException("The observed attribute must be accessible in the observed object.");
                    }
                } catch (MBeanException mb_ex) {
                    if ((alreadyNotifieds[index] & RUNTIME_ERROR_NOTIFIED) != RESET_FLAGS_ALREADY_NOTIFIED) {
                        return;
                    } else {
                        notif_type = MonitorNotification.RUNTIME_ERROR;
                        setAlreadyNotified(index, RUNTIME_ERROR_NOTIFIED);
                        throw new MonitorSettingException(mb_ex.getMessage());
                    }
                } catch (ReflectionException ref_ex) {
                    if ((alreadyNotifieds[index] & OBSERVED_ATTRIBUTE_ERROR_NOTIFIED) != RESET_FLAGS_ALREADY_NOTIFIED) {
                        return;
                    } else {
                        notif_type = MonitorNotification.OBSERVED_ATTRIBUTE_ERROR;
                        setAlreadyNotified(index, OBSERVED_ATTRIBUTE_ERROR_NOTIFIED);
                        throw new MonitorSettingException(ref_ex.getMessage());
                    }
                }

                // Check that the observed attribute is either of type "Integer" or "Float".
                //
                if (scan_gauge instanceof Integer) {
                    type[index] = INTEGER;
                } else if (scan_gauge instanceof Byte) {
                    type[index] = BYTE;
                } else if (scan_gauge instanceof Short) {
                    type[index] = SHORT;
                } else if (scan_gauge instanceof Long) {
                    type[index] = LONG;
                } else if (scan_gauge instanceof Float) {
                    type[index] = FLOAT;
                } else if (scan_gauge instanceof Double) {
                    type[index] = DOUBLE;
                } else {
                    if ((alreadyNotifieds[index] & OBSERVED_ATTRIBUTE_TYPE_ERROR_NOTIFIED) != RESET_FLAGS_ALREADY_NOTIFIED) {
                        return;
                    } else {
                        notif_type = MonitorNotification.OBSERVED_ATTRIBUTE_TYPE_ERROR;
                        setAlreadyNotified(index, OBSERVED_ATTRIBUTE_TYPE_ERROR_NOTIFIED);
                        throw new MonitorSettingException("The observed attribute type must be an integer type or a floating-point type.");
                    }
                }

                // Check that threshold high and threshold low are of the same type as the gauge.
                //
                if (!isThresholdTypeValid(index)) {
                    if ((alreadyNotifieds[index] & THRESHOLD_ERROR_NOTIFIED) != RESET_FLAGS_ALREADY_NOTIFIED) {
                        return;
                    } else {
                        notif_type = MonitorNotification.THRESHOLD_ERROR;
                        setAlreadyNotified(index, THRESHOLD_ERROR_NOTIFIED);
                        throw new MonitorSettingException("The threshold high and threshold low must be of the same type as the gauge.");
                    }
                }

                // Clear all already notified flags.
                //
		resetAllAlreadyNotified(index);
                
                // Update the derived gauge attributes and check the validity of the new value.
                // The derived gauge value is invalid when the differenceMode flag is set to true and it is
                // the first notification (so we haven't 2 consecutive values to update the derived gauge).
                //
                boolean is_derived_gauge_valid = updateDerivedGauge(scan_gauge, index);
                
                // Notify the listeners if the updated derived gauge value is valid.
                //
                if (is_derived_gauge_valid) {
                    updateNotifications(index);
                }
            }
	    } catch (MonitorSettingException ms_ex) {
            
            // Create and send the monitor error notification.
            //
		sendNotify = true;
		notifType = notif_type;
		timeStamp = derivedGaugeTimestamp[index];
		msg = ms_ex.getMessage();
		derGauge = derivedGauge[index];
		trigger = null;
            
// 		sendNotification(notif_type, derivedGaugeTimestamp[index], ms_ex.getMessage(), 
// 				 derivedGauge[index], null, index);
		
            // Reset values.
            //
            status[index] = RISING_OR_FALLING;
            previousScanGauge[index] = null;
        }
    }   

	if (sendNotify) {
	    sendNotification(notifType, timeStamp, msg, derGauge, trigger, index);
	}
    }   

    /**
     * This method is called when adding a new observed object in the vector.
     * It updates all the gauge specific arrays.
     * @param index The index of the observed object.
     */
    void insertSpecificElementAt(int index) {
	synchronized(this) {
        // Update derivedGauge, derivedGaugeTimestamp,  previousScanGauge, 
        // status and type arrays.
        //
        Number nb = new Integer(0);
        insertNumberElementAt(derivedGauge, nb, index);
        insertNumberElementAt(previousScanGauge, null, index);
        insertlongElementAt(derivedGaugeTimestamp, (new Date()).getTime(), index);
        insertintElementAt(status, RISING_OR_FALLING, index);
        insertintElementAt(type, INTEGER, index);
    }
    }
    
    
    /**
     * This method is called when removing an observed object from the vector.
     * It updates all the gauge specific arrays.
     * @param index The index of the observed object.
     */
    void removeSpecificElementAt(int index) {
	synchronized(this) {
        // Update derivedGauge, derivedGaugeTimestamp,  previousScanGauge, 
        // status and type arrays.
        //
        removeNumberElementAt(derivedGauge, index);
        removeNumberElementAt(previousScanGauge, index);
        removelongElementAt(derivedGaugeTimestamp, index);
        removeintElementAt(status, index);
        removeintElementAt(type, index);
    }
    }


  /**
   * GaugeAlarmClock inner class:
   * This class provides a simple implementation of an alarm clock MBean.
   * The aim of this MBean is to set up an alarm which wakes up the gauge monitor every granularity period.
   */
  
  private static class GaugeAlarmClock extends TimerTask {
    
    GaugeMonitor listener = null;

    /*
     * ------------------------------------------
     *  CONSTRUCTORS
     * ------------------------------------------
     */

    public GaugeAlarmClock(GaugeMonitor listener) {
        this.listener = listener;
    }

    /*
     * ------------------------------------------
     *  PUBLIC METHODS
     * ------------------------------------------
     */

    /**
     * This method is called by the GaugeAlarmClock thread when it is started.
     */
    public void run() {
      if (listener.isActive()) {
        for (int i = 0; i < listener.elementCount; i++) {
          listener.notifyAlarmClock(i);
        }
      }
    }
  }
}
