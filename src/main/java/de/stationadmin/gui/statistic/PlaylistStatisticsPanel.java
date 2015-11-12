/**
 * 
 */
package de.stationadmin.gui.statistic;

import java.awt.Color;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.beans.BeanAdapter;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.schedule.Schedule;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.gui.ClientContext;

/**
 *
 * @author Frank Korf
 *
 */
public class PlaylistStatisticsPanel extends JPanel {
  private static final long serialVersionUID = 5752235067680615947L;
  private ClientContext ctx;

  public PlaylistStatisticsPanel(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.init();
  }
  
  private void init() {
    this.setLayout(new FormLayout("5dlu,pref,10dlu:grow,pref,5dlu","5dlu,pref,7dlu,pref,10dlu,pref,7dlu,pref,5dlu:grow"));
    CellConstraints cc = new CellConstraints();
    
    ValueModel numPlaylistsModel = new BeanAdapter<PlaylistRegistry>(ctx.getAdminClient().getPlaylistRegistry(), true).getValueModel("numPlaylists");
    ValueModel numTitlesModel = new BeanAdapter<TrackRegistry>(ctx.getAdminClient().getTitleRegistry(), true).getValueModel("numTracks");

    ValueModel numPlaylistsUsedModel = new BeanAdapter<Schedule>(ctx.getAdminClient().getSchedule(), true).getValueModel("numPlaylists");
    ValueModel numTitlesUsedModel = new BeanAdapter<Schedule>(ctx.getAdminClient().getSchedule(), true).getValueModel("numTracks");

    int row = 2;
    this.setBackground(Color.WHITE);

    NumberFormat nf = NumberFormat.getInstance();
    nf.setGroupingUsed(false);

    this.add(new JLabel(ctx.getString("playliststats.property.numPlaylists")), cc.xy(2, row));
    this.add(BasicComponentFactory.createLabel(numPlaylistsModel, nf), cc.xy(4, row, CellConstraints.RIGHT, CellConstraints.CENTER));
    row += 2;
    
    this.add(new JLabel(ctx.getString("playliststats.property.numTitles")), cc.xy(2, row));
    this.add(BasicComponentFactory.createLabel(numTitlesModel, nf), cc.xy(4, row, CellConstraints.RIGHT, CellConstraints.CENTER));
    row += 2;

    this.add(new JLabel(ctx.getString("playliststats.property.numPlaylistsUsed")), cc.xy(2, row));
    this.add(BasicComponentFactory.createLabel(numPlaylistsUsedModel, nf), cc.xy(4, row, CellConstraints.RIGHT, CellConstraints.CENTER));
    row += 2;
    
    this.add(new JLabel(ctx.getString("playliststats.property.numTitlesUsed")), cc.xy(2, row));
    this.add(BasicComponentFactory.createLabel(numTitlesUsedModel, nf), cc.xy(4, row, CellConstraints.RIGHT, CellConstraints.CENTER));
    row += 2;

  }
  
}
