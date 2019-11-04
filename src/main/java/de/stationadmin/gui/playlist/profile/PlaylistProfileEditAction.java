package de.stationadmin.gui.playlist.profile;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;

public class PlaylistProfileEditAction extends AbstractAction {
  private static final long serialVersionUID = 7478441572348100571L;
  private ClientContext ctx;

  public PlaylistProfileEditAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getString("action.playlistprofile"));
  }

  @Override
  public void actionPerformed(ActionEvent e) {

    PlaylistProfileDlg dlg = new PlaylistProfileDlg(ctx);
    dlg.setVisible(true);
  }

}
