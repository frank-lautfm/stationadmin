/**
 * 
 */
package de.stationadmin.gui.backup;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

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
public class TitleTagRestorePanel extends JPanel {
  private static final long serialVersionUID = -7386236089137928548L;
  // private ClientContext ctx;
  private BackupService backupService;
  private TextProvider textProvider;
  private File file;
  private List<JCheckBox> tagCbs = new ArrayList<JCheckBox>();

  public TitleTagRestorePanel(ClientContext ctx, File file) {
    super();
    this.backupService = ctx.getAdminClient().getBackupService();
    this.textProvider = ctx.getTextProvider();
    this.file = file;
    this.init();
  }

  private void init() {
    this.setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,50dlu:grow,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    {
      JPanel head = new JPanel(new FlowLayout(FlowLayout.LEADING));
      head.add(new JLabel(textProvider.getString("backup.restore.tag.head")));
      this.add(head, cc.xy(2, 2));
    }

    {
      JPanel tagPanel = new JPanel(new GridLayout(-1, 2));
      tagPanel.setBackground(Color.WHITE);

      try {
        Map<String, String> availableTags = backupService.getAvailableTags(file);
        List<String> tags = new ArrayList<String>(availableTags.keySet());
        Collections.sort(tags);

        for (String tag : tags) {
          JCheckBox cb = new JCheckBox(tag);
          cb.putClientProperty("tagname", tag);
          cb.putClientProperty("tagfile", availableTags.get(tag));
          tagPanel.add(cb);
          cb.setBackground(Color.WHITE);
          this.tagCbs.add(cb);
        }

      } catch (IOException e) {

      }

      this.add(new JScrollPane(tagPanel), cc.xy(2, 4, CellConstraints.FILL, CellConstraints.TOP));
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
    private static final long serialVersionUID = 2532021826036429380L;
    private volatile String status = "";
    private volatile int cnt;

    /**
     * @param ctx
     */
    public ImportAction() {
      super(getRootPane());
      this.putValue(Action.NAME, textProvider.getString("backup.restore.import"));
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
      for (JCheckBox cb : tagCbs) {
        if (cb.isEnabled() && cb.isSelected()) {
          String tagname = (String) cb.getClientProperty("tagname");
          String tagfile = (String) cb.getClientProperty("tagfile");
          this.status = textProvider.getString("backup.restore.tag.import.status", tagname);
          backupService.restoreTag(file, tagname, tagfile);
          cnt++;
        }
      }

    }

    /**
     * @see de.stationadmin.gui.util.ThreadedAction#showError(java.lang.Exception)
     */
    @Override
    protected void showError(Exception e) {
      JXErrorPane.showDialog(AppUtils.getRootFrame(), textProvider.createErrorInfo(e, "backup.restore.tag.import.failed"));
    }

    /**
     * @see de.stationadmin.gui.util.ThreadedAction#onSuccess()
     */
    @Override
    protected void onSuccess() {
      if (cnt > 0) {
        JOptionPane.showMessageDialog(AppUtils.getRootFrame(), textProvider.getString("backup.restore.tag.import.succeeded", Integer.toString(cnt)));
        for (JCheckBox cb : tagCbs) {
          if (cb.isEnabled() && cb.isSelected()) {
            cb.setSelected(false);
          }
        }
      }
    }

  }

}
