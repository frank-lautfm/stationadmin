/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.playlist.PlaylistService;
import de.stationadmin.gui.TextProvider;

/**
 * Saves all currently modified playlists
 * 
 * @author Frank Korf
 * 
 */
public class ResetModifiedPlaylistsAction extends AbstractAction {
  private static final long serialVersionUID = 862615195672841109L;
  private TextProvider textProvider;
  private PlaylistService playlistService;

  /**
   * @param ctx
   */
  public ResetModifiedPlaylistsAction(TextProvider textProvider, PlaylistService playlistService) {
    super();
    this.textProvider = textProvider;
    this.playlistService = playlistService;
    this.putValue(Action.NAME, this.textProvider.getString("action.playlist.resetmulti.name"));
  }

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    PlaylistRegistry registry = playlistService.getPlaylistRegistry();
    for (Playlist playlist : registry.getAllPlaylists()) {
      if (playlist.isModified()) {
        playlist.reset();
      }
    }    
  }

}
