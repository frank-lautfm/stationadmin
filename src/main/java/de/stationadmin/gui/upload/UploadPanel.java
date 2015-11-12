/**
 * 
 */
package de.stationadmin.gui.upload;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.trackimport.MP3TrackImportTask;
import de.stationadmin.base.playlist.trackimport.TrackImportTask.Status;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.upload.mix.MixUploadWizard;
import de.stationadmin.gui.util.SwingTools;

/**
 * 
 * @author Frank Korf
 */
public class UploadPanel extends JPanel {
  private static final long serialVersionUID = 5080274104355318762L;
  private ClientContext ctx;
  private UploadManager uploadManager;
  private UploadProgressPanel progressPanel;
  private UploadedTitlePanel titlePanel;
  private List<TitleConfirmationInterceptor> confirmationInterceptors;

  public UploadPanel(ClientContext ctx, List<TitleConfirmationInterceptor> confirmationInterceptors) {
    super();
    this.ctx = ctx;
    this.confirmationInterceptors = confirmationInterceptors;
    this.uploadManager = new UploadManager(this.ctx.getAdminClient());
    this.uploadManager.addPropertyChangeListener("numberOfRemainingFiles", new PropertyChangeListener() {

      /**
       * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
       */
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (((Integer) evt.getOldValue()).intValue() == 0) {
          startIfNecessary();
        }
      }

    });
    this.init();
  }

  private void init() {
    this.setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,pref:grow,5dlu,pref:grow,5dlu"));
    CellConstraints cc = new CellConstraints();

    JToolBar toolbar = new JToolBar();
    toolbar.add(new RestartAction());
    toolbar.addSeparator();
    toolbar.add(new AddFilesAction());
    toolbar.add(new AddDJMixAction());
    this.add(toolbar, cc.xy(2, 2));

    this.progressPanel = new UploadProgressPanel(ctx, uploadManager);
    this.add(this.progressPanel, cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));

    this.titlePanel = new UploadedTitlePanel(ctx, this.confirmationInterceptors);
    this.add(this.titlePanel, cc.xy(2, 6, CellConstraints.FILL, CellConstraints.FILL));

    this.titlePanel.refresh();
  }

  public void suspend() {
    this.progressPanel.stop();
  }

  public void resume() {
    this.progressPanel.restart();
    this.titlePanel.refresh();
  }

  protected void afterUpload() {
    SwingUtilities.invokeLater(new Runnable() {

      @Override
      public void run() {
        titlePanel.refresh();
      }

    });
  }

  public void addFiles(File[] files) {
    for (File file : DupeTitleDlg.removeDupes(ctx.getTextProvider(), ctx.getAdminClient().getTrackService()
        .getTrackRegistry(), Arrays.asList(files))) {
      this.uploadManager.add(file);
    }
  }

  private synchronized void startIfNecessary() {
    if (this.uploadManager.getNumberOfRemainingFiles() > 0 && !uploadManager.isRunning()) {
      Thread t = new Thread(new Runnable() {

        @Override
        public void run() {
          try {
            uploadManager.run();
          } catch (Exception e) {
            final ErrorInfo errorInfo = ctx.getTextProvider().createErrorInfo(e, "upload.msg.failed");
            SwingUtilities.invokeLater(new Runnable() {

              public void run() {
                JXErrorPane.showDialog(ctx.getRootWindow(), errorInfo);
              }

            });
          } finally {
            afterUpload();
          }
        }
      });
      t.start();
    }

  }

  private class RestartAction extends AbstractAction {
    private static final long serialVersionUID = 8586247476016511594L;

    RestartAction() {
      this.putValue(Action.SMALL_ICON, ctx.getIcon("player_play.png"));
      this.putValue(Action.SHORT_DESCRIPTION, ctx.getTextProvider().getString("upload.action.start.tooltip"));

      PropertyChangeListener cl = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          checkEnabled();
        }
      };
      uploadManager.addPropertyChangeListener("running", cl);
      uploadManager.addPropertyChangeListener("numberOfRemainingFiles", cl);

      checkEnabled();
    }

    void checkEnabled() {
      this.setEnabled(!uploadManager.isRunning() && uploadManager.getNumberOfRemainingFiles() > 0);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      startIfNecessary();
    }

  }

  private class AddFilesAction extends AbstractAction {
    private static final long serialVersionUID = 4576020253387010135L;
    private File currentDir;

    AddFilesAction() {
      this.putValue(Action.SMALL_ICON, ctx.getIcon("schedule_import.png"));
      this.putValue(Action.SHORT_DESCRIPTION, ctx.getTextProvider().getString("upload.action.open.tooltip"));
      String last = ctx.getAdminClient().getSettings().getMp3Root();
      if (last == null) {
        last = Preferences.userRoot().get("upload.last", null);
      }
      if (last != null) {
        this.currentDir = new File(last);
      }
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      JFileChooser fileChooser = new JFileChooser();
      if (this.currentDir != null) {
        fileChooser.setCurrentDirectory(this.currentDir);
      }
      fileChooser.setFileFilter(new FileNameExtensionFilter("mp3", "mp3"));
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fileChooser.setMultiSelectionEnabled(true);
      if (fileChooser.showOpenDialog(ctx.getRootWindow()) == JFileChooser.APPROVE_OPTION) {
        if (fileChooser.getSelectedFiles().length > 0) {
          addFiles(fileChooser.getSelectedFiles());
        }
        try {
          Preferences.userRoot().put("upload.last", fileChooser.getCurrentDirectory().getAbsolutePath());
        } catch (Exception e) {
        }
      }
    }

  }

  private class AddDJMixAction extends AbstractAction {
    private static final long serialVersionUID = 492090339809561508L;

    AddDJMixAction() {
      super("DJ Mix");
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent arg0) {
      MixUploadWizard wiz = new MixUploadWizard(ctx);
      wiz.setVisible(true);
    }

  }

}
