/**
 * 
 */
package de.stationadmin.base.loganalyzer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

import de.stationadmin.base.Service;
import de.stationadmin.base.SessionCtx;
import de.stationadmin.base.Settings;
import de.stationadmin.base.Version;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.TrackRegistry;

/**
 * @author korf
 * 
 */
public class LogAnalyzerService implements Service {
  public static final int DAY_IN_MS = 1000 * 60 * 60 * 24;
  private static final Logger log = Logger.getLogger(LogAnalyzerService.class);
  private static final String DATE_FORMAT = "yyyy-MM-dd";
  private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
  private String apiURL = "http://stationadmin.emjoy.net/api/";
  private SessionCtx ctx;
  private Settings settings;
  private TrackRegistry titleRegistry;
  private String logCacheDir;

  private DefaultHttpClient client;

  private List<ListenersEntry> listenersToday;
  private List<Play> playsToday;

  public LogAnalyzerService(SessionCtx ctx, Settings settings, TrackRegistry titleRegistry) {
    this.ctx = ctx;
    this.settings = settings;
    this.titleRegistry = titleRegistry;
    this.logCacheDir = ctx.getStationDirectory() + "log" + File.separatorChar;

    this.client = new DefaultHttpClient();
    client.getParams().setParameter("http.useragent",
        "Mozilla/4.0 (compatible; Station Admin " + Version.VERSION + "; " + System.getProperty("os.name") + ")");

  }

  /**
   * @see de.stationadmin.base.Service#load()
   */
  @Override
  public void load() {
    ctx.updateStatus("loadLogstatistics");
    if (this.settings.isLogDownloadPermitted() && this.settings.isLogAutodownloadPermitted()) {
      Thread t = new Thread() {

        @Override
        public void run() {
          // make sure statistics of past 7 days are downloaded
          for (int i = 7; i > 0; i--) {
            long time = System.currentTimeMillis() - i * 24 * 60 * 60 * 1000l;
            Date day = new Date(time);
            try {
              getPlaysOf(day);
              getListenersOf(day);
            } catch (IOException e) {
              log.error("error while loading log data for " + day);
            }
          }
        }

      };
      t.start();

    }

    try {
      this.writeDailyStatistics();
    } catch (Exception e) {
      log.error("failed to write daily summary", e);
    }

  }

  private String getRawDataOf(String type, Date day) throws IOException {
    if (day.getTime() > System.currentTimeMillis() && !isToday(day)) {
      return "";
    }
    String date = new SimpleDateFormat(DATE_FORMAT).format(day);
    String filename = this.logCacheDir + type + "-" + date + ".log";
    File file = new File(filename);

    // check if local copy exists
    if (file.exists()) {
      return FileUtils.readFileToString(file, "UTF-8");
    }

    if (day.getTime() > System.currentTimeMillis() - DAY_IN_MS * 8 && this.settings.isLogDownloadPermitted()) {

      // fetch from server
      log.info("download " + type + " for " + date);
      HttpGet action = new HttpGet(apiURL + type + ".php?station=" + this.ctx.getStation() + "&day=" + date);
      HttpResponse response = this.client.execute(action);
      String content = IOUtils.toString(response.getEntity().getContent());

      if (!isToday(day)) {
        // write data to cache
        FileUtils.writeStringToFile(file, content, "UTF-8");
      }

      return content;
    } else {
      return "";
    }
  }

  String getRawListenerStatsOf(Date day) throws IOException {
    return this.getRawDataOf("station_listeners", day);
  }

  String getRawPlaysOf(Date day) throws IOException {
    return this.getRawDataOf("station_plays", day);
  }

  public List<Play> getPlaysOf(Date day) throws IOException {
    boolean today = this.isToday(day);
    if (today && this.playsToday != null && this.playsToday.size() > 0) {
      if (this.isToday(playsToday.get(0).getStartTime())) {
        return this.playsToday;
      } else {
        // day change - discard old list
        this.playsToday = null;
      }

    }
    String raw = this.getRawPlaysOf(day);
    List<Play> plays = new ArrayList<Play>();
    String[] lines = StringUtils.split(raw, "\r\n");
    SimpleDateFormat fmt = new SimpleDateFormat(TIME_FORMAT);
    for (String line : lines) {
      String[] cols = StringUtils.split(line.trim(), "\t");
      if (cols.length == 2) {
        try {
          Date date = fmt.parse(cols[0]);
          int titleId = Integer.parseInt(cols[1]);
          DetailedTrack title = this.titleRegistry.getTrack(titleId);
          if (title == null) {
            title = new DetailedTrack();
            title.setId(titleId);
            title.setArtist("<unknown>");
            title.setTitle("<Title " + titleId + ">");
          }
          Play play = new Play(date, title);
          plays.add(play);

        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    if (today) {
      this.playsToday = plays;
    }
    return plays;
  }

  /**
   * Gets all plays from the given time frame
   * 
   * @param from
   * @param to
   * @return
   * @throws IOException
   */
  public List<Play> getPlaysBetween(Date from, Date to) throws IOException {
    List<Play> plays = new ArrayList<Play>();

    for (long t = from.getTime(); t < to.getTime() + DAY_IN_MS; t += DAY_IN_MS) {
      List<Play> playsOfDay = this.getPlaysOf(new Date(t));
      for (Play play : playsOfDay) {
        if (play.getStartTime().getTime() >= from.getTime() && play.getStartTime().getTime() < to.getTime() + 59999) {
          plays.add(play);
        }
      }
    }

    return plays;

  }

  /**
   * Get all listener statistics entries of the given time frame
   * 
   * @param from
   * @param to
   * @return
   * @throws IOException
   */
  public List<ListenersEntry> getListenersBetween(Date from, Date to) throws IOException {
    List<ListenersEntry> entries = new ArrayList<ListenersEntry>();

    for (long t = from.getTime(); t < to.getTime() + DAY_IN_MS; t += DAY_IN_MS) {
      List<ListenersEntry> entriesOfDay = this.getListenersOf(new Date(t));
      for (ListenersEntry entry : entriesOfDay) {
        if (entry.getTime().getTime() >= from.getTime() && entry.getTime().getTime() < to.getTime() + 59999) {
          entries.add(entry);
        }
      }
    }
    return entries;
  }

  public List<DailySummary> getDailySummaries(Date from, Date to) throws IOException {
    List<DailySummary> entries = new ArrayList<DailySummary>();

    for (long t = from.getTime(); t < to.getTime() + DAY_IN_MS; t += DAY_IN_MS) {
      DailySummary summary = this.getDailySummaryOf(new Date(t));
      if (summary != null) {
        entries.add(summary);
      }
    }

    return entries;

  }

  public List<ListenersAvgEntry> getAverageListenersInDay(List<ListenersEntry> rawEntries, int weekdays, int granularity) {
    List<ListenersAvgEntry> entries = new ArrayList<ListenersAvgEntry>();

    ListenerEntryCollection[] rawEntriesByHour = new ListenerEntryCollection[24];
    for (int i = 0; i < rawEntriesByHour.length; i++) {
      rawEntriesByHour[i] = new ListenerEntryCollection();
    }
    Calendar cal = Calendar.getInstance();
    for (ListenersEntry entry : rawEntries) {
      cal.setTime(entry.getTime());
      int hour = cal.get(Calendar.HOUR_OF_DAY);
      int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
      if (weekdays == 0 || (weekdays & (1 << dayOfWeek)) > 0) {
        rawEntriesByHour[hour].add(entry);
      }
    }

    int total = 0;
    for (ListenerEntryCollection col : rawEntriesByHour) {
      total += col.getTotalListeners();
    }

    for (int i = 0; i < 24; i += granularity) {
      ListenerEntryCollection base = rawEntriesByHour[i];
      for (int j = 1; j < granularity; j++) {
        base.add(rawEntriesByHour[i + j]);
      }
      int fraction = Math.round(((float) base.getTotalListeners() / total) * 100);
      entries.add(new ListenersAvgEntry(i, i + granularity - 1, base.getAvgListeners(), fraction));
    }

    return entries;
  }

  public DailySummary getDailySummaryOf(Date day) throws IOException {
    if (this.isToday(day) || day.getTime() > System.currentTimeMillis()) {
      return null; // today or future - not available
    }

    String date = new SimpleDateFormat(DATE_FORMAT).format(day);
    String filename = this.logCacheDir + "station_dailysummary" + "-" + date + ".log";
    File file = new File(filename);
    
    Calendar cal = Calendar.getInstance();
    cal.setTime(day);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    // check if local copy exists
    if (file.exists()) {
      String raw = FileUtils.readFileToString(file, "UTF-8");
      int listeners = 0, duration = 0, avg = 0;
      for (String line : StringUtils.split(raw, "\n")) {
        String[] keyValue = StringUtils.split(line, "\t", 2);
        try {
          if (keyValue[0].equals("listeners")) {
            listeners = Integer.parseInt(keyValue[1]);
          } else if (keyValue[0].equals("duration")) {
            duration = Integer.parseInt(keyValue[1]);
          } else if (keyValue[0].equals("avg")) {
            avg = Integer.parseInt(keyValue[1]);
          }
        } catch (NumberFormatException e) {
          log.warn("corrupted entry for " + date, e);
        }
      }
      return new DailySummary(cal.getTime(), listeners, duration, avg);
    } else {
      // try to estimate duration based on listeners log
      List<ListenersEntry> listenersEntries = this.getListenersOf(day);
      if (listenersEntries.size() > 0) {
        int tlm = 0;

        for (int i = 0; i < listenersEntries.size(); i++) {
          ListenersEntry entry = listenersEntries.get(i);

          int intervalLength = 10;
          if (i + 1 < listenersEntries.size()) {
            long startNext = listenersEntries.get(i + 1).getTime().getTime();
            int minutes = (int) ((startNext - entry.getTime().getTime()) / 60000);
            if (minutes < 10) {
              intervalLength = minutes;
            }
          }
          tlm += intervalLength * entry.getListeners();
        }
        return new DailySummary(cal.getTime(), tlm);
      }
    }

    return null;
  }

  public List<ListenersEntry> getListenersOf(Date day) throws IOException {
    boolean today = this.isToday(day);
    if (today && this.listenersToday != null && this.listenersToday.size() > 0) {
      if (this.isToday(listenersToday.get(0).getTime())) {
        return this.listenersToday;
      } else {
        // day change - discard old list
        this.listenersToday = null;
      }
    }

    String raw = this.getRawListenerStatsOf(day);
    List<ListenersEntry> entries = new ArrayList<ListenersEntry>();
    String[] lines = StringUtils.split(raw, "\r\n");
    SimpleDateFormat fmt = new SimpleDateFormat(TIME_FORMAT);
    for (String line : lines) {
      String[] cols = StringUtils.split(line.trim(), "\t");
      if (cols.length == 2) {
        try {
          Date date = fmt.parse(cols[0]);
          int listeners = Integer.parseInt(cols[1]);
          ListenersEntry entry = new ListenersEntry(date, listeners);
          entries.add(entry);
        } catch (Exception e) {
          e.printStackTrace();
        }

      }
    }
    if (today) {
      this.listenersToday = entries;
    }
    return entries;
  }

  private boolean isToday(Date day) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(System.currentTimeMillis());
    int today = cal.get(Calendar.DAY_OF_YEAR);
    cal.setTime(day);
    return cal.get(Calendar.DAY_OF_YEAR) == today;

  }

  /**
   * @see de.stationadmin.base.Service#synchronize()
   */
  @Override
  public void synchronize() throws IOException {
    // nothing to do
  }

  /**
   * @see de.stationadmin.base.Service#close()
   */
  @Override
  public void close() {

  }

  /**
   * @see de.stationadmin.base.Service#initBackgroundTasks()
   */
  @Override
  public void initBackgroundTasks() {
    this.ctx.getStationStatus().addPropertyChangeListener("currentListeners", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (listenersToday != null) {
          listenersToday.add(new ListenersEntry(new Date(System.currentTimeMillis()), ctx.getStationStatus().getCurrentListeners()));
        }
      }
    });

    this.ctx.getStationStatus().addPropertyChangeListener("currentTitleId", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (playsToday != null) {
          try {
            int titleId = ctx.getStationStatus().getCurrentTrackId();
            if (playsToday.size() > 0 && playsToday.get(playsToday.size() - 1).getTrack().getId() == titleId) {
              // might happen during start up - just ignore the event
              return;
            }
            playsToday.add(new Play(new Date(System.currentTimeMillis()), titleRegistry.getTrack(titleId)));
          } catch (Exception e) {
            log.error(e);
          }
        }
      }
    });

    this.ctx.getStationStatus().addPropertyChangeListener("listenersYesterday", new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        try {
          writeDailyStatistics();
        } catch (Exception e) {
          log.error("failed to write daily summary", e);
        }
      }

    });

  }

  private void writeDailyStatistics() throws IOException {
    if (this.ctx.getStationStatus().getListenersYesterday() == 0) {
      return;
    }
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(System.currentTimeMillis());
    cal.add(Calendar.DAY_OF_MONTH, -1);

    String date = new SimpleDateFormat(DATE_FORMAT).format(cal.getTime());
    String filename = this.logCacheDir + "station_dailysummary" + "-" + date + ".log";

    if (!new File(filename).exists()) {
      StringBuilder buf = new StringBuilder();
      buf.append("listeners\t" + this.ctx.getStationStatus().getListenersYesterday() + "\n");
      buf.append("duration\t" + this.ctx.getStationStatus().getDurationYesterday() + "\n");
      buf.append("avg\t" + this.ctx.getStationStatus().getAvgListeningTimeYesterday() + "\n");
      FileUtils.writeStringToFile(new File(filename), buf.toString());
    }

  }

}
