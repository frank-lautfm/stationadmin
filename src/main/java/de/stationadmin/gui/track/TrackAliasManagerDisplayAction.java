package de.stationadmin.gui.track;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;

/**
 * 
 * @author Frank Korf
 * 
 */
public class TrackAliasManagerDisplayAction extends AbstractAction {

  private ClientContext ctx;

  public TrackAliasManagerDisplayAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getString("action.titlealiasmanager"));
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    TrackAliasManager mgr = new TrackAliasManager(ctx);
    mgr.setVisible(true);
  }

}
