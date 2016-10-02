/**
 * 
 */
package de.stationadmin.gui.migration;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;
import de.stationadmin.migration.MessageReceiver;
import de.stationadmin.migration.MigrationUtil;

/**
 * @author korf
 * 
 */
public class MigrationDlg extends StationAdminFrame {
  private static final long serialVersionUID = -5424695336508913245L;

  private JTextArea logTf = new JTextArea(10, 40);
  private MigrateAction action = new MigrateAction();
  private JCheckBox reloadTrackMapping = new JCheckBox("Track-Mapping neu laden");

  /**
   * @param ctx
   * @throws HeadlessException
   */
  public MigrationDlg(ClientContext ctx) throws HeadlessException {
    super(ctx, "migration");
    this.init();
  }

  private void init() {
    this.logTf.setEditable(false);
    this.setTitle("Datenübernahme von Version 3");
    this.setSize(500, 400);
    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(new JScrollPane(this.logTf), BorderLayout.CENTER);
    
    this.reloadTrackMapping.setSelected(new MigrationUtil(ctx.getAdminClient()).isReloadTrackmapping());

    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
    bottom.add(new JButton(this.action));
    bottom.add(this.reloadTrackMapping);
    this.getContentPane().add(bottom, BorderLayout.SOUTH);
  }

  void log(final String msg) {
    SwingUtilities.invokeLater(new Runnable() {

      @Override
      public void run() {
        logTf.append(msg + "\n");
      }
    });
  }

  private class MigrateAction extends AbstractAction {
    private static final long serialVersionUID = 8515275611946529505L;

    MigrateAction() {
      this.putValue(Action.NAME, "Start");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      this.setEnabled(false);

      Thread t = new Thread() {
        public void run() {
          MigrationUtil util = new MigrationUtil(ctx.getAdminClient());
          util.setReloadTrackmapping(reloadTrackMapping.isSelected());

          util.setMessageReceiver(new MessageReceiver() {

            @Override
            public void onMessage(String msg) {
              log(msg);

            }
          });

          try {
            util.init();
            util.migrateSettings();
            util.migrateOnlinePlaylists();
            util.migrateArchivePlaylists();
            util.migrateTrackAliases();
            util.migrateTags();
            util.migrateTasks();
            util.migrateLogs();
            
            log("\nFertig!\n");

          } catch (Exception e) {
            Logger log = Logger.getLogger(MigrationDlg.class);
            log.error("migration error", e);
            JXErrorPane.showDialog(MigrationDlg.this, ctx.getTextProvider().createErrorInfo(e, "migrationerror"));
          } finally {
            action.setEnabled(true);
          }

        }
      };
      t.start();

    }

  }

}
