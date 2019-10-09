package de.stationadmin.gui.playlist;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.gui.ClientContext;

public class ShuffleTesterAction extends AbstractAction {

  private ClientContext ctx;
  private ValueModel playlistHolder;

  /**
   * @param ctx
   * @param playlistHolder
   */
  public ShuffleTesterAction(ClientContext ctx, ValueModel playlistHolder) {
    super();
    this.putValue(Action.NAME, ctx.getString("action.playlist.shuffletester"));
    this.ctx = ctx;
    this.playlistHolder = playlistHolder;
    this.setEnabled(false);
    playlistHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        setEnabled(evt.getNewValue() instanceof Playlist && ((Playlist) evt.getNewValue()).getType() == PlaylistType.ONLINE && ((Playlist) evt.getNewValue()).isShuffle()
            && ctx.getDesktop().isSupported(Desktop.Action.BROWSE));
      }
    });
  }

  @Override
  public void actionPerformed(ActionEvent evt) {
    Playlist playlist = (Playlist) playlistHolder.getValue();
    int duration = (playlist.getLength() / (60 * 60)) + 1;
    if(duration > 18) {
      duration = 18;
    }
    String url = "https://radioadmin.laut.fm/tools/#/shuffle-tester?stationId=" + ctx.getAdminClient().getStationId() + "&playlistId=" + playlist.getId() + "&duration=" + duration;
    try {
      ctx.getDesktop().browse(new URI(url));
    } catch (Exception e) {
      JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "action.playlist.shuffletester.failed"));
    }
  }

}
