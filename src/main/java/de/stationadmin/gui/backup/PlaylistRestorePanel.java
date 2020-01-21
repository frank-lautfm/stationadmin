/**
 * 
 */
package de.stationadmin.gui.backup;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.backup.BackupService;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.PlaylistNameCompator;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.ThreadedAction;

/**
 * 
 * @author Frank Korf
 * 
 */
public class PlaylistRestorePanel extends JPanel {
  private static final long serialVersionUID = 333728681978983348L;

  private File file;
  private boolean legacyBackup = false;

  private TextProvider textProvider;
  private StationAdminClient adminClient;
  private Map<Integer, JCheckBox> playlistCbs = new HashMap<Integer, JCheckBox>();
  private ValueHolder profileImport = new ValueHolder(Boolean.FALSE);

  /**
   * @param file
   * @param rootWindow
   * @param textProvider
   * @param adminClient
   */
  public PlaylistRestorePanel(TextProvider textProvider, StationAdminClient adminClient, File file) {
    super();
    this.file = file;
    this.legacyBackup = file.lastModified() < StationAdminClient.TIMESTAMP_RADIOADMIN_SWITCH;
    this.textProvider = textProvider;
    this.adminClient = adminClient;
    this.init();
    this.readAvailablePlaylists();
  }

  private void init() {

    this.setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,50dlu:grow,5dlu,pref,5dlu,pref,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    {
      JPanel head = new JPanel(new FlowLayout(FlowLayout.LEADING));
      head.add(new JLabel(textProvider.getString("backup.restore.playlist.head")));
      this.add(head, cc.xy(2, 2));
    }

    {
      JPanel playlistPanel = new JPanel(new GridLayout(-1, 2));
      playlistPanel.setBackground(Color.WHITE);
      List<Playlist> playlists = adminClient.getPlaylistService().getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE);
      Collections.sort(playlists, new PlaylistNameCompator());
      for (Playlist playlist : playlists) {
        JCheckBox cb = new JCheckBox(playlist.getName());
        cb.setBackground(Color.WHITE);
        cb.putClientProperty("playlist", playlist);
        cb.setEnabled(false);
        playlistPanel.add(cb);
        this.playlistCbs.put(playlist.getId(), cb);
      }

      this.add(new JScrollPane(playlistPanel), cc.xy(2, 4, CellConstraints.FILL, CellConstraints.TOP));
    }

    {
      JToolBar toolbar = new JToolBar();
      toolbar.setFloatable(false);

      JButton selectAll = new JButton(textProvider.getString("backup.restore.playlist.selectall"));
      selectAll.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          for (JCheckBox cb : playlistCbs.values()) {
            cb.setSelected(true);
          }
        }
      });
      JButton selectNone = new JButton(textProvider.getString("backup.restore.playlist.selectnone"));
      selectNone.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          for (JCheckBox cb : playlistCbs.values()) {
            cb.setSelected(false);
          }
        }
      });

      toolbar.add(selectAll);
      toolbar.add(selectNone);

      this.add(toolbar, cc.xy(2, 6));

    }

    {
      JCheckBox cbProfile = BasicComponentFactory.createCheckBox(profileImport, textProvider.getString("backup.restore.playlist.profiles"));
      this.add(cbProfile, cc.xy(2, 8));
      try {
        cbProfile.setEnabled(adminClient.getBackupService().checkPlaylistProfileAvailability(file));
      } catch (Exception e) {
        cbProfile.setEnabled(false);
      }
    }

    {
      JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));

      JButton importBtn = new JButton(new ImportAction());
      bottom.add(importBtn);

      bottom.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
      this.add(bottom, cc.xy(2, 10));

    }

  }

  private void readAvailablePlaylists() {
    try {
      BackupService tool = adminClient.getBackupService();
      Map<Integer, Boolean> availability = tool.checkPlaylistBackupAvailability(this.file);

      for (Entry<Integer, Boolean> entry : availability.entrySet()) {
        if (entry.getValue()) {
          JCheckBox cb = this.playlistCbs.get(entry.getKey());
          if (cb != null) {
            cb.setEnabled(true);
          }
        }
      }

    } catch (IOException e) {
      JXErrorPane.showDialog(AppUtils.getRootFrame(), this.textProvider.createErrorInfo(e, "backup.restore.playlist.msg.readerror"));
    }

  }

  private class ImportAction extends ThreadedAction {
    private static final long serialVersionUID = -2056523148091339404L;
    private volatile String status = "";
    private volatile int cnt;

    /**
     * @param ctx
     */
    public ImportAction() {
      super(getRootPane());
      this.putValue(Action.NAME, textProvider.getString("backup.restore.import"));
    }

    /**
     * @see de.stationadmin.gui.util.ThreadedAction#getStatus()
     */
    @Override
    protected String getStatus() {
      return this.status;
    }

    /**
     * @see de.stationadmin.gui.util.ThreadedAction#onSuccess()
     */
    @Override
    protected void onSuccess() {
      if (cnt > 0) {
        JOptionPane.showMessageDialog(AppUtils.getRootFrame(), textProvider.getString("backup.restore.playlist.import.succeeded", Integer.toString(cnt)));
        for (JCheckBox cb : playlistCbs.values()) {
          if (cb.isEnabled() && cb.isSelected()) {
            cb.setSelected(false);
          }
        }
      }
    }

    /**
     * @see de.stationadmin.gui.util.ThreadedAction#performAction()
     */
    @Override
    protected void performAction() throws Exception {
      BackupService tool = adminClient.getBackupService();
      for (JCheckBox cb : playlistCbs.values()) {
        if (cb.isEnabled() && cb.isSelected()) {
          Playlist playlist = (Playlist) cb.getClientProperty("playlist");
          this.status = textProvider.getString("backup.restore.playlist.import.status", playlist.getName());
          tool.restorePlaylist(file, playlist.getId(), legacyBackup);
          cnt++;
        }
      }
      if(profileImport.getValue().equals(Boolean.TRUE)) {
        tool.restorePlaylistProfile(file);
      }

    }

    /**
     * @see de.stationadmin.gui.util.ThreadedAction#showError(java.lang.Exception)
     */
    @Override
    protected void showError(Exception e) {
      JXErrorPane.showDialog(AppUtils.getRootFrame(), textProvider.createErrorInfo(e, "backup.restore.playlist.import.failed"));
    }

  }
}
