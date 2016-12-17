/**
 * 
 */
package de.stationadmin.gui;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.util.AbstractBean;
import de.stationadmin.lfm.backend.LautfmAdminService;
import de.stationadmin.lfm.backend.Station;

/**
 * Main class to start the GUI application
 * 
 * @author Frank Korf
 */
public class Start {
  private ClientContext ctx = new ClientContext();
  private StationAdminWindow mainWindow;

  private static void configureLogging() {
    try {

      String dataDirectory = System.getProperty("user.home") + File.separatorChar + "laut.fm" + File.separatorChar + "StationAdmin" + File.separatorChar;
      RollingFileAppender appender = new RollingFileAppender(new PatternLayout("%r [%t] %p %c %x - %m%n"), dataDirectory + "stationadmin.log");
      appender.setMaximumFileSize(1024 * 1024 * 3);
      appender.setMaxBackupIndex(10);
      BasicConfigurator.configure(appender);

      Logger.getRootLogger().setLevel(Level.INFO);
      Logger.getLogger(LautfmAdminService.class).setLevel(Level.ERROR);

      System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    System.setProperty("java.net.preferIPv4Stack", "true");
    configureLogging();

    final Start admin = new Start();
    try {
      UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
    } catch (Exception e) {
    }

    try {
      if (admin.getCtx().getAdminClient() != null || admin.login()) {
        admin.showMain();
      }
    } catch (Throwable t) {
      Logger.getLogger(Start.class).error("failed start main application", t);
    }
  }

  public void determinRadioStatus() {
    try {
      ctx.getRadioStatus().setValue(ctx.getAdminClient().isRadioStarted());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * @return the ctx
   */
  public ClientContext getCtx() {
    return ctx;
  }

  public boolean login() {
    // FIXME
    boolean autologin = Preferences.userRoot().getBoolean("autologin", false);
    if (autologin) {
      String token = Preferences.userRoot().get("token", null);
      int stationId = Preferences.userRoot().getInt("station", -1);
      if (token != null && stationId > 0) {
        try {
          LautfmAdminService service = new LautfmAdminService(token, "StationAdmin");
          List<Station> stations = service.getStations();
          for (Station s : stations) {
            if (s.getId() == stationId) {
              StationAdminClient client = new StationAdminClient(service, s);
              ctx.setAdminClient(client);
              return true;
            }
          }

        } catch (Exception e) {
          // autologin failed, proceed with login dialog below
        }
      }
    }

    LoginDlg loginDlg = new LoginDlg(ctx);
    loginDlg.setModal(true);
    loginDlg.setVisible(true);

    return ctx.getAdminClient() != null;

  }

  public void showMain() throws InterruptedException, InvocationTargetException {
    AbstractBean.setEventsInEDT(true);

    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() {
        try {
          mainWindow = new StationAdminWindow(ctx);
          mainWindow.setVisible(true);
          mainWindow.initAdminClient();
        } catch (Throwable t) {
          Logger.getLogger(Start.class).error("failed start main application", t);
          JXErrorPane.showDialog(t);
        }
      }
    });

  }
}
