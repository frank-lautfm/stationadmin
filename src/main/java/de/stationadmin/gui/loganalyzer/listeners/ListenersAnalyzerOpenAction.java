/**
 * 
 */
package de.stationadmin.gui.loganalyzer.listeners;

import javax.swing.Action;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.loganalyzer.LogAccessAction;

/**
 * @author korf
 *
 */
public class ListenersAnalyzerOpenAction extends LogAccessAction {
  private static final long serialVersionUID = 9086410550326478691L;
  
  public ListenersAnalyzerOpenAction(ClientContext ctx) {
    super(ctx);
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.listenersanalyzer.name"));
    
  }

  @Override
  public void performAction() {
    try {
    ListenersAnalyzer win = new ListenersAnalyzer(ctx);
    win.setVisible(true);
    } catch (Throwable t) {
      JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(t, "action.listenersanalyzer.msg.error"));
    }

  }

}
