/**
 * 
 */
package de.stationadmin.gui.backup;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.gui.ClientContext;

/**
 * 
 * @author Frank Korf
 * 
 */
public class BackupRestoreAction extends AbstractAction {
  private static final long serialVersionUID = 4856812523592765623L;
  private ClientContext ctx;

  public BackupRestoreAction(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.backup.restore"));
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent evt) {

    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileFilter(new FileNameExtensionFilter("Zip", "zip"));
    fileChooser.setDialogTitle((String) this.getValue(Action.NAME));
    fileChooser.setCurrentDirectory(new File(ctx.getAdminClient().getBackupService().getBackupDirectory()));

    if (fileChooser.showOpenDialog(ctx.getRootWindow()) == JFileChooser.APPROVE_OPTION) {
      if (fileChooser.getSelectedFile().lastModified() < StationAdminClient.TIMESTAMP_RADIOADMIN_SWITCH) {
        // JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(null, "action.backup.restore.err.v3"));
        JOptionPane.showMessageDialog(ctx.getRootWindow(), "Aus Station Admin 3.x Backups können nur Playlists importiert werden.", "Hinweis", JOptionPane.INFORMATION_MESSAGE);
      }
      // } else {
      BackupRestoreDlg dlg = new BackupRestoreDlg(this.ctx, fileChooser.getSelectedFile());
      dlg.setVisible(true);
      // }
    }

  }

}
