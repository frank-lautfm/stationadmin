/**
 * 
 */
package de.stationadmin.lfm.backend;

/**
 *
 * @author Frank Korf
 *
 */
public interface ProgressListener {
  
  void setMaxValue(int max);
  
  void setCurrentValue(int value);

  boolean isAbortCurrent();
}
