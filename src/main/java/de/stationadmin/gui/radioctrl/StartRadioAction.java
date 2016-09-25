/**
 * 
 */
package de.stationadmin.gui.radioctrl;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.gui.ClientContext;

/**
 * 
 * @author Frank Korf
 * 
 */
public class StartRadioAction extends AbstractAction implements PropertyChangeListener {
  private static final long serialVersionUID = 8118560544177275223L;
  private ClientContext ctx;

  public StartRadioAction(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getString("action.radio.start"));
    this.setEnabled(false);
    // this.setEnabled(!ctx.getRadioStatus().booleanValue());
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent evt) {
    try {
      this.ctx.getAdminClient().startRadio();
      this.ctx.getRadioStatus().setValue(this.ctx.getAdminClient().isRadioStarted());
    } catch (IOException e) {
      JXErrorPane.showDialog(null, ctx.createErrorInfo(e, "action.radio.start.error"));
    }
  }

  /**
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (evt.getSource() == ctx.getRadioStatus()) {
      // this.setEnabled(!ctx.getRadioStatus().booleanValue());
    }
  }

}
