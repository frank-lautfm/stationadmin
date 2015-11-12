/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.playlist.PlaylistService;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.validation.GVLValidator;
import de.stationadmin.base.playlist.validation.PlaylistValidationException;
import de.stationadmin.base.schedule.Schedule;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.ThreadedAction;

/**
 * Saves all currently modified playlists
 * 
 * @author Frank Korf
 * 
 */
public class SaveModifiedPlaylistsAction extends ThreadedAction {
  private static final long serialVersionUID = 862615195672841109L;
  private volatile String status = "";
  private Playlist currentPlaylist;
  private TextProvider textProvider;
  private PlaylistService playlistService;
  private Schedule schedule;

  /**
   * @param ctx
   */
  public SaveModifiedPlaylistsAction(TextProvider textProvider, PlaylistService playlistService, Schedule schedule) {
    super();
    this.textProvider = textProvider;
    this.playlistService = playlistService;
    this.schedule = schedule;
    this.putValue(Action.NAME, this.textProvider.getString("action.playlist.savemulti.name"));
  }

  /**
   * @see de.stationadmin.gui.util.ThreadedAction#getStatus()
   */
  @Override
  protected String getStatus() {
    return this.status;
  }

  /**
   * @see de.stationadmin.gui.util.ThreadedAction#performAction()
   */
  @Override
  protected void performAction() throws Exception {
    PlaylistRegistry registry = playlistService.getPlaylistRegistry();
    for (Playlist playlist : registry.getAllPlaylists()) {
      if (playlist.isModified()) {
        this.status = this.textProvider.getString("action.playlist.msg", playlist != null
            ? playlist.getDisplayName()
            : "Playlist");
        this.currentPlaylist = playlist;
        playlistService.savePlaylist(playlist);
      }
    }
  }

  /**
   * @see de.stationadmin.gui.util.ThreadedAction#showError(java.lang.Exception)
   */
  @Override
  protected void showError(Exception e) {
    if (e instanceof PlaylistValidationException) {
      JXErrorPane.showDialog(
          null,
          this.textProvider.createErrorInfo(e, "playlist.validationerror."
              + ((PlaylistValidationException) e).getError().name().toLowerCase(), currentPlaylist.getDisplayName()));
    } else {
      JXErrorPane.showDialog(null,
          this.textProvider.createErrorInfo(e, "action.playlist.save.error", currentPlaylist.getDisplayName()));
    }
  }

  @Override
  protected boolean beforeExecution() {
    PlaylistRegistry registry = playlistService.getPlaylistRegistry();
    GVLValidator validator = new GVLValidator();
    int errors = 0;
    for (Playlist playlist : registry.getAllPlaylists()) {
      if (playlist.isModified() && this.schedule.isScheduled(playlist)) {
        List<Entry> violations = new ArrayList<Entry>();
        validator.validate(playlist, violations);
        if(violations.size() > 0) {
          errors++;
        }
      }
    }
    if(errors > 0) {
      return JOptionPane.showConfirmDialog(null, textProvider.getString("action.playlist.savemulti.msg.validationerror", Integer.toString(errors)), null, JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
      
    }
    return true;
  }

}
