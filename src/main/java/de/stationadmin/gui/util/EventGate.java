/**
 * 
 */
package de.stationadmin.gui.util;

/**
 * @author korf
 *
 */
public class EventGate {
  private volatile boolean updating = false;

  public boolean isUpdating() {
    return updating;
  }

  public void setUpdating(boolean updating) {
    this.updating = updating;
  }

}
