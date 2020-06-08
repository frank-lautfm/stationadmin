package de.stationadmin.gui.playlist;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.util.MissingSourceTracksException;
import de.stationadmin.base.playlist.util.PlaylistFiller;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.AppUtils;

public class PlaylistConfigExplainAction extends AbstractAction {
  private static final long serialVersionUID = -6359043129706573491L;
  private ClientContext ctx;
  private ValueModel playlistHolder;
  private PlaylistFiller filler;

  /**
   * @param ctx
   * @param playlistHolder
   */
  public PlaylistConfigExplainAction(ClientContext ctx, ValueModel playlistHolder) {
    super();
    this.putValue(Action.NAME, ctx.getTextProvider().getString("playlistcfg.explain.action.name"));
    this.ctx = ctx;
    this.playlistHolder = playlistHolder;
    playlistHolder.addValueChangeListener(new PropertyChangeListener() {
      
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        setEnabled(evt.getNewValue() != null);
      }
    });
  }

  @Override
  public void actionPerformed(ActionEvent evt) {
    Playlist playlist = (Playlist) playlistHolder.getValue();
    if(playlist != null) {
      PlaylistConfigExplainDlg dlg = new PlaylistConfigExplainDlg(ctx, playlist);
      dlg.setVisible(true);
    }

  }

}
