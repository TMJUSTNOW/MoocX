/*
 * @(#)file      NotificationBroadcaster.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.33
 * @(#)lastedit      03/07/15
 *
 * Copyright 2000-2003 Sun Microsystems, Inc.  All rights reserved.
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 * 
 * Copyright 2000-2003 Sun Microsystems, Inc.  Tous droits r�serv�s.
 * Ce logiciel est propriet� de Sun Microsystems, Inc.
 * Distribu� par des licences qui en restreignent l'utilisation. 
 *
 */


package javax.management;


/**
 * <p>Interface implemented by an MBean that emits Notifications. It
 * allows a listener to be registered with the MBean as a notification
 * listener.</p>
 *
 * <p>New code should use the {@link NotificationEmitter} interface
 * instead.</p>
 *
 * @since-jdkbundle 1.5
 */
public interface NotificationBroadcaster { 
    
    /**
     * Adds a listener to this MBean.
     *
     * @param listener The listener object which will handle the
     * notifications emitted by the broadcaster.
     * @param filter The filter object. If filter is null, no
     * filtering will be performed before handling notifications.
     * @param handback An opaque object to be sent back to the
     * listener when a notification is emitted. This object cannot be
     * used by the Notification broadcaster object. It should be
     * resent unchanged with the notification to the listener.
     *
     * @exception IllegalArgumentException Listener parameter is null.
     *
     * @see #removeNotificationListener
     */
    public void addNotificationListener(NotificationListener listener,
					NotificationFilter filter,
					Object handback)
	    throws java.lang.IllegalArgumentException;
    
    /**
     * Removes a listener from this MBean.  If the listener
     * has been registered with different handback objects or
     * notification filters, all entries corresponding to the listener
     * will be removed.
     *
     * @param listener A listener that was previously added to this
     * MBean.
     *
     * @exception ListenerNotFoundException The listener is not
     * registered with the MBean.
     *
     * @see #addNotificationListener
     * @see NotificationEmitter#removeNotificationListener
     */
    public void removeNotificationListener(NotificationListener listener) 
	    throws ListenerNotFoundException;
    
    /**
     * <p>Returns an array indicating, for each notification this
     * MBean may send, the name of the Java class of the notification
     * and the notification type.</p>
     *
     * <p>It is not illegal for the MBean to send notifications not
     * described in this array.  However, some clients of the MBean
     * server may depend on the array being complete for their correct
     * functioning.</p>
     *
     * @return the array of possible notifications.
     */
    public MBeanNotificationInfo[] getNotificationInfo();  
}
