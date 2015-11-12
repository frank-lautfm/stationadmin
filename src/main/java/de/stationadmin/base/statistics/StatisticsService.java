/**
 * 
 */
package de.stationadmin.base.statistics;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.TimerTask;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import de.stationadmin.base.Service;
import de.stationadmin.base.SessionCtx;
import de.stationadmin.base.Settings;

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
      this.sessionCtx.getTimer().schedule(this.statsRefresherTask, 0,
          this.settings.getStatisticsRefreshInterval() * 1000 * 60);
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
   * Retrieves the latest stastitics from the laut.fm server (including current
   * listeners)
   * 
   * @throws IOException
   */
  public void refreshStatistics() throws IOException {
    // log.debug("refresh statistics");
    // Statistics stats = new Statistics(); // FIXME
    // this.sessionCtx.getServer().getStatistics();
    // StationStatus stationStatus = this.sessionCtx.getStationStatus();
    // stationStatus.setCurrentListeners(stats.getCurrentListeners());
    // stationStatus.setRank(stats.getRank());
    // stationStatus.setAvgListeningTimeYesterday(stats.getAvgListeningTimeYesterday());
    // stationStatus.setListenersYesterday(stats.getListenersYesterday());
    // stationStatus.setAvgListeningTimeToday(stats.getAvgListeningTimeToday());
    // stationStatus.setListenersToday(stats.getListenersToday());
    // stationStatus.setDurationToday(stats.getDurationToday());
    // stationStatus.setDurationYesterday(stats.getDurationYesterday());

    // add current listeners to history
    // this.listenerStatsHistory.add(stats.getCurrentListeners(), stats.getRank());
  }

  /* (non-Javadoc)
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
