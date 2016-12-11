package de.stationadmin.gui;

import java.awt.Desktop;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;
import org.jdesktop.swingx.error.ErrorLevel;

import com.jgoodies.binding.value.ValueHolder;

import de.stationadmin.base.ErrorHandler;
import de.stationadmin.base.StationAdminClient;
import de.stationadmin.gui.upload.UploadWindowManager;
import de.stationadmin.gui.util.AppUtils;

/**
 * Context object for all classes of the GUI - provides access to relevant objects and resources
 * 
 * @author Frank Korf
 */
public class ClientContext {

  private StationAdminClient adminClient;
  private TextProvider textProvider = new TextProvider();
  private JumpHandler jumpHandler = new JumpHandler();

  private ValueHolder radioStatus = new ValueHolder(Boolean.FALSE);
  private UploadWindowManager uploadWindowManager = new UploadWindowManager(this);
  private Desktop desktop;

  public ClientContext() {
    if (Desktop.isDesktopSupported()) {
      this.desktop = Desktop.getDesktop();
    }
  }

  public static String getHomeDir() {
    return System.getProperty("user.home") + File.separatorChar + "laut.fm" + File.separatorChar;
  }

  /**
   * Gets a localized string
   * 
   * @param key
   * @param parameters
   * @return
   */
  public String getString(String key, String... parameters) {
    return this.textProvider.getString(key, parameters);
  }

  /**
   * Gets an icon
   * 
   * @param name
   * @return
   */
  public ImageIcon getIcon(String name) {
    return AppUtils.getIcon(name);
  }

  public StationAdminClient getAdminClient() {
    return adminClient;
  }

  public void setAdminClient(StationAdminClient adminClient) {
    this.adminClient = adminClient;
    this.adminClient.registerErrorHandler(new ErrorHandler() {

      @Override
      public void display(String text, Exception e) {
        displayError(text, e);

      }
    });
  }

  protected void displayError(final String text, final Exception e) {
    SwingUtilities.invokeLater(new Runnable() {

      @Override
      public void run() {
        ErrorInfo errorInfo = new ErrorInfo(textProvider.getString("error.title"), text, null, "general", e, ErrorLevel.SEVERE, null);
        JXErrorPane.showDialog(AppUtils.getRootFrame(), errorInfo);
      }
    });

  }

  /**
   * @return the radioStatus
   */
  public ValueHolder getRadioStatus() {
    return radioStatus;
  }

  public ErrorInfo createErrorInfo(Throwable exception, String msgKey, String... msgParameters) {
    return this.textProvider.createErrorInfo(exception, msgKey, msgParameters);
  }

  /**
   * @return the rootWindow
   */
  public JFrame getRootWindow() {
    return AppUtils.getRootFrame();
  }

  /**
   * @return the uploadWindowManager
   */
  public UploadWindowManager getUploadWindowManager() {
    return uploadWindowManager;
  }

  /**
   * @return the desktop
   */
  public Desktop getDesktop() {
    return desktop;
  }

  /**
   * @return the jumpHandler
   */
  public JumpHandler getJumpHandler() {
    return jumpHandler;
  }

  /**
   * @return the textProvider
   */
  public TextProvider getTextProvider() {
    return textProvider;
  }

}
