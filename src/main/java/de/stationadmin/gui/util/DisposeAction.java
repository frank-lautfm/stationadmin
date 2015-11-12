/**
 * 
 */
package de.stationadmin.gui.util;

import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 *
 * @author Frank Korf
 *
 */
public class DisposeAction extends AbstractAction {
  private static final long serialVersionUID = 6552456041149993812L;
  private Window dialog;

  public DisposeAction(Window dialog, String label) {
    super();
    this.dialog = dialog;
    this.putValue(Action.NAME, label);
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    dialog.dispose();
  }

}
