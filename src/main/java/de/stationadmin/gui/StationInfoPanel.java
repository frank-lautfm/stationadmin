package de.stationadmin.gui;

import javax.swing.JPanel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.gui.schedule.ScheduleTodayPanel;
import de.stationadmin.gui.statistic.ListenerStatisticsPanel;
import de.stationadmin.gui.statistic.PlaylistStatisticsPanel;
import de.stationadmin.gui.statistic.StatisticsTodayPanel;
import de.stationadmin.gui.statistic.StatisticsYesterdayPanel;
import de.stationadmin.gui.track.TrackHistoryPanel;
import de.stationadmin.gui.util.TitledPanel;

/**
 * Container that displays basic station information like recent titles and statistics
 * 
 * @author Frank Korf
 */
public class StationInfoPanel extends JPanel {
  private static final long serialVersionUID = -4175293343746402668L;

  private ClientContext ctx;

  public StationInfoPanel(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.init();
  }

  private void init() {
    this.setLayout(new FormLayout("1dlu,min(pref;200dlu),2dlu,50dlu:grow,2dlu,pref,1dlu", "100dlu:grow"));
    CellConstraints cc = new CellConstraints();
    TrackHistoryPanel titleHistory = new TrackHistoryPanel(ctx);
    this.add(titleHistory, cc.xy(4, 1, CellConstraints.FILL, CellConstraints.FILL));

    if (!ctx.getAdminClient().getSessionCtx().isDJOnly()) {
      JPanel leftPanel = new JPanel(new FormLayout("pref", "pref:grow"));
      this.add(leftPanel, cc.xy(2, 1, CellConstraints.FILL, CellConstraints.FILL));

      TitledPanel schedule = new TitledPanel(ctx.getString("schedule.title"), new ScheduleTodayPanel(ctx.getAdminClient().getSchedule(), ctx.getJumpHandler()));
      leftPanel.add(schedule, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
    }

    JPanel rightPanel = new JPanel(new FormLayout("pref", "pref:grow,5dlu,pref:grow,5dlu,pref:grow,5dlu,pref:grow"));
    this.add(rightPanel, cc.xy(6, 1, CellConstraints.FILL, CellConstraints.FILL));

    if (!ctx.getAdminClient().getSessionCtx().isDJOnly()) {

      TitledPanel stats = new TitledPanel(ctx.getString("statistics.current.title"), new ListenerStatisticsPanel(ctx));
      rightPanel.add(stats, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));

      TitledPanel statsToday = new TitledPanel(ctx.getString("statistics.today.title"), new StatisticsTodayPanel(ctx));
      rightPanel.add(statsToday, cc.xy(1, 3, CellConstraints.FILL, CellConstraints.FILL));

      TitledPanel statsYesterday = new TitledPanel(ctx.getString("statistics.yesterday.title"), new StatisticsYesterdayPanel(ctx));
      rightPanel.add(statsYesterday, cc.xy(1, 5, CellConstraints.FILL, CellConstraints.FILL));

      TitledPanel statsPlaylist = new TitledPanel(ctx.getString("playliststats.title"), new PlaylistStatisticsPanel(ctx));
      rightPanel.add(statsPlaylist, cc.xy(1, 7, CellConstraints.FILL, CellConstraints.FILL));
    }

  }

}
