/**
 * 
 */
package de.stationadmin.lfm.backend;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author korf
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "Track")
public class MarkTrackPrivateRequest {
  private int id;
  @JsonProperty("private")
  private boolean privateTrack;
  /**
   * @return the id
   */
  public int getId() {
    return id;
  }
  /**
   * @param id the id to set
   */
  public void setId(int id) {
    this.id = id;
  }
  /**
   * @return the privateTrack
   */
  public boolean isPrivateTrack() {
    return privateTrack;
  }
  /**
   * @param privateTrack the privateTrack to set
   */
  public void setPrivateTrack(boolean privateTrack) {
    this.privateTrack = privateTrack;
  }
  
  

}
