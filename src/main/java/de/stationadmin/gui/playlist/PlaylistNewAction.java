/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.playlist.config.PlaylistConfigurationDialog;
import de.stationadmin.gui.playlist.config.PlaylistConfigurationModel;

/**
 * Opens the dialog for creating a new playlist
 * 
 * @author korf
 */
public class PlaylistNewAction extends AbstractAction {
  private static final long serialVersionUID = 4856891819065912632L;
  private ClientContext ctx;
  private ValueModel playlistHolder;

  /**
   * @param ctx
   * @param playlistHolder
   */
  public PlaylistNewAction(ClientContext ctx, ValueModel playlistHolder) {
    super();
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.newplaylist"));
    this.ctx = ctx;
    this.playlistHolder = playlistHolder;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Playlist playlist = new Playlist(ctx.getAdminClient().getTrackService().getTrackRegistry(), PlaylistType.ONLINE);
    if (playlistHolder != null) {
      playlistHolder.setValue(playlist);
    }
    PlaylistConfigurationModel model = new PlaylistConfigurationModel(playlist, ctx.getAdminClient().getTagManager());
    PlaylistConfigurationDialog dlg = new PlaylistConfigurationDialog(ctx, model);
    dlg.setVisible(true);

  }

}
