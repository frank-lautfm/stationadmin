/**
 * 
 */
package de.stationadmin.gui.upload;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import de.stationadmin.gui.ClientContext;

/**
 * Main window for mp3 upload. Basically just a wrapper for {@link UploadPanel}.
 * 
 * @author Frank Korf
 */
public class UploadWindow extends JFrame {
  private static final long serialVersionUID = 8253943750283339802L;
  private UploadPanel uploadPanel;
  private List<TitleConfirmationInterceptor> confirmationInterceptors = Collections
      .synchronizedList(new ArrayList<TitleConfirmationInterceptor>());

  public UploadWindow(ClientContext ctx) {
    this.uploadPanel = new UploadPanel(ctx, this.confirmationInterceptors);

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

  }

  public void addFiles(File... files) {
    this.uploadPanel.addFiles(files);
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      this.uploadPanel.resume();
    }
  }

  public void addConfirmationInterceptor(TitleConfirmationInterceptor ic) {
    this.confirmationInterceptors.add(ic);
  }

  public void removeConfirmationInterceptor(TitleConfirmationInterceptor ic) {
    this.confirmationInterceptors.remove(ic);
  }

}
