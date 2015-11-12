/**
 * 
 */
package de.stationadmin.base.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.SwingUtilities;

/**
 * Base class that provides property change support.
 * <p>
 * PCS is transient - so listeners wouldn't be seriazlized when persisting this
 * object with XStream
 * 
 * @author korf
 */
public class AbstractBean {
  private static boolean eventsInEDT = false;

  private transient PropertyChangeSupport pcs;

  protected PropertyChangeSupport getPcs() {
    if (this.pcs == null) {
      this.pcs = new PropertyChangeSupport(this);
    }
    return pcs;
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    getPcs().addPropertyChangeListener(listener);
  }

  public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
    getPcs().addPropertyChangeListener(property, listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    getPcs().removePropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
    getPcs().removePropertyChangeListener(property, listener);
  }

  protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue) {
    if (eventsInEDT && !SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(new Runnable() {

        @Override
        public void run() {
          getPcs().firePropertyChange(propertyName, oldValue, newValue);
        }

      });

    } else {
      getPcs().firePropertyChange(propertyName, oldValue, newValue);
    }
  }

  protected void fireIndexedPropertyChange(final String propertyName, final int index, final Object oldValue,
      final Object newValue) {
    if (eventsInEDT && !SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(new Runnable() {

        @Override
        public void run() {
          getPcs().fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
        }

      });

    } else {
      getPcs().fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
    }

  }

  public static boolean isEventsInEDT() {
    return eventsInEDT;
  }

  public static void setEventsInEDT(boolean eventsInEDT) {
    AbstractBean.eventsInEDT = eventsInEDT;
  }

}
