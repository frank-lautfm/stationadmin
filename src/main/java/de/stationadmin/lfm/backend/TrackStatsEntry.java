/**
 * 
 */
package de.stationadmin.lfm.backend;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

/**
 * @author korf
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "Track")
public class TrackStatsEntry {

  private int id;
  private int listeners;
  @JsonProperty("started_at")
  @JsonDeserialize(using = CustomJsonDateDeserializer.class)
  private Date startedAt;

  /**
   * @return the id
   */
  public int getId() {
    return id;
  }

  /**
   * @param id
   *          the id to set
   */
  public void setId(int id) {
    this.id = id;
  }

  /**
   * @return the listeners
   */
  public int getListeners() {
    return listeners;
  }

  /**
   * @param listeners
   *          the listeners to set
   */
  public void setListeners(int listeners) {
    this.listeners = listeners;
  }

  /**
   * @return the startedAt
   */
  public Date getStartedAt() {
    return startedAt;
  }

  /**
   * @param startedAt
   *          the startedAt to set
   */
  public void setStartedAt(Date startedAt) {
    this.startedAt = startedAt;
  }

}
