/**
 * 
 */
package de.stationadmin.gui.backup;

import java.awt.HeadlessException;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.AppUtils;

/**
 * 
 * @author Frank Korf
 * 
 */
public class BackupRestoreDlg extends JFrame {
  private static final long serialVersionUID = -108011662216337228L;
  private ClientContext ctx;
  private File file;
  private TextProvider textProvider;

  public BackupRestoreDlg(ClientContext ctx, File file) throws HeadlessException {
    super();
    this.ctx = ctx;
    this.textProvider = ctx.getTextProvider();
    this.file = file;
    this.init();
  }

  private void init() {
    this.setTitle(textProvider.getString("backup.restore.title"));

    JTabbedPane tabPane = new JTabbedPane();
    tabPane.add(textProvider.getString("backup.restore.tab.playlists"), new PlaylistRestorePanel(this.textProvider, ctx.getAdminClient(), this.file));
    tabPane.add(textProvider.getString("backup.restore.tab.playlists.archive"), new ArchivePlaylistRestorePanel(this.textProvider, ctx.getAdminClient(), this.file));
    tabPane.add(textProvider.getString("backup.restore.tab.titletags"), new TitleTagRestorePanel(this.ctx, this.file));
    tabPane.add(textProvider.getString("backup.restore.tab.tasks"), new TaskRestorePanel(this.ctx, this.file));
    tabPane.add(textProvider.getString("backup.restore.tab.schedule"), new ScheduleRestorePanel(this.ctx, this.file));

    this.getContentPane().setLayout(new FormLayout("3dlu,pref:grow,3dlu", "3dlu,pref:grow,3dlu"));
    this.getContentPane().add(tabPane, new CellConstraints(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    this.setSize(400, 400);
    AppUtils.centerWithinRoot(this);

  }

}
