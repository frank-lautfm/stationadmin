/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.playlist.config.PlaylistConfigurationDialog;
import de.stationadmin.gui.playlist.config.PlaylistConfigurationModel;
import de.stationadmin.gui.util.AppUtils;

class PlaylistEditPropertiesAction extends AbstractAction implements PropertyChangeListener {
  private static final long serialVersionUID = -1127269220503072051L;

  private ClientContext ctx;
  private ValueModel playlistHolder;

  public PlaylistEditPropertiesAction(ClientContext ctx, ValueModel playlistHolder, boolean asButton) {
    this.ctx = ctx;
    this.playlistHolder = playlistHolder;
    if (asButton) {
      this.putValue(Action.SMALL_ICON, AppUtils.getIcon("configure.png"));
      this.putValue(Action.SHORT_DESCRIPTION, this.ctx.getTextProvider().getString("playlistviewer.configure.tooltip"));
    } else {
      this.putValue(Action.NAME, this.ctx.getTextProvider().getString("properties"));
    }
    setEnabled(false);
    this.playlistHolder.addValueChangeListener(this);
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    Playlist playlist = (Playlist) this.playlistHolder.getValue();
    if (playlist != null) {
      PlaylistConfigurationModel model = new PlaylistConfigurationModel(playlist, this.ctx.getAdminClient().getTagManager(), ctx.getAdminClient().getSettings(), ctx.getAdminClient().getPlaylistService().getShuffleScripts(), ctx.getAdminClient().getPlaylistService().getProfiles(), ctx.getTextProvider());
      PlaylistConfigurationDialog dlg = new PlaylistConfigurationDialog(this.ctx, model);
      dlg.setVisible(true);
    }
  }

  /**
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    Playlist playlist = (Playlist) this.playlistHolder.getValue();
    setEnabled(evt.getNewValue() != null && playlist != null);
  }

}