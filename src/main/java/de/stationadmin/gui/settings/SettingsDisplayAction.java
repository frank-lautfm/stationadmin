package de.stationadmin.gui.settings;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;


public class SettingsDisplayAction extends AbstractAction {
  private static final long serialVersionUID = -6620749945332775042L;
  private ClientContext ctx;
  
  public SettingsDisplayAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getString("action.settings"));
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    SettingsDlg dlg = new SettingsDlg(ctx);
    dlg.setVisible(true);

  }

}
