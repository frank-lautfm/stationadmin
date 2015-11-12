/**
 * 
 */
package de.stationadmin.base;

import java.io.File;
import java.io.IOException;
import java.util.Timer;

import de.stationadmin.lfm.backend.LautfmAdminService;
import de.stationadmin.lfmapi.LautfmService;

/**
 * @author Frank
 *
 */
public class SessionCtx {
  private int stationId;
  private String station;
  private String dataDirectory;
  private String settingsDirectory;
  private LautfmAdminService server;
  private LautfmService lfmService;
  private Status status;
  private StationStatus stationStatus = new StationStatus();
  private Timer timer = new Timer(true);
  private Boolean liveEnabled;

  /**
   * @param station
   * @param dataDirectory
   * @param server
   */
  public SessionCtx(LautfmAdminService server, LautfmService lfmAPI, int stationId, String station, String dataDirectory, String settingsDirectory) {
    super();
    this.stationId = stationId;
    this.station = station;
    this.dataDirectory = this.appendFileSeparator(dataDirectory);
    this.settingsDirectory = this.appendFileSeparator(settingsDirectory);
    this.server = server;
    this.lfmService = lfmAPI;
  }
  
  private String appendFileSeparator(String dir) {
    if(!(dir.endsWith("/") || dir.endsWith("\\"))) {
      return dir + File.separatorChar;
    }
    else {
      return dir;
    }
  }

  /**
   * @return the status
   */
  public Status getStatus() {
    return status;
  }

  /**
   * @param status the status to set
   */
  public void setStatus(Status status) {
    this.status = status;
  }

  /**
   * @return the station
   */
  public String getStation() {
    return station;
  }

  /**
   * @return the dataDirectory
   */
  public String getDataDirectory() {
    return dataDirectory;
  }

  public String getStationDirectory() {
    return this.dataDirectory + this.station + File.separatorChar;
  }

  /**
   * @return the server
   */
  public LautfmAdminService getServer() {
    return server;
  }

  /**
   * @return the lfmAPI
   */
  public LautfmService getLfmAPI() {
    return lfmService;
  }

  public void updateStatus(String key, String... parameters) {
    if (key != null) {
      this.setStatus(new Status(key, parameters));
    } else {
      this.setStatus(null);
    }
  }

  /**
   * @return the timer
   */
  public Timer getTimer() {
    return timer;
  }

  /**
   * @return the stationStatus
   */
  public StationStatus getStationStatus() {
    return stationStatus;
  }

  public String getSettingsDirectory() {
    return settingsDirectory;
  }
  
  public void checkSession() throws IOException {
//    if(!StringUtils.equals(this.station, this.server.getCurrentStation())) {
//      this.server.logout();
//      throw new IOException("Ungueltige Session - bitte neu einloggen!");
//    }
  }

  public boolean isLiveEnabled() throws IOException {
//    if(this.liveEnabled == null) {
//      this.liveEnabled = this.server.isLiveEnabled();
//    }
    return false;
  }

  public int getStationId() {
    return stationId;
  }


}
