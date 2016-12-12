/**
 * 
 */
package de.stationadmin.gui.loganalyzer;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

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
    this.performAction();
  }

  protected abstract void performAction();

}
