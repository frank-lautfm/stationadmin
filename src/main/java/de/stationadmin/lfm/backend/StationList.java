/**
 * 
 */
package de.stationadmin.lfm.backend;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * @author korf
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StationList {
  
  private Station[] stations;

  public Station[] getStations() {
    return stations;
  }

  public void setStations(Station[] stations) {
    this.stations = stations;
  }

}
