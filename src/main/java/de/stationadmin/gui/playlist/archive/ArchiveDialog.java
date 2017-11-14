/**
 * 
 */
package de.stationadmin.gui.playlist.archive;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistService;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.SwingTools;

/**
 * @author Frank
 *
 */
public class ArchiveDialog extends JDialog {
  private static final long serialVersionUID = -2060606289684524273L;
  private TextProvider textProvider;
  private PlaylistService playlistService;
  private Playlist playlist;
  private ValueHolder nameHolder = new ValueHolder();
  private ValueHolder errorMsg = new ValueHolder(" ");

  /**
   * @param textProvider
   * @param playlistService
   * @param playlist
   */
  public ArchiveDialog(TextProvider textProvider, PlaylistService playlistService, Playlist playlist) {
    super();
    this.textProvider = textProvider;
    this.playlistService = playlistService;
    this.playlist = playlist;
    this.init();
  }

  private void init() {
    FormLayout layout = new FormLayout("5dlu,pref,5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,pref,8dlu,pref,5dlu");
    this.getContentPane().setLayout(layout);
    CellConstraints cc = new CellConstraints();

    this.setTitle(this.textProvider.getString("playlistarchive.title"));
    this.getContentPane().add(new JLabel(this.textProvider.getString("playlistarchive.property.name")), cc.xy(2, 2));
    ComponentFactory componentFactory = new ComponentFactory(textProvider);
    JTextField tf = componentFactory.createTextField(this.nameHolder, false);
    tf.setColumns(15);
    this.getContentPane().add(tf, cc.xy(4, 2));

    JLabel msgLabel = BasicComponentFactory.createLabel(errorMsg);
    Font font = msgLabel.getFont();
    msgLabel.setFont(new Font(font.getFamily(), Font.ITALIC, font.getSize()));
    this.getContentPane().add(msgLabel, cc.xywh(2, 4, 3, 1));

    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
    buttonPanel.add(new JButton(new SaveAction()));
    buttonPanel.add(new JButton(new DisposeAction()));
    this.getContentPane().add(buttonPanel, cc.xywh(2, 6, 3, 1));

    this.pack();
    SwingTools.centerOnScreen(this);
  }

  private class SaveAction extends AbstractAction {
    private static final long serialVersionUID = 2226196850376416413L;
    private Set<String> usedNames = new HashSet<String>();

    SaveAction() {
      super(textProvider.getString("save"));
      this.setEnabled(false);

      for (Playlist playlist : playlistService.getPlaylistRegistry().getAllPlaylists()) {
        usedNames.add(playlist.getName());
      }

      nameHolder.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          checkEnabled();
        }
      });
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      Playlist archivedPlaylist = new Playlist(playlist.getTrackRegistry(), PlaylistType.ARCHIVED);
      archivedPlaylist.setName(nameHolder.getString());
      for (Playlist.Entry entry : playlist.getEntries()) {
        BasicTrack title = playlist.getTrackRegistry().getTrack(entry.getTrackId());
        if(title == null) {
          title = entry.getTrack();
        }
        archivedPlaylist.addTrack(title);
      }
      try {
        playlistService.getPlaylistRegistry().register(archivedPlaylist);
        playlistService.savePlaylist(archivedPlaylist);
        dispose();
      } catch (Exception e) {
        JXErrorPane.showDialog(null, textProvider.createErrorInfo(e, "playlistarchive.msg.failed"));
      }
    }

    private void checkEnabled() {
      String name = StringUtils.trimToEmpty((String) nameHolder.getValue());
      if (name.length() == 0) {
        errorMsg.setValue(textProvider.getString("playlistarchive.msg.noname"));
        setEnabled(false);
      } else if (usedNames.contains(name)) {
        errorMsg.setValue(textProvider.getString("playlistarchive.msg.dupename"));
        setEnabled(false);
      } else {
        errorMsg.setValue("");
        setEnabled(true);
      }

    }

  }

  private class DisposeAction extends AbstractAction {
    private static final long serialVersionUID = -7559791897944754785L;

    DisposeAction() {
      super(textProvider.getString("cancel"));
    }

    public void actionPerformed(ActionEvent e) {
      dispose();
    }

  }

}
