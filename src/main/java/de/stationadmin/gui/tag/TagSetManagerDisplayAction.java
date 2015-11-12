package de.stationadmin.gui.tag;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;

public class TagSetManagerDisplayAction extends AbstractAction {
  private static final long serialVersionUID = -8402909649086329220L;
  private ClientContext ctx;
  
  public TagSetManagerDisplayAction(ClientContext ctx) {
    this.putValue(Action.SMALL_ICON, ctx.getIcon("configure.png"));
    this.putValue(Action.SHORT_DESCRIPTION, ctx.getTextProvider().getString("titletagset.action.open.tooltip"));
    this.ctx = ctx;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    TagSetManagerDlg dlg = new TagSetManagerDlg(ctx);
    dlg.setVisible(true);

  }

}
