/**
 * 
 */
package de.stationadmin.gui.backup;

import java.awt.FlowLayout;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.backup.BackupService;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.ThreadedAction;

/**
 * 
 * @author Frank Korf
 * 
 */
public class ScheduleRestorePanel extends JPanel {
  private static final long serialVersionUID = 4351585319030190533L;
  private TextProvider textProvider;
  private BackupService backupService;
  private File file;
  private boolean available = false;

  private Map<Integer, JCheckBox> playlistCbs = new HashMap<Integer, JCheckBox>();

  public ScheduleRestorePanel(ClientContext ctx, File file) {
    super();
    this.textProvider = ctx.getTextProvider();
    this.backupService = ctx.getAdminClient().getBackupService();
    this.file = file;

    try {
      BackupService tool = ctx.getAdminClient().getBackupService();
      this.available = tool.isScheduleAvailable(file);
    } catch (Exception e) {

    }

    this.init();
  }

  private void init() {

    this.setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,50dlu:grow,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    {
      JPanel head = new JPanel(new FlowLayout(FlowLayout.LEADING));
      if (available) {
        head.add(new JLabel(textProvider.getString("backup.restore.schedule.available.head")));
      } else {
        head.add(new JLabel(textProvider.getString("backup.restore.schedule.unavailable.head")));
      }
      this.add(head, cc.xy(2, 2));
    }

    {
      JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));

      JButton importBtn = new JButton(new ImportAction());
      bottom.add(importBtn);

      bottom.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
      this.add(bottom, cc.xy(2, 6));

    }

  }

  private class ImportAction extends ThreadedAction {
    private static final long serialVersionUID = 2902409961310655709L;
    private volatile String status = "";
    private volatile int cnt;

    /**
     * @param ctx
     */
    public ImportAction() {
      super(getRootPane());
      this.putValue(Action.NAME, textProvider.getString("backup.restore.import"));
      this.setEnabled(available);
    }

    /**
     * @see de.stationadmin.gui.util.ThreadedAction#getStatus()
     */
    @Override
    protected String getStatus() {
      return this.status;
    }

    /**
     * @see de.stationadmin.gui.util.ThreadedAction#performAction()
     */
    @Override
    protected void performAction() throws Exception {
      this.status = textProvider.getString("backup.restore.schedule.import.status");
      backupService.restoreSchedule(file);
    }

    /**
     * @see de.stationadmin.gui.util.ThreadedAction#showError(java.lang.Exception)
     */
    @Override
    protected void showError(Exception e) {
      JXErrorPane.showDialog(AppUtils.getRootFrame(), textProvider.createErrorInfo(e, "backup.restore.playlist.import.failed"));
    }

    /**
     * @see de.stationadmin.gui.util.ThreadedAction#onSuccess()
     */
    @Override
    protected void onSuccess() {
      if (cnt > 0) {
        JOptionPane.showMessageDialog(AppUtils.getRootFrame(), textProvider.getString("backup.restore.playlist.import.succeeded", Integer.toString(cnt)));
        for (JCheckBox cb : playlistCbs.values()) {
          if (cb.isEnabled() && cb.isSelected()) {
            cb.setSelected(false);
          }
        }
      }
    }

  }
}
