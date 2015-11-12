/**
 * 
 */
package de.stationadmin.gui.tag;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;

/**
 *
 * @author Frank Korf
 *
 */
public class TagManagerDisplayAction extends AbstractAction {

  private ClientContext ctx;
  
  public TagManagerDisplayAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getString("action.titletagmanager"));
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    TagManagerDlg dlg = new TagManagerDlg(ctx);
    dlg.setVisible(true);
  }


}
