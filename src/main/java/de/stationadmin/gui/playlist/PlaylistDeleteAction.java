/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.playlist.PlaylistService;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.AppUtils;

class PlaylistDeleteAction extends AbstractAction implements PropertyChangeListener {
  private static final long serialVersionUID = 4692227689345527634L;
  private TextProvider textProvider;
  private ValueModel playlistHolder;
  private PlaylistService service;

  public PlaylistDeleteAction(ValueModel playlistHolder, PlaylistService service, TextProvider textProvider, boolean asButton) {
    this.playlistHolder = playlistHolder;
    this.textProvider = textProvider;
    this.service = service;
    if (asButton) {
      this.putValue(Action.SMALL_ICON, AppUtils.getIcon("delete.png"));
      this.putValue(Action.SHORT_DESCRIPTION, this.textProvider.getString("action.playlist.delete.tooltip"));
    } else {
      this.putValue(Action.NAME, this.textProvider.getString("delete"));
    }
    setEnabled(false);
    this.playlistHolder.addValueChangeListener(this);
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent evt) {
    Playlist playlist = (Playlist) this.playlistHolder.getValue();
    if (playlist != null) {
      String key = playlist.getType() == PlaylistType.ARCHIVED ? "action.playlist.delete.msg.confirm.archived" : "action.playlist.delete.msg.confirm";
      if (JOptionPane.showConfirmDialog(AppUtils.getRootFrame(), this.textProvider.getString(key, playlist.getName()), null, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        try {
          this.service.deletePlaylist(playlist);
        } catch (IOException e) {
          JXErrorPane.showDialog(null, this.textProvider.createErrorInfo(e, "action.playlist.delete.msg.failed"));

        }
      }
    }
  }

  /**
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    setEnabled(evt.getNewValue() != null && ((Playlist) evt.getNewValue()).getType().isDeleteSupported() && ((Playlist) evt.getNewValue()).getId() > -1);
  }

}