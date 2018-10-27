package de.stationadmin.gui.track;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.track.TrackHistory.Entry;
import de.stationadmin.base.util.TimeFormat;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.playlist.PlaylistEntryJumpTarget;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.TitledPanel;

public class TrackHistoryPanel extends TitledPanel {
  private static final long serialVersionUID = -4259795072616140767L;
  private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyy HH:mm");
  private ClientContext ctx;
  private JPanel historyContainer = new JPanel(new GridBagLayout());
  private Color background = AppUtils.getTextBackgroundColor();

  // private JProgressBar titleProgressBar;

  public TrackHistoryPanel(ClientContext ctx) {
    super(ctx.getTextProvider().getString("titlehistory.title"), toScrollPane(new JPanel()));
    this.ctx = ctx;
    this.ctx.getAdminClient().getStationStatus()
        .addPropertyChangeListener("currentTrackLabel", new PropertyChangeListener() {

          @Override
          public void propertyChange(PropertyChangeEvent evt) {
            refresh();
          }
        });
    this.refresh();
    JPanel contentContainer = (JPanel) ((JScrollPane) this.getContentContainer()).getViewport().getView();
    // contentContainer.setBackground(Color.WHITE);
    contentContainer.setBackground(background);
    contentContainer.setOpaque(true);
    contentContainer.setLayout(new FormLayout("5dlu,pref:grow,5dlu,", "5dlu,pref:grow,5dlu"));
    contentContainer.add(this.historyContainer, new CellConstraints(2, 2, CellConstraints.LEFT, CellConstraints.TOP));
  }

  private static JScrollPane toScrollPane(JPanel panel) {
    JScrollPane scroll = new JScrollPane(panel);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    return scroll;
  }

  protected void refresh() {
    this.historyContainer.removeAll();
    this.historyContainer.setBackground(background);
    this.historyContainer.setOpaque(true);

    // heading
    JLabel headLabel = new JLabel("Titel");
    headLabel.setFont(ComponentFactory.boldLabelFont);

    GridBagConstraints cc = new GridBagConstraints();
    cc.gridx = 1;
    cc.gridy = 0;
    cc.anchor = GridBagConstraints.WEST;
    cc.insets = new Insets(10, 0, 10, 30);

    this.historyContainer.add(headLabel, cc);

    JLabel listenerLabel = new JLabel("Hörer");
    listenerLabel.setFont(ComponentFactory.boldLabelFont);

    cc = new GridBagConstraints();
    cc.gridx = 2;
    cc.gridy = 0;
    cc.anchor = GridBagConstraints.WEST;
    cc.insets = new Insets(10, 0, 10, 10);
    this.historyContainer.add(listenerLabel, cc);

    // this.titleProgressBar = null;

    List<Entry> entries = this.ctx.getAdminClient().getTrackService().getTrackHistory().getEntries();
    int row = 1;
    for (int i = entries.size() - 1; i >= 0 && i >= entries.size() - 100; i--) {
      Entry entry = entries.get(i);

      // Color color = (i % 2 == 1) ? new Color(245, 245, 255) : Color.WHITE;

      boolean isCurrrentTitle = (entry.getTitle().getId() == this.ctx.getAdminClient().getStationStatus()
          .getCurrentTrackId());
      // int entryRows = isCurrrentTitle ? 3 : 2;
      int entryRows = 2;
      int bottomSpace = isCurrrentTitle ? 15 : 3;

      JPanel entryPanel = new JPanel(new GridLayout(entryRows, 1));
      entryPanel.putClientProperty("entry", entry);
      entryPanel.setBackground(background);
      JLabel dateLabel = new JLabel(dateFormat.format(entry.getDate()));
      dateLabel.setFont(ComponentFactory.defaultLabelFontSmall);
      entryPanel.add(dateLabel);
      String str = entry.getTitle().getArtist() + " - " + entry.getTitle().getTitle();
      if (entry.getTitle().getLength() > 0) {
        str += " (" + TimeFormat.format(entry.getTitle().getLength(), false) + ")";
      }
      JLabel titleLabel = new JLabel(str);
      entryPanel.add(titleLabel);
      // if (isCurrrentTitle) {
      // int min = this.progressBarTime(entry.getDate().getTime());
      // int max =
      // this.progressBarTime(ctx.getAdminClient().getStationStatus().getCurrentTitleEndTime());
      // int current = this.progressBarTime(System.currentTimeMillis());
      // if (min <= current && current <= max) {
      // this.titleProgressBar = new JProgressBar(min, max);
      // this.titleProgressBar.setValue(current);
      // entryPanel.add(this.titleProgressBar);
      // } else {
      // System.out.println(min + " < " + current + " < " + max);
      // }
      // }
      //

      if (isCurrrentTitle) {
        entryPanel.addMouseListener(new MouseAdapter() {

          @Override
          public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
              Entry entry = (Entry) ((JComponent) e.getSource()).getClientProperty("entry");
              if (entry != null) {
                int playlistId = ctx.getAdminClient().getSchedule().getCurrent().getPlaylistId();
                Playlist playlist = ctx.getAdminClient().getPlaylistService().getPlaylistRegistry()
                    .getPlaylist(playlistId);
                ctx.getJumpHandler().jumpTo(new PlaylistEntryJumpTarget(playlist, entry.getTitle()));
              }
            }
          }

        });
      }

      cc = new GridBagConstraints();
      cc.gridx = 1;
      cc.gridy = row;
      cc.fill = GridBagConstraints.BOTH;
      cc.anchor = GridBagConstraints.WEST;
      cc.insets = new Insets(10, 0, bottomSpace, 30);
      this.historyContainer.add(entryPanel, cc);

      int listeners = this.ctx.getAdminClient().getStatisticsService().getListenerStatsHistory().getListenersAt(entry.getDate().getTime());
      cc = new GridBagConstraints();
      cc.gridx = 2;
      cc.gridy = row;
      cc.anchor = GridBagConstraints.SOUTHEAST;
      cc.insets = new Insets(2, 0, bottomSpace, 10);

      JPanel listenerPanel = new JPanel(new FormLayout("pref:grow", "pref:grow"));
      listenerPanel.setBackground(background);
      listenerPanel.setOpaque(true);
      if (listeners > -1) {
        listenerPanel.add(new JLabel(Integer.toString(listeners)), new CellConstraints(1, 1, CellConstraints.RIGHT,
            CellConstraints.BOTTOM));
      }

      this.historyContainer.add(listenerPanel, cc);
      row++;
    }

    this.invalidate();
    this.repaint();
  }

  int progressBarTime(long time) {
    time = time / 1000;
    time = time % 100000;
    return (int) time;
  }

}
