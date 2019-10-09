package de.stationadmin.base.config;

/**
 * Interface for services contributing data that needs to be stored in client configuration
 */
public interface ClientConfigurationSource {
  
  /**
   * Applies configuration that was retrieved from server
   * @param cfg
   */
  void applyClientConfiguration(ClientConfiguration cfg);

  /**
   * Adds configuration to the given ClientConfiguration instance before this will be saved
   * @param cfg
   */
  void collectClientConfiguration(ClientConfiguration cfg);
  
 
}
