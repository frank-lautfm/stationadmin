/**
 * 
 */
package de.stationadmin.migration;

/**
 * @author korf
 *
 */
public interface MessageReceiver {

  void onMessage(String msg);
}
