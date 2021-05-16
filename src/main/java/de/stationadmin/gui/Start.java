/**
 * 
 */
package de.stationadmin.gui;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.util.AbstractBean;
import de.stationadmin.gui.util.AppUtils;
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

	public static void configureLogging() {
		try {

			String dataDirectory = System.getProperty("user.home") + File.separatorChar + "laut.fm" + File.separatorChar
					+ "StationAdmin" + File.separatorChar;
			RollingFileAppender appender = new RollingFileAppender(new PatternLayout("%d [%t] %p %c %x - %m%n"),
					dataDirectory + "stationadmin.log");
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
			String laf = Preferences.userRoot().get("stationadmin.lookandfeel", null);
			AppUtils.setLookAndFeel(laf);
		} catch (Exception e) {
		}

		try {
			String token = args.length > 1 ? args[0] : null;
			String stationName = args.length > 1 ? args[1] : null;
			if (admin.getCtx().getAdminClient() != null || admin.login(token, stationName)) {
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

	public boolean login(String token, String stationName) {

		if (token != null && stationName != null) {
			// try to log in with passed token and station name
			try {
				LautfmAdminService service = new LautfmAdminService(token, "StationAdmin");
				List<Station> stations = service.getStations();
				for (Station s : stations) {
					if (s.getName().equalsIgnoreCase(stationName)) {
						StationAdminClient client = new StationAdminClient(service, s);
						ctx.setAdminClient(client);
						return true;
					}
				}
			} catch (Exception e) {
			}

		}

		boolean autologin = Preferences.userRoot().getBoolean("autologin", false);
		if (autologin) {
			// try autologin
			token = Preferences.userRoot().get("token", null);
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

		// log in via dialog
		LoginDlg loginDlg = new LoginDlg(ctx);
		loginDlg.setModal(true);
		loginDlg.setVisible(true);

		return ctx.getAdminClient() != null;

	}

	public void showMain() throws InterruptedException, InvocationTargetException {
		AbstractBean.setEventsInEDT(true);

		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(final Thread t, final Throwable e) {
				Logger.getLogger(Start.class).error("Uncaught exception in thread " + t.getName(), e);
				if (SwingUtilities.isEventDispatchThread()) {
					ErrorInfo errorInfo = getCtx().createErrorInfo(e, "unexpectederror", e.getMessage());
					JXErrorPane.showDialog(AppUtils.getRootFrame(), errorInfo);
				}
			}
		});

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
