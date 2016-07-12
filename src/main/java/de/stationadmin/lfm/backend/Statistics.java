/**
 * 
 */
package de.stationadmin.lfm.backend;

import java.util.Date;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author korf
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "Statistics")
public class Statistics {

  @JsonProperty("listeners_now")
  private int listenersNow;

  @JsonProperty("position_now")
  private int positionNow;

  @JsonProperty("tlh_log")
  private Map<String, Integer> tlhLog;

  @JsonProperty("switchons_log")
  private Map<String, Integer> switchonsLog;

  /**
   * @return the listenersNow
   */
  public int getListenersNow() {
    return listenersNow;
  }

  /**
   * @param listenersNow
   *          the listenersNow to set
   */
  public void setListenersNow(int listenersNow) {
    this.listenersNow = listenersNow;
  }

  /**
   * @return the positionNow
   */
  public int getPositionNow() {
    return positionNow;
  }

  /**
   * @param positionNow
   *          the positionNow to set
   */
  public void setPositionNow(int positionNow) {
    this.positionNow = positionNow;
  }

  /**
   * @return the tlhLog
   */
  public Map<String, Integer> getTlhLog() {
    return tlhLog;
  }

  /**
   * @param tlhLog
   *          the tlhLog to set
   */
  public void setTlhLog(Map<String, Integer> tlhLog) {
    this.tlhLog = tlhLog;
  }

  /**
   * @return the switchonsLog
   */
  public Map<String, Integer> getSwitchonsLog() {
    return switchonsLog;
  }

  /**
   * @param switchonsLog
   *          the switchonsLog to set
   */
  public void setSwitchonsLog(Map<String, Integer> switchonsLog) {
    this.switchonsLog = switchonsLog;
  }

}
