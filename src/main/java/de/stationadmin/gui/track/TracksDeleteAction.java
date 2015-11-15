/**
 * 
 */
package de.stationadmin.gui.track;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.Title;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.ThreadedAction;

/**
 *
 * @author Frank Korf
 *
 */
public class TracksDeleteAction extends ThreadedAction {
  private static final long serialVersionUID = -1252201973458997565L;
  private ClientContext ctx;
  private int[] trackIds;
  private int numUsed = 0;
  
  private Set<Playlist> dirtyPlaylists = new HashSet<Playlist>();
  private String status;

  public TracksDeleteAction(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.title.delete"));
    this.setEnabled(false);
  }

  /**
   * @return the titleIds
   */
  protected int[] getTrackIds() {
    return trackIds;
  }

  protected void setTracks(List<Title> tracks) {
    int[] trackIds = new int[tracks != null ? tracks.size() : 0];
    boolean enabled = trackIds.length > 0;
    this.numUsed = 0;
    if (tracks != null) {
      for (int i = 0; i < tracks.size(); i++) {
        trackIds[i] = tracks.get(i).getId();
        RegisteredTrack r = tracks.get(i) instanceof RegisteredTrack ? (RegisteredTrack) tracks.get(i)
            : ctx.getAdminClient().getTrackService().getTrackRegistry().getTrack(tracks.get(i).getId());
        if (r == null || !r.isOwnTrack()) {
          enabled = false;
        }
        if (r.getPlaylistIds().size() > 0) {
          numUsed++;
        }
      }
    }
    this.trackIds = trackIds;
    this.setEnabled(enabled);
  }

  @Override
  protected String getStatus() {
    return this.status;
  }

  @Override
  protected void performAction() throws Exception {
    this.status = ctx.getTextProvider().getString("action.title.delete.status.prepare");
    if (this.isEnabled()) {
      this.status = null;
      if (numUsed > 0) {
        for (Playlist pl : dirtyPlaylists) {
          this.status = ctx.getTextProvider().getString("action.title.delete.status.playlist", pl.getName());
          ctx.getAdminClient().getPlaylistService().savePlaylist(pl);
        }
      }

      try {
        this.status = ctx.getTextProvider().getString("action.title.delete.status.tags");
        this.ctx.getAdminClient().getTagManager().onTracksDelete(trackIds);
      } catch (Exception e) {
        Logger.getLogger(TracksDeleteAction.class).warn("unable to update tag files", e);
        // accept inconsistency - problem will disappear after next
        // synchronization
      }

      this.status = ctx.getTextProvider().getString("action.title.delete.status.tracks");
      ctx.getAdminClient().getTrackService().deleteTracks(trackIds);
    }

  }

  @Override
  protected void showError(Exception e) {
    JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "action.title.delete.failed"));
  }

  @Override
  protected boolean beforeExecution() {
    this.dirtyPlaylists.clear();
    if (numUsed > 0) {
      if (JOptionPane.showConfirmDialog(null, ctx.getTextProvider().getString("action.title.delete.msg.playlist_confirm", Integer.toString(this.numUsed)),
          (String)this.getValue(Action.NAME), JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
        return false;
      }
      
      boolean resetPlaylists = false;
      for (int trackId : this.trackIds) {
        if (!resetPlaylists) {
          RegisteredTrack track = ctx.getAdminClient().getTrackService().getTrackRegistry().getTrack(trackId);
          for (int playlistId : track.getPlaylistIds()) {
            Playlist pl = ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylist(playlistId);
            pl.removeTrack(trackId);
            if (pl.getLength() < 60 * 60) {
              resetPlaylists = true;
              ErrorInfo info = ctx.createErrorInfo(null, "action.title.delete.msg.playlistMinLen", pl.getName());
              JXErrorPane.showDialog(null, info);
            }
            dirtyPlaylists.add(pl);
          }
        }
      }

      if (resetPlaylists) {
        for (Playlist pl : dirtyPlaylists) {
          pl.reset();
        }
        return false;
      }
      
    }
    return true;
  }

}
