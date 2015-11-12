package de.stationadmin.gui.mp3explorer;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;

public class MP3ExplorerDisplayAction extends AbstractAction {
  private static final long serialVersionUID = -6620749945332775042L;
  private ClientContext ctx;
  
  public MP3ExplorerDisplayAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getString("action.mp3explorer"));
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    MP3Explorer explorer = new MP3Explorer(ctx);
    explorer.setVisible(true);
  }

}
