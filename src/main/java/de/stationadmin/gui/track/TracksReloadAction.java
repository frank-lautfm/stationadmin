/**
 * 
 */
package de.stationadmin.gui.track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.ThreadedAction;
import de.stationadmin.lfm.backend.ResourceNotFoundException;

/**
 *
 * @author Frank Korf
 *
 */
public class TracksReloadAction extends ThreadedAction {
  private static final long serialVersionUID = -1252201973458997565L;
  private ClientContext ctx;
  private int[] trackIds;
  private int numDeleted;

  private String status;

  public TracksReloadAction(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.track.reload"));
    this.setEnabled(false);
  }

  /**
   * @return the titleIds
   */
  protected int[] getTrackIds() {
    return trackIds;
  }

  public void setTracks(List<BasicTrack> tracks) {
    int[] trackIds = new int[tracks != null ? tracks.size() : 0];
    for(int i = 0; i < tracks.size(); i++) {
      trackIds[i] = tracks.get(i).getId();
    }
    this.trackIds = trackIds;
    this.setEnabled(trackIds.length > 0);
  }
  
  public void setTrackIds(int[] trackIds) {
    this.trackIds = trackIds;
    this.setEnabled(trackIds.length > 0);
  }

  @Override
  protected String getStatus() {
    return this.status;
  }

  @Override
  protected void performAction() throws Exception {
    this.status = ctx.getTextProvider().getString("action.track.reload.status.tracks");
    this.numDeleted = 0;
    if (this.isEnabled()) {

      List<Integer> notFound = new ArrayList<Integer>();
      for (int trackId : this.trackIds) {
        try {
          this.ctx.getAdminClient().getTrackService().reloadTrack(trackId);
        } catch (ResourceNotFoundException e) {
          notFound.add(trackId);
        }
      }

      if (notFound.size() > 0) {
        Set<Playlist> dirtyPlaylists = new HashSet<Playlist>();

        // remove tags (if any)
        for (int trackId : notFound) {
          RegisteredTrack track = ctx.getAdminClient().getTrackService().getTrackRegistry().getTrack(trackId);
          this.numDeleted++;
          if (track != null) {
            if (track.getTagCnt() > 0) {
              ctx.getAdminClient().getTagManager().onTracksDelete(trackId);
            }
            for(int playlistId : track.getPlaylistIds()) {
               Playlist pl = ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylist(playlistId);
               pl.removeTrack(trackId);
               dirtyPlaylists.add(pl);
            }
          }
          
          this.ctx.getAdminClient().getTrackService().getTrackRegistry().remove(trackId);
        }
        for(Playlist pl : dirtyPlaylists) {
          this.status = ctx.getTextProvider().getString("action.track.reload.status.playlist", pl.getName());
          ctx.getAdminClient().getPlaylistService().savePlaylist(pl);
        }
        
      }

      this.ctx.getAdminClient().getTrackService().saveTracks();

    }

  }

  @Override
  protected void showError(Exception e) {
    JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "action.track.reload.failed"));
  }

  @Override
  protected void onSuccess() {
    if(numDeleted > 0) {
      String msg = ctx.getTextProvider().getString("action.track.reload.msg.deletedTracks", Integer.toString(this.numDeleted));
      JOptionPane.showMessageDialog(ctx.getRootWindow(), msg);
    }
  }

}
