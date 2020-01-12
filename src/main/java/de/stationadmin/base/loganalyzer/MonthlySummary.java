package de.stationadmin.base.loganalyzer;

import java.util.Calendar;
import java.util.Date;

import de.stationadmin.lfm.backend.ListenerStatsEntry;
import de.stationadmin.lfm.backend.ListenerStatsSource;

public class MonthlySummary {

  private int year;
  private int month;
  private Date date;
  private int tlh;
  private int avgUniqs;
  private int avgSessions;

  public MonthlySummary(ListenerStatsEntry entry) {
    
    date = entry.getDateFrom();
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    year = cal.get(Calendar.YEAR);
    month = cal.get(Calendar.MONTH) + 1;
    
    int days = (int) (entry.getDateTo().getTime() - entry.getDateFrom().getTime()) / (1000 * 60 * 60 * 24);
    ListenerStatsSource src = entry.getSources().get("all");
    tlh = src.getTlh();
    if (days > 0) {
      avgUniqs = src.getUniqs() / days;
      avgSessions = src.getSessions() / days;
    }
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public int getTlh() {
    return tlh;
  }

  public void setTlh(int tlh) {
    this.tlh = tlh;
  }

  public int getAvgUniqs() {
    return avgUniqs;
  }

  public void setAvgUniqs(int avgUniqs) {
    this.avgUniqs = avgUniqs;
  }

  public int getAvgSessions() {
    return avgSessions;
  }

  public void setAvgSessions(int avgSessions) {
    this.avgSessions = avgSessions;
  }

  public int getYear() {
    return year;
  }

  public int getMonth() {
    return month;
  }

}
