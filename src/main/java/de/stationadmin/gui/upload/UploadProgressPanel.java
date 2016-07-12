/**
 * 
 */
package de.stationadmin.gui.upload;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.track.upload.QueuedTrack;
import de.stationadmin.base.track.upload.UploadManager;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.ComponentFactory;

/**
 * 
 * @author Frank Korf
 * 
 */
public class UploadProgressPanel extends JPanel {
  private static final long serialVersionUID = 9192902816894066612L;
  private ClientContext ctx;
  private UploadManager uploadManager;
  private DefaultListModel<QueuedTrack> remainingFilesModel;
  private JXList remainingFilesList;
  private JProgressBar currentFileProgress;
  private JProgressBar totalProgress;
  private JLabel statusLabel;
  private Timer timer;

  public UploadProgressPanel(ClientContext ctx, UploadManager uploadManager) {
    super();
    this.ctx = ctx;
    this.uploadManager = uploadManager;
    this.init();
    this.rebuildListModel();

  }

  private void init() {
    this.remainingFilesModel = new DefaultListModel<QueuedTrack>();
    this.remainingFilesList = new JXList(this.remainingFilesModel);
    this.remainingFilesList.setCellRenderer(new FilenameListCellRenderer());
    this.remainingFilesList.addHighlighter(new AbstractHighlighter() {
      Font bigFont = new Font(ComponentFactory.defaultLabelFont.getFamily(), Font.BOLD, ComponentFactory.defaultLabelFont.getSize() + 2);

      @Override
      protected Component doHighlight(Component component, ComponentAdapter adapter) {
        if (adapter.row == 0) {
          component.setFont(bigFont);
        }
        return component;
      }

    });
    
    remainingFilesList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
    remainingFilesList.getActionMap().put("delete", new AbstractAction() {
      private static final long serialVersionUID = 6284472877932977584L;

      @Override
      public void actionPerformed(ActionEvent e) {
        Object[] values = remainingFilesList.getSelectedValues();
        if(values != null) {
          for(Object value : values) {
            uploadManager.removeFile(((QueuedTrack)value).getFile().getFile());
          }
        }
      }
      
    });
    
    remainingFilesList.setTransferHandler(new UploadTransferHandler(this.ctx.getTextProvider(), this.uploadManager));


    this.currentFileProgress = new JProgressBar();
    this.totalProgress = new JProgressBar();
    this.statusLabel = new JLabel();

    this.setLayout(new FormLayout("pref:grow", "3dlu,pref:grow,5dlu,pref,3dlu"));
    CellConstraints cc = new CellConstraints();
    this.add(new JScrollPane(this.remainingFilesList), cc.xy(1, 2, CellConstraints.FILL, CellConstraints.FILL));

    JPanel progressPanel = new JPanel(new FormLayout("3dlu,pref,5dlu,pref:grow,3dlu", "3dlu,pref,3dlu,pref,3dlu,pref,3dlu"));

    progressPanel.add(new JLabel(ctx.getTextProvider().getString("upload.progress.status")), cc.xy(2, 2));
    progressPanel.add(this.statusLabel, cc.xy(4, 2, CellConstraints.FILL, CellConstraints.FILL));
    progressPanel.add(new JLabel(ctx.getTextProvider().getString("upload.progress.current")), cc.xy(2, 4));
    progressPanel.add(this.currentFileProgress, cc.xy(4, 4, CellConstraints.FILL, CellConstraints.FILL));
    progressPanel.add(new JLabel(ctx.getTextProvider().getString("upload.progress.total")), cc.xy(2, 6));
    progressPanel.add(this.totalProgress, cc.xy(4, 6, CellConstraints.FILL, CellConstraints.FILL));

    progressPanel.setBorder(BorderFactory.createTitledBorder(ctx.getTextProvider().getString("upload.section.progress")));

    this.add(progressPanel, cc.xy(1, 4));

    timer = new Timer(500, new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        {
          currentFileProgress.setMaximum(uploadManager.getProgressListener().getCurrentFileMax());
          currentFileProgress.setValue(uploadManager.getProgressListener().getCurrentFileUploaded());

          int maxKb = uploadManager.getProgressListener().getCurrentFileMax() / 1024;
          int uploadedKb = uploadManager.getProgressListener().getCurrentFileUploaded() / 1024;
          currentFileProgress.setToolTipText(uploadedKb + " / " + maxKb + " kb");
        }

        {
          totalProgress.setMaximum(uploadManager.getProgressListener().getTotalMax());
          totalProgress.setValue(uploadManager.getProgressListener().getTotalUploaded());
          int maxKb = uploadManager.getProgressListener().getTotalMax() / 1024;
          int uploadedKb = uploadManager.getProgressListener().getTotalUploaded() / 1024;
          totalProgress.setToolTipText((uploadManager.getCurrentIndex() + 1) + " / " + uploadManager.getQueue().size() + " Dateien, " + uploadedKb
              + " / " + maxKb + " kb");

        }
        
        {
          if(uploadManager.getNumberOfRemainingFiles() == 0 || !uploadManager.isRunning()) {
            statusLabel.setText("");
          }
          else if(uploadManager.getProgressListener().getCurrentFileUploaded() < uploadManager.getProgressListener().getCurrentFileMax()) {
            statusLabel.setText(ctx.getTextProvider().getString("upload.progress.status.transfer"));
          }
          else {
            statusLabel.setText(ctx.getTextProvider().getString("upload.progress.status.encode"));
          }
        }
        
      }

    });
    this.timer.start();

    this.uploadManager.addPropertyChangeListener("numberOfRemainingFiles", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        rebuildListModel();
      }

    });

  }

  protected void rebuildListModel() {
    this.remainingFilesModel.removeAllElements();
    List<QueuedTrack> queue = this.uploadManager.getQueue();
    int start = this.uploadManager.getCurrentIndex();
    for (int i = start; i < queue.size(); i++) {
      QueuedTrack entry = queue.get(i);
      this.remainingFilesModel.addElement(entry);
    }
  }

  public void stop() {
    this.timer.stop();
  }

  public void restart() {
    if (!this.timer.isRunning()) {
      this.timer.start();
    }
  }

}
