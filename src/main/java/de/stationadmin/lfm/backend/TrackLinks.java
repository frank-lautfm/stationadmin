/**
 * 
 */
package de.stationadmin.lfm.backend;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author korf
 * 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackLinks {
  private Link self;
  private Link prelisten;
  @JsonProperty("prelisten_authless")
  private Link prelistenAuthless;

  /**
   * @return the self
   */
  public Link getSelf() {
    return self;
  }

  /**
   * @param self
   *          the self to set
   */
  public void setSelf(Link self) {
    this.self = self;
  }

  /**
   * @return the prelisten
   */
  public Link getPrelisten() {
    return prelisten;
  }

  /**
   * @param prelisten
   *          the prelisten to set
   */
  public void setPrelisten(Link prelisten) {
    this.prelisten = prelisten;
  }

  /**
   * @return the prelistenAuthless
   */
  public Link getPrelistenAuthless() {
    return prelistenAuthless;
  }

  /**
   * @param prelistenAuthless
   *          the prelistenAuthless to set
   */
  public void setPrelistenAuthless(Link prelistenAuthless) {
    this.prelistenAuthless = prelistenAuthless;
  }

}
