/**
 * 
 */
package de.stationadmin.gui.backup;

import java.awt.event.ActionEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;
import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.backup.BackupService;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.AppUtils;

/**
 *
 * @author Frank Korf
 *
 */
public class BackupCreateAction extends AbstractAction {
  private static final long serialVersionUID = 2572801667117821471L;

  private SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
  private StationAdminClient adminClient;
  private TextProvider textProvider;

  /**
   * @param rootWindow
   * @param adminClient
   * @param textProvider
   */
  public BackupCreateAction(TextProvider textProvider, StationAdminClient adminClient) {
    super();
    this.adminClient = adminClient;
    this.textProvider = textProvider;
    this.putValue(Action.NAME, this.textProvider.getString("action.backup.create"));
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent evt) {

    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileFilter(new FileNameExtensionFilter("Zip", "zip"));
    fileChooser.setDialogTitle((String) this.getValue(Action.NAME));
    BackupService tool = adminClient.getBackupService();
    
    new File(tool.getBackupDirectory()).mkdirs();

    String filename = this.adminClient.getStation() + " " + this.fmt.format(new Date()) + ".zip";
    File file = new File(FilenameUtils.concat(tool.getBackupDirectory(), filename));
    fileChooser.setSelectedFile(file);

    if (fileChooser.showSaveDialog(AppUtils.getRootFrame()) == JFileChooser.APPROVE_OPTION) {
      try {
        tool.createBackup(fileChooser.getSelectedFile());
      } catch (Exception e) {
        JXErrorPane.showDialog(AppUtils.getRootFrame(), this.textProvider.createErrorInfo(e, "action.backup.msg.failed"));
      }
    }

  }

}
