/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.gui.ClientContext;

/**
 * 
 * @author Frank Korf
 * 
 */
public class RemoveTracksFromPlaylistsAction extends AbstractAction {
  private static final long serialVersionUID = 4072864315683614234L;
  private ClientContext ctx;
  private List<BasicTrack> titles;

  public RemoveTracksFromPlaylistsAction(ClientContext ctx) {
    this.ctx = ctx;
    this
        .putValue(Action.NAME, ctx.getString("action.removeTitlesFromPlaylist"));
    this.setEnabled(false);
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    if (titles != null && this.titles.size() > 0) {
      for (BasicTrack title : titles) {
        RegisteredTrack regTitle;
        if (title instanceof RegisteredTrack) {
          regTitle = (RegisteredTrack) title;
        } else {
          regTitle = this.ctx.getAdminClient().getTitleRegistry().getTrack(
              title.getId());
        }
        this.removeFromPlaylists(regTitle);
      }
    }
  }
  
  private void removeFromPlaylists(RegisteredTrack regTitle) {
    if(regTitle != null && regTitle.getPlaylistIds().size() > 0) {
      for(int playlistId : regTitle.getPlaylistIds()) {
        Playlist playlist =  this.ctx.getAdminClient().getPlaylistRegistry().getPlaylist(playlistId);
        if(playlist != null) {
          playlist.removeTrack(regTitle.getId());
        }
      }
    }
    
  }

  /**
   * @return the titles
   */
  public List<BasicTrack> getTitles() {
    return titles;
  }

  /**
   * @param titles
   *          the titles to set
   */
  public void setTitles(List<BasicTrack> titles) {
    this.titles = titles;
    this.setEnabled(this.titles != null && this.titles.size() > 0);
  }

}
