package de.stationadmin.gui.migration;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;

public class MigrationDlgDisplayAction extends AbstractAction {
  private static final long serialVersionUID = -6620749945332775042L;
  private ClientContext ctx;
  
  public MigrationDlgDisplayAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getString("Datenübernahme von Version 3"));
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    MigrationDlg dlg = new MigrationDlg(ctx);
    dlg.setVisible(true);
  }

}
