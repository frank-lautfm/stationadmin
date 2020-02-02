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
import de.stationadmin.base.playlist.util.PlaylistFiller;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.AppUtils;

public class AutoFillAction extends AbstractAction {
  private static final long serialVersionUID = -6359043129706573491L;
  private ClientContext ctx;
  private ValueModel playlistHolder;
  private PlaylistFiller filler;

  /**
   * @param ctx
   * @param playlistHolder
   */
  public AutoFillAction(ClientContext ctx, ValueModel playlistHolder) {
    super();
    this.putValue(Action.SMALL_ICON, AppUtils.getIcon("autofill.png"));
    this.putValue(Action.SHORT_DESCRIPTION, ctx.getTextProvider().getString("action.playlist.fill.tooltip"));
    this.ctx = ctx;
    this.playlistHolder = playlistHolder;
    this.filler = new PlaylistFiller(ctx.getAdminClient().getPlaylistService(), ctx.getAdminClient().getTrackService(), ctx.getAdminClient().getTagManager());
    this.setEnabled(false);
    playlistHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        Playlist pl = evt.getNewValue() instanceof Playlist ? (Playlist) evt.getNewValue() : null;
        setEnabled(pl != null && pl.getType() == PlaylistType.ONLINE && pl.getAutoFillRule() != null && pl.getAutoFillRule().isEnabled());
      }
    });
  }

  @Override
  public void actionPerformed(ActionEvent evt) {
    Playlist playlist = (Playlist) playlistHolder.getValue();
    if (playlist != null) {
      try {
        filler.fillPlaylist(playlist);
      } catch (Exception e) {
        JXErrorPane.showDialog(AppUtils.getRootFrame(), ctx.createErrorInfo(e, "action.playlist.fill.error"));
      }
    }

  }

}
