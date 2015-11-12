/**
 * 
 */
package de.stationadmin.gui.playlist.tools;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;

/**
 * Action for displaying {@link DupeFinderDlg}
 * 
 * @author Frank Korf
 */
public class DupeFinderDisplayAction extends AbstractAction {
	private static final long serialVersionUID = 2743196713675597333L;
	private ClientContext ctx;
	
	public DupeFinderDisplayAction(ClientContext ctx) {
		this.ctx = ctx;
		this.putValue(Action.NAME, ctx.getString("action.dupefinder"));
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		DupeFinderDlg dlg = new DupeFinderDlg(ctx);
		dlg.setVisible(true);
	}

}
