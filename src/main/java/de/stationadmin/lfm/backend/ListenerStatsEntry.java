package de.stationadmin.lfm.backend;

import java.util.Date;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "ListenerStats")
public class ListenerStatsEntry {

  private Date date;
  @JsonProperty("date_from")
  private Date dateFrom;
  @JsonProperty("date_to")
  private Date dateTo;
  private int sessions;
  @JsonProperty("parallel_sessions_per_h")
  private int[] parallelSessionsPerHour;

  private Map<String, ListenerStatsSource> sources;

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public Date getDateFrom() {
    return dateFrom;
  }

  public void setDateFrom(Date dateFrom) {
    this.dateFrom = dateFrom;
  }

  public Date getDateTo() {
    return dateTo;
  }

  public void setDateTo(Date dateTo) {
    this.dateTo = dateTo;
  }

  public int getSessions() {
    return sessions;
  }

  public void setSessions(int sessions) {
    this.sessions = sessions;
  }

  public int[] getParallelSessionsPerHour() {
    return parallelSessionsPerHour;
  }

  public void setParallelSessionsPerHour(int[] parallelSessionsPerHour) {
    this.parallelSessionsPerHour = parallelSessionsPerHour;
  }

  public Map<String, ListenerStatsSource> getSources() {
    return sources;
  }

  public void setSources(Map<String, ListenerStatsSource> sources) {
    this.sources = sources;
  }

}
