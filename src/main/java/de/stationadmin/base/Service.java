/**
 * 
 */
package de.stationadmin.base;

import java.io.IOException;

/**
 * Interface for Station Admin services
 * 
 * @author Frank
 */
public interface Service {
  
  /**
   * Loads data (usually from disk) on startup
   * @throws IOException
   */
  void load() throws IOException;
  
  /**
   * Synchronizes data with the server
   * @throws IOException
   */
  void synchronize() throws IOException;

  /**
   * Releases any allocated resources, stops background tasks
   */
  void close();
  
  /**
   * Starts background tasks (if available)
   */
  void initBackgroundTasks();
}
