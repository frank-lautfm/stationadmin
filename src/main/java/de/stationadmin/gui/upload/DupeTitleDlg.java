/**
 * 
 */
package de.stationadmin.gui.upload;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.jdesktop.swingx.JXLabel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.trackimport.MP3TrackImportTask;
import de.stationadmin.base.playlist.trackimport.TrackImportTask.Status;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.AppUtils;

/**
 * @author Frank
 *
 */
public class DupeTitleDlg extends JDialog {
  private static final long serialVersionUID = 5193217843149392055L;
  private TextProvider textProvider;
  private JList list;

  /**
   * @param textProvider
   * @param dupes
   */
  public DupeTitleDlg(TextProvider textProvider, List<File> dupes) {
    super();
    this.textProvider = textProvider;

    DefaultListModel model = new DefaultListModel();
    for (File file : dupes) {
      model.addElement(file);
    }
    list = new JList(model);
    list.setCellRenderer(new FilenameListCellRenderer());
    list.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    list.getSelectionModel().addSelectionInterval(0, dupes.size() - 1);

    this.setTitle(this.textProvider.getString("upload.dupe.title"));

    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,pref:grow,5dlu,pref,5dlu"));

    JXLabel label = new JXLabel();
    label.setText(this.textProvider.getString("upload.dupe.description"));
    label.setLineWrap(true);
    this.getContentPane().add(label, new CellConstraints(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    this.getContentPane().add(new JScrollPane(list),
        new CellConstraints(2, 4, CellConstraints.FILL, CellConstraints.FILL));

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    JButton btn = new JButton("Ok");
    btn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });
    buttonPanel.add(btn);

    this.getContentPane().add(buttonPanel, new CellConstraints(2, 6, CellConstraints.FILL, CellConstraints.FILL));

  }

  public List<File> getSelectedFiles() {
    List<File> files = new ArrayList<File>();
    for (Object obj : list.getSelectedValues()) {
      files.add((File) obj);
    }
    return files;
  }

  public static List<File> removeDupes(TextProvider textProvider, TrackRegistry titleRegistry, List<File> files) {
    // precheck
    List<File> dupes = new ArrayList<File>();
    try {
      for (File file : files) {
        if (file.exists() && !file.isDirectory() && file.getName().toLowerCase().endsWith("mp3")) {
          try {
            MP3TrackImportTask task = new MP3TrackImportTask(file);
            task.resolve();
            if (task.getStatus() == Status.OPEN) {
              List<RegisteredTrack> titles = titleRegistry.search(task.getArtist(), task.getTitle());
              if (titles.size() > 0) {
                dupes.add(file);
              }
            }
          } catch (Exception e) {
            // ignore
          }
        }
      }
      if (dupes.size() > 0) {
        DupeTitleDlg dlg = new DupeTitleDlg(textProvider, dupes);
        dlg.setSize(300, 400);
        dlg.setModal(true);
        AppUtils.centerWithinRoot(dlg);
        dlg.setVisible(true);
        dupes.removeAll(dlg.getSelectedFiles());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    List<File> newFiles = new ArrayList<File>(files);
    newFiles.removeAll(dupes);
    return newFiles;

  }

}
