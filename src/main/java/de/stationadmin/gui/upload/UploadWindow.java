/**
 * 
 */
package de.stationadmin.gui.upload;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import de.stationadmin.base.track.upload.UploadManager;
import de.stationadmin.gui.ClientContext;

/**
 * Main window for mp3 upload. Basically just a wrapper for {@link UploadPanel}.
 * 
 * @author Frank Korf
 */
public class UploadWindow extends JFrame {
  private static final long serialVersionUID = 8253943750283339802L;
  private UploadPanel uploadPanel;
  private boolean hintShown = false;

  public UploadWindow(ClientContext ctx) {
    this.uploadPanel = new UploadPanel(ctx);

    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(uploadPanel, BorderLayout.CENTER);
    this.addWindowListener(new WindowAdapter() {

      @Override
      public void windowClosing(WindowEvent e) {
        uploadPanel.suspend();
      }

    });
    this.setSize(600, 500);
    this.setTitle(ctx.getString("upload.title"));
    this.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

    this.hintShown = Preferences.userRoot().getBoolean("uploadHintShown", false);

  }

  public UploadManager getUploadManager() {
    return this.uploadPanel.getUploadManager();
  }

  public void addFiles(File[] files, boolean forcePrivate) {
    this.uploadPanel.addFiles(files, forcePrivate);
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      this.uploadPanel.resume();
      if (!hintShown) {
        JOptionPane.showMessageDialog(this, "<html>Benutze das Kontextmenü oder drücke die P-Taste um ausgewählte Tracks<br>während des Uploads als 'privat' zu markieren</html>");
        Preferences.userRoot().putBoolean("uploadHintShown", true);
        this.hintShown = true;
      }
    }
  }

}
