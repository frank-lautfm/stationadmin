/**
 * 
 */
package de.stationadmin.base.statistics;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimerTask;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import de.stationadmin.base.Service;
import de.stationadmin.base.SessionCtx;
import de.stationadmin.base.Settings;
import de.stationadmin.base.StationStatus;
import de.stationadmin.lfm.backend.Statistics;

/**
 * @author Frank
 *
 */
public class StatisticsService implements Service {
  private static final Logger log = Logger.getLogger(StatisticsService.class);
  private SessionCtx sessionCtx;
  private ListenerStatsHistory listenerStatsHistory = new ListenerStatsHistory();
  private TimerTask statsRefresherTask = null;
  private Settings settings;
  private PropertyChangeListener onTitleChangeRefresher;

  /**
   * @param ctx
   */
  public StatisticsService(SessionCtx ctx, Settings settings) {
    this.sessionCtx = ctx;
    this.settings = settings;

    this.listenerStatsHistory.setLogRank(settings.isLogRank());
    settings.addPropertyChangeListener("logRank", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        listenerStatsHistory.setLogRank(StatisticsService.this.settings.isLogRank());
      }
    });

    initSettingsObserver(settings);
  }

  /**
   * @see de.stationadmin.base.Service#close()
   */
  @Override
  public void close() {
  }

  void configureStatsRefresher() {
    if (this.onTitleChangeRefresher == null) {
      this.onTitleChangeRefresher = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          try {
            refreshStatistics();
          } catch (IOException e) {
            log.error("failed to refresh statistics", e);
          }
        }

      };
    }

    if (this.statsRefresherTask != null) {
      this.statsRefresherTask.cancel();
      this.statsRefresherTask = null;
    }
    if (this.settings.getStatisticsRefreshInterval() > 0) {
      log.info("configure statistics refresher task");
      this.statsRefresherTask = new StatsRefresher();
      this.sessionCtx.getTimer().schedule(this.statsRefresherTask, 0, this.settings.getStatisticsRefreshInterval() * 1000 * 60);
      this.sessionCtx.getStationStatus().removePropertyChangeListener("currentTitleId", this.onTitleChangeRefresher);
    }
    if (this.settings.getStatisticsRefreshInterval() < 0) {
      // sychronize with start of title
      this.sessionCtx.getStationStatus().addPropertyChangeListener("currentTitleId", this.onTitleChangeRefresher);
    }
  }

  /**
   * Gets the history for listener statistics
   * 
   * @return history
   */
  public ListenerStatsHistory getListenerStatsHistory() {
    return listenerStatsHistory;
  }

  private void initSettingsObserver(Settings settings) {
    settings.addPropertyChangeListener("statisticsRefreshInterval", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        configureStatsRefresher();
      }

    });

    settings.addPropertyChangeListener("statisticsLogFile", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        listenerStatsHistory.setLogFile(StringUtils.trimToNull((String) evt.getNewValue()));
      }

    });

  }

  /**
   * Initializes the recording of statistics
   */
  public void initStatisticsRecording() {
    this.sessionCtx.updateStatus("initStatsRecording");
    try {
      this.configureStatsRefresher();
    } finally {
      this.sessionCtx.updateStatus(null);
    }
  }

  /**
   * @see de.stationadmin.base.Service#load()
   */
  @Override
  public void load() throws IOException {
  }

  /**
   * Retrieves the latest stastitics from the laut.fm server (including current listeners)
   * 
   * @throws IOException
   */
  public void refreshStatistics() throws IOException {
    // log.debug("refresh statistics");
    Statistics stats = sessionCtx.getServer().getStatistics(sessionCtx.getStationId());
    StationStatus stationStatus = this.sessionCtx.getStationStatus();
    stationStatus.setCurrentListeners(stats.getListenersNow());
    stationStatus.setRank(stats.getPositionNow());

    DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(System.currentTimeMillis());
    String today = fmt.format(cal.getTime());
    cal.add(Calendar.DAY_OF_MONTH, -1);
    String yesterday = fmt.format(cal.getTime());

    int listenersToday = stats.getSwitchonsLog().containsKey(today) ? stats.getSwitchonsLog().get(today) : 0;
    int tlhToday = stats.getTlhLog().containsKey(today) ? stats.getTlhLog().get(today) : 0;

    int listenersYesterday = stats.getSwitchonsLog().containsKey(yesterday) ? stats.getSwitchonsLog().get(yesterday) : 0;
    int tlhYesterday = stats.getTlhLog().containsKey(yesterday) ? stats.getTlhLog().get(yesterday) : 0;

    stationStatus.setAvgListeningTimeYesterday(listenersYesterday > 0 ? tlhYesterday * 60 / listenersYesterday : 0);
    stationStatus.setListenersYesterday(listenersYesterday);
    stationStatus.setAvgListeningTimeToday(listenersToday > 0 ? tlhToday * 60 / listenersToday : 0);
    stationStatus.setListenersToday(listenersToday);
    stationStatus.setDurationToday(tlhToday);
    stationStatus.setDurationYesterday(tlhYesterday);

    // add current listeners to history
    this.listenerStatsHistory.add(stats.getListenersNow(), stats.getPositionNow());
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.emjoy.stationadmin.base.Service#startBackgrounTasks()
   */
  @Override
  public void initBackgroundTasks() {
    this.initStatisticsRecording();
  }

  /**
   * @see de.stationadmin.base.Service#synchronize()
   */
  @Override
  public void synchronize() throws IOException {
  }

  private class StatsRefresher extends TimerTask {

    /**
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
      try {
        refreshStatistics();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

}
