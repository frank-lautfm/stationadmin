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
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.gui.ClientContext;

/**
 * Opens the dialog for creating a new playlist
 * 
 * @author korf
 */
public class PlaylistDuplicateAction extends AbstractAction {
  private static final long serialVersionUID = 4856891819065912632L;
  private ClientContext ctx;
  private ValueModel playlistHolder;

  /**
   * @param ctx
   * @param playlistHolder
   */
  public PlaylistDuplicateAction(ClientContext ctx, ValueModel playlistHolder) {
    super();
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.playlist.duplicate"));
    this.ctx = ctx;
    this.playlistHolder = playlistHolder;
    this.setEnabled(false);
    playlistHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        setEnabled(evt.getNewValue() instanceof Playlist && ((Playlist) evt.getNewValue()).getType() == PlaylistType.ONLINE);
      }
    });
  }

  @Override
  public void actionPerformed(ActionEvent evt) {
    Playlist source = (Playlist) playlistHolder.getValue();
    Playlist copy = new Playlist(ctx.getAdminClient().getTrackService().getTrackRegistry(), PlaylistType.ONLINE);
    copy.setProperties(source.getProperties(), false);
    copy.setName(source.getName() + " - Kopie");
    copy.setId(-1);
    for (Entry entry : source.getEntries()) {
      copy.addTrack(entry.getTrack());
    }

    try {
      ctx.getAdminClient().getPlaylistService().savePlaylist(copy);

      if (playlistHolder != null) {
        playlistHolder.setValue(copy);
      }
    } catch (Exception e) {

    }

  }

}
