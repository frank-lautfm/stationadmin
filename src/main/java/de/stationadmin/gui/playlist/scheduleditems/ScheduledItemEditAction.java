package de.stationadmin.gui.playlist.scheduleditems;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;

public class ScheduledItemEditAction extends AbstractAction {
  private static final long serialVersionUID = 5637845623893957634L;
  private ClientContext ctx;

  public ScheduledItemEditAction(ClientContext ctx) {
    this.putValue(Action.NAME, ctx.getString("scheduleditems.title"));
    this.ctx = ctx;
  }




  @Override
  public void actionPerformed(ActionEvent e) {
    ScheduledItemDlg dlg = new ScheduledItemDlg(ctx);
    dlg.setVisible(true);
  }

}
