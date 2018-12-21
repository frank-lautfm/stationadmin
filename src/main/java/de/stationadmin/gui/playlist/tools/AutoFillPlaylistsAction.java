package de.stationadmin.gui.playlist.tools;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.AppUtils;

public class AutoFillPlaylistsAction extends AbstractAction {
  private static final long serialVersionUID = -7686746010613036366L;
  private ClientContext ctx;

  public AutoFillPlaylistsAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.playlist.fill"));
  }

  public void checkEnabled() {
    boolean enabled = false;
    for (Playlist playlist : ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE)) {
      if (playlist.getAutoFillRule().isEnabled()) {
        enabled = true;
        break;
      }
    }
    setEnabled(enabled);
  }

  @Override
  public void actionPerformed(ActionEvent evt) {
    boolean enabled = false;
    for (Playlist playlist : ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE)) {
      if (playlist.getAutoFillRule().isEnabled()) {
        enabled = true;
        break;
      }
    }
    if (enabled) {
      AutoFillPlaylistsDlg dlg = new AutoFillPlaylistsDlg(ctx);
      dlg.setVisible(true);
    } else {
      String msg = ctx.getString("autofill.dlg.unavailable");
      JOptionPane.showMessageDialog(AppUtils.getRootFrame(), msg, null, JOptionPane.INFORMATION_MESSAGE);

    }

  }

}
