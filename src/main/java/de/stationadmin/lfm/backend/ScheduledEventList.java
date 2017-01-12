/**
 * 
 */
package de.stationadmin.lfm.backend;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * @author korf
 *
 */
@XmlRootElement(name = "ScheduledEventList")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduledEventList {
  private ScheduledEvent[] events;

  /**
   * @return the events
   */
  public ScheduledEvent[] getEvents() {
    return events;
  }

  /**
   * @param events the events to set
   */
  public void setEvents(ScheduledEvent[] events) {
    this.events = events;
  }

}
