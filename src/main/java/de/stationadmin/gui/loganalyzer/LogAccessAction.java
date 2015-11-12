/**
 * 
 */
package de.stationadmin.gui.loganalyzer;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

import org.jdesktop.swingx.JXLabel;

import de.stationadmin.gui.ClientContext;

/**
 * @author korf
 * 
 */
public abstract class LogAccessAction extends AbstractAction {
  private static final long serialVersionUID = 915087958821923688L;
  protected ClientContext ctx;

  public LogAccessAction(ClientContext ctx) {
    super();
    this.ctx = ctx;
  }

  @Override
  public final void actionPerformed(ActionEvent evt) {
    if (!ctx.getAdminClient().getSettings().isLogDownloadPermitted()) {
      JXLabel label = new JXLabel(ctx.getTextProvider().getString("logdownload.confirmation.msg", "stationadmin.emjoy.net"));
      JCheckBox chekbox = new JCheckBox(ctx.getTextProvider().getString("logdownload.confirmation.autodownload"));
      Object[] message = { label, chekbox };

      if (JOptionPane.showConfirmDialog(ctx.getRootWindow(), message,
          ctx.getTextProvider().getString("logdownload.confirmation.title", "stationadmin.emjoy.net"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        ctx.getAdminClient().getSettings().setLogDownloadPermitted(true);
        ctx.getAdminClient().getSettings().setLogAutodownloadPermitted(chekbox.isSelected());
        try {
          ctx.getAdminClient().saveSettings();
        } catch (Exception e) {
        }
        this.performAction();
      }
    } else {
      this.performAction();
    }
  }

  protected abstract void performAction();

}
