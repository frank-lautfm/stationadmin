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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.stationadmin.base.AccessDeniedException;
import de.stationadmin.base.Service;
import de.stationadmin.base.SessionCtx;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.base.track.TrackService;
import de.stationadmin.lfm.backend.ListenerStatsEntry;
import de.stationadmin.lfm.backend.ListenerStatsPeriod;
import de.stationadmin.lfm.backend.ListenerStatsSource;
import de.stationadmin.lfm.backend.TrackStatsEntry;

/**
 * @author korf
 * 
 */
public class LogAnalyzerService implements Service {
  public static final int DAY_IN_MS = 1000 * 60 * 60 * 24;
  private static final Logger log = LogManager.getLogger(LogAnalyzerService.class);
  private static final String DATE_FORMAT = "yyyy-MM-dd";
  private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
  private SessionCtx ctx;
  private TrackService trackService;
  private TrackRegistry trackRegistry;
  private String logCacheDir;

  private List<ListenersEntry> listenersToday;
  private List<Play> playsToday;

  private TrackStatsEntry[] bufferedEntries;
  private int bufferedEntriesDays = 0;

  private long lastServerRequest;
  private int serverRequestCnt;

  public LogAnalyzerService(SessionCtx ctx, TrackService trackService) {
    this.ctx = ctx;
    this.trackService = trackService;
    this.trackRegistry = trackService.getTrackRegistry();
    this.logCacheDir = ctx.getStationDirectory() + "log" + File.separatorChar;

  }

  /**
   * @see de.stationadmin.base.Service#load()
   */
  @Override
  public void load() {
    if (ctx.isDJOnly()) {
      return;
    }
    ctx.updateStatus("loadLogstatistics");
    Thread t = new Thread() {

      @Override
      public void run() {
        // make sure statistics of past 6 days are downloaded
        for (int i = 31; i > 0; i--) {
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
    if (file.exists() && file.length() > 0) {
      return FileUtils.readFileToString(file, "UTF-8");
    }

    Calendar calDay = Calendar.getInstance();
    Calendar calEntry = Calendar.getInstance();
    calDay.setTime(day);

    int numLiveTracksBefore = this.trackRegistry.getNumLiveTracks();

    if (day.getTime() > System.currentTimeMillis() - (DAY_IN_MS * 100l)) {

      SimpleDateFormat timeFmt = new SimpleDateFormat(TIME_FORMAT);

      // fetch from server
      log.info("download " + type + " for " + date);

      int numDays = (int) ((System.currentTimeMillis() - day.getTime()) / DAY_IN_MS) + 2;
      if (numDays > 7) {
        numDays = 7;
      }

      TrackStatsEntry[] stats = null;

      // try get by day
      if (System.currentTimeMillis() - this.lastServerRequest < 1000 * 60) {
        if (this.serverRequestCnt > 30) {
          try {
            // add a delay in order to avoid flooding server with requests
            Thread.sleep(100);
          } catch (Exception e) {
          }
        }
      }
      else {
        this.serverRequestCnt = 0;
      }
      stats = this.ctx.getServer().getTrackStatisticsByDate(this.ctx.getStationId(), day);

      this.serverRequestCnt++;
      this.lastServerRequest = System.currentTimeMillis();

      // on failure: try get from recent stats
      if (stats == null || stats.length == 0) {

        if (bufferedEntries != null && bufferedEntriesDays >= numDays && !isToday(day)) {
          stats = bufferedEntries;
        } else {
          stats = this.ctx.getServer().getTrackStatistics(this.ctx.getStationId(), numDays);
          Arrays.sort(stats, new Comparator<TrackStatsEntry>() {

            @Override
            public int compare(TrackStatsEntry o1, TrackStatsEntry o2) {
              long d1 = o1.getStartedAt() != null ? o1.getStartedAt().getTime() : Long.MAX_VALUE;
              long d2 = o2.getStartedAt() != null ? o2.getStartedAt().getTime() : Long.MAX_VALUE;
              return Long.compare(d1, d2);
            }
          });

          bufferedEntries = stats;
          bufferedEntriesDays = numDays;
        }
      }
      StringBuilder buf = new StringBuilder();
      for (TrackStatsEntry entry : stats) {
        calEntry.setTime(entry.getStartedAt());
        if ((entry.isLive() || entry.getId() > 0) && calDay.get(Calendar.DAY_OF_MONTH) == calEntry.get(Calendar.DAY_OF_MONTH)
            && calDay.get(Calendar.MONTH) == calEntry.get(Calendar.MONTH)) {

          buf.append(timeFmt.format(entry.getStartedAt()));
          buf.append('\t');
          if (type.equals("station_listeners")) {
            buf.append(entry.getListeners());
          } else {
            if (!entry.isLive()) {
              buf.append(entry.getId());
            } else {
              int id = this.trackRegistry.registerLiveTrack(entry.getArtistName(), entry.getTitle());
              buf.append("L" + id);
            }
          }
          buf.append('\n');
        }
      }
      String content = buf.toString();

      if (this.trackRegistry.getNumLiveTracks() > numLiveTracksBefore) {
        try {
          this.trackService.saveLiveTracks();
        } catch (Exception e) {
          log.error("Error while saving live tracks: ", e);
        }
      }

      if (!isToday(day)) {
        // write data to cache
        FileUtils.writeStringToFile(file, content, "UTF-8");
      }

      return content;
    } else

    {
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
    checkAccess();
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
          boolean isLiveTrack = cols[1].length() > 1 && cols[1].charAt(0) == 'L';
          int trackId = isLiveTrack ? Integer.parseInt(cols[1].substring(1)) : Integer.parseInt(cols[1]);
          BasicTrack track = isLiveTrack ? this.trackRegistry.getLiveTrack(trackId) : this.trackRegistry.getTrack(trackId);
          if (track == null) {
            track = this.trackRegistry.getByLegacyId(trackId);
          }
          if (track == null) {
            track = new DetailedTrack();
            track.setId(trackId);
            track.setArtist("<unknown>");
            track.setTitle("<Track " + trackId + ">");
          }
          Play play = new Play(date, track);
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
    checkAccess();
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
    checkAccess();
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
    checkAccess();
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
    checkAccess();
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
    checkAccess();
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
      int listeners = 0, duration = 0, avg = 0, uniqs = -1;
      for (String line : StringUtils.split(raw, "\n")) {
        String[] keyValue = StringUtils.split(line, "\t", 2);
        try {
          if (keyValue[0].equals("listeners")) {
            listeners = Integer.parseInt(keyValue[1]);
          } else if (keyValue[0].equals("duration")) {
            duration = Integer.parseInt(keyValue[1]);
            if (cal.getTimeInMillis() > 1481806854618l) {
              duration = duration * 60;
            }
            // else: Stats from Station Admin 3 have been in minutes anyway
          } else if (keyValue[0].equals("avg")) {
            avg = Integer.parseInt(keyValue[1]);
          } else if (keyValue[0].equals("uniqs")) {
            uniqs = Integer.parseInt(keyValue[1]);
          }
        } catch (NumberFormatException e) {
          log.warn("corrupted entry for " + date, e);
        }
      }
      return new DailySummary(cal.getTime(), listeners, duration, avg, uniqs);
    } else {
      // try to estimate duration based on listeners log
      List<ListenersEntry> listenersEntries = this.getListenersOf(day);
      if (listenersEntries.size() > 0) {
        double tlm = 0;

        for (int i = 0; i < listenersEntries.size(); i++) {
          ListenersEntry entry = listenersEntries.get(i);
          if (i + 1 < listenersEntries.size()) {
            long startNext = listenersEntries.get(i + 1).getTime().getTime();

            int diffMs = (int) (startNext - entry.getTime().getTime());
            double minutes = Math.round((double) diffMs / 60000);
            tlm += minutes * entry.getListeners();
          } else {
            tlm += 3 * entry.getListeners();
          }
        }
        return new DailySummary(cal.getTime(), (int) tlm);
      }
    }

    return null;
  }

  public List<MonthlySummary> getMonthlySummary() throws IOException {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, 2011);
    cal.set(Calendar.MONTH, Calendar.JANUARY);
    cal.set(Calendar.DAY_OF_MONTH, 1);
    ListenerStatsEntry[] entries = ctx.getServer().getListenerStatistics(ctx.getStationId(), cal.getTime(), new Date(), ListenerStatsPeriod.Monthly);
    ArrayList<MonthlySummary> summaries = new ArrayList<>();
    for (ListenerStatsEntry entry : entries) {
      summaries.add(new MonthlySummary(entry));
    }
    return summaries;
  }

  public List<ListenersEntry> getListenersOf(Date day) throws IOException {
    checkAccess();
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

  private void checkAccess() {
    if (this.ctx.isDJOnly()) {
      throw new AccessDeniedException();
    }

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

    this.ctx.getStationStatus().addPropertyChangeListener("currentTrackId", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (playsToday != null) {
          try {
            int titleId = ctx.getStationStatus().getCurrentTrackId();
            if (playsToday.size() > 0 && playsToday.get(playsToday.size() - 1).getTrack().getId() == titleId) {
              // might happen during start up - just ignore the event
              return;
            }
            if (titleId > 0) {
              playsToday.add(new Play(new Date(System.currentTimeMillis()), trackRegistry.getTrack(titleId)));
            }
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

    // Statistics stats = ctx.getServer().getStatistics(ctx.getStationId());
    Date today = new Date();
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, -31);
    ListenerStatsEntry[] listenerStats = ctx.getServer().getListenerStatistics(ctx.getStationId(), cal.getTime(), today, ListenerStatsPeriod.Daily);

    cal.setTimeInMillis(System.currentTimeMillis());

    SimpleDateFormat dateFmt = new SimpleDateFormat(DATE_FORMAT);
    for (int i = 0; i < 31; i++) {
      cal.add(Calendar.DAY_OF_MONTH, -1);
      String date = dateFmt.format(cal.getTime());
      String filename = this.logCacheDir + "station_dailysummary" + "-" + date + ".log";
      File file = new File(filename);
      if (i > 3 && file.exists()) {
        // we have already data and it shouldn't change anymore
        continue;
      }

      for (int j = 0; j < listenerStats.length; j++) {
        if (dateFmt.format(listenerStats[j].getDate()).equals(date) && listenerStats[j].getSources().containsKey("all")) {
          ListenerStatsSource all = listenerStats[j].getSources().get("all");

          int avg = all.getSessions() > 0 ? all.getTlh() * 60 / all.getSessions() : 0;

          StringBuilder buf = new StringBuilder();
          buf.append("listeners\t" + all.getSessions() + "\n");
          buf.append("duration\t" + all.getTlh() + "\n");
          buf.append("avg\t" + avg + "\n");
          buf.append("uniqs\t" + all.getUniqs() + "\n");
          FileUtils.writeStringToFile(file, buf.toString(), "UTF-8");
          break;
        }
      }

    }

  }

}
