package de.stationadmin.gui.playlist;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.SwingTools;

public class PlaylistToOnlineAction extends AbstractAction {
  private static final long serialVersionUID = 1405117014362903492L;

  private ClientContext ctx;
  private ValueModel playlistHolder;

  public PlaylistToOnlineAction(ClientContext ctx, ValueModel playlistHolder) {
    super();
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.playlist.online"));
    this.ctx = ctx;
    this.playlistHolder = playlistHolder;

    this.setEnabled(false);
    playlistHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        setEnabled(evt.getNewValue() instanceof Playlist && ((Playlist) evt.getNewValue()).getType() == PlaylistType.ARCHIVED);
      }
    });

  }

  @Override
  public void actionPerformed(ActionEvent evt) {

    Playlist source = (Playlist) playlistHolder.getValue();

    String name = source.getName();
    HashSet<String> names = new HashSet<>();
    for (Playlist pl : ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE)) {
      names.add(pl.getName().toUpperCase());
    }

    boolean tracksOnly = false;
    if (names.contains(name.toUpperCase())) {

      int suffix = 2;
      name = source.getName() + " (" + suffix + ")";
      while (names.contains(name.toUpperCase())) {
        suffix++;
        name = source.getName() + " (" + suffix + ")";
      }

      NameDecisionDialog dlg = new NameDecisionDialog(source.getName(), name);
      SwingTools.centerWithin(ctx.getRootWindow(), dlg);
      dlg.setVisible(true);

      name = dlg.getAcceptedName();
      if (name == null) {
        return;
      }
      tracksOnly = dlg.isTracksOnly();
    }

    Playlist copy = null;

    boolean isNew = false;
    if (names.contains(name.toUpperCase())) {
      // find existing playlist to overwrite
      for (Playlist pl : ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE)) {
        if (pl.getName().equals(name)) {
          copy = pl;
          copy.removeEntries(new ArrayList<>(copy.getEntries()));
          break;
        }
      }
    }
    if (copy == null) {
      // create new playlist
      copy = new Playlist(source.getTrackRegistry(), PlaylistType.ONLINE);
      isNew = true;
    }
    if (!tracksOnly) {
      List<String> properties = source.getProperties();
      properties.removeIf(p -> p.contains("type") || p.contains("shuffle") || p.startsWith("name"));
      copy.setProperties(properties, true);
    }
    copy.setName(name);
    for (Entry entry : source.getEntries()) {
      copy.addTrack(entry.getTrack());
    }

    try {
      ctx.getAdminClient().getPlaylistService().savePlaylist(copy);
      if (isNew) {
        ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().register(copy);
      }
    } catch (Exception e) {
      JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "action.playlist.archive.error"));
    }

  }

  class NameDecisionDialog extends JDialog {
    private static final long serialVersionUID = 6973571957505201652L;
    private String originalName;
    private ValueHolder suggestedName = new ValueHolder();
    private ValueHolder overwrite = new ValueHolder(1);
    private String acceptedName;
    private boolean tracksOnly = false;

    NameDecisionDialog(String originalName, String suggestedName) {
      this.originalName = originalName;
      this.suggestedName.setValue(suggestedName);
      init();
      setModal(true);
    }

    String getAcceptedName() {
      return this.acceptedName;
    }

    private void init() {
      this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,3dlu,pref,3dlu,pref,3dlu,pref,3dlu,pref,5dlu,pref,5dlu"));
      CellConstraints cc = new CellConstraints();

      TextProvider textProvider = ctx.getTextProvider();
      String msgText = textProvider.getString("action.playlist.online.msg.overwrite", originalName);
      getContentPane().add(new JLabel(msgText), cc.xy(2, 2));

      JRadioButton btnOverwrite = BasicComponentFactory.createRadioButton(this.overwrite, 1, textProvider.getString("action.playlist.online.option.overwrite"));
      getContentPane().add(btnOverwrite, cc.xy(2, 4));
      JRadioButton btnOverwriteFull = BasicComponentFactory.createRadioButton(this.overwrite, 2, textProvider.getString("action.playlist.online.option.overwrite.full"));
      getContentPane().add(btnOverwriteFull, cc.xy(2, 6));
      JRadioButton btnRename = BasicComponentFactory.createRadioButton(this.overwrite, 3, textProvider.getString("action.playlist.online.option.rename"));
      getContentPane().add(btnRename, cc.xy(2, 8));
      final JTextField tfName = BasicComponentFactory.createTextField(suggestedName);
      tfName.setEditable(false);
      getContentPane().add(tfName, cc.xy(2, 10));

      overwrite.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          tfName.setEditable(overwrite.getValue().equals(3));
        }
      });

      JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
      JButton okBtn = new JButton(ctx.getTextProvider().getString("ok"));
      okBtn.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          if (overwrite.getValue().equals(1) || overwrite.getValue().equals(2)) {
            acceptedName = originalName;
            tracksOnly = overwrite.getValue().equals(1);
          } else {
            acceptedName = (String) suggestedName.getValue();
            tracksOnly = false;
          }
          dispose();
        }
      });

      JButton cancelBtn = new JButton(ctx.getTextProvider().getString("cancel"));
      cancelBtn.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          dispose();
        }
      });
      btnPanel.add(okBtn);
      btnPanel.add(cancelBtn);

      getContentPane().add(btnPanel, cc.xy(2, 12, CellConstraints.CENTER, CellConstraints.CENTER));

      Dimension dim = this.getPreferredSize();
      this.setSize((int) dim.getWidth() + 20, (int) dim.getHeight() + 50);

    }

    public boolean isTracksOnly() {
      return tracksOnly;
    }

  }

}
