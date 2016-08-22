/**
 * 
 */
package de.stationadmin.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import org.apache.commons.lang.SystemUtils;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXStatusBar;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;

import de.stationadmin.base.StationStatus;
import de.stationadmin.base.Status;
import de.stationadmin.base.Version;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.schedule.Schedule;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.Title;
import de.stationadmin.gui.backup.BackupCreateAction;
import de.stationadmin.gui.backup.BackupRestoreAction;
import de.stationadmin.gui.help.AboutDisplayAction;
import de.stationadmin.gui.help.HelpWindowDisplayAction;
import de.stationadmin.gui.live.MP3StreamerOpenAction;
import de.stationadmin.gui.loganalyzer.dailysummary.DailySummariesOpenAction;
import de.stationadmin.gui.loganalyzer.listeners.ListenersAnalyzerOpenAction;
import de.stationadmin.gui.loganalyzer.plays.PlaysAnalyzerOpenAction;
import de.stationadmin.gui.loganalyzer.plays.UnplayedTracksViewerOpenAction;
import de.stationadmin.gui.migration.MigrationDlgDisplayAction;
import de.stationadmin.gui.mp3explorer.MP3ExplorerDisplayAction;
import de.stationadmin.gui.playlist.PlaylistContainer;
import de.stationadmin.gui.playlist.PlaylistEntryJumpTarget;
import de.stationadmin.gui.playlist.PlaylistNewAction;
import de.stationadmin.gui.playlist.PlaylistTrackSearchOpenAction;
import de.stationadmin.gui.playlist.ResetModifiedPlaylistsAction;
import de.stationadmin.gui.playlist.SaveModifiedPlaylistsAction;
import de.stationadmin.gui.playlist.forecast.ForecastDisplayAction;
import de.stationadmin.gui.playlist.tools.DupeFinderDisplayAction;
import de.stationadmin.gui.playlist.tools.MultiPlaylistShuffleDisplayAction;
import de.stationadmin.gui.playlist.tools.TempPlaylistDisplayAction;
import de.stationadmin.gui.radioctrl.StartRadioAction;
import de.stationadmin.gui.schedule.ScheduleEditorDisplayAction;
import de.stationadmin.gui.settings.SettingsDisplayAction;
import de.stationadmin.gui.subscriptions.SubscriptionManagerDisplayAction;
import de.stationadmin.gui.tag.TagManagerDisplayAction;
import de.stationadmin.gui.tasks.TaskManagerDisplayAction;
import de.stationadmin.gui.track.RegisteredTracksViewer;
import de.stationadmin.gui.track.TrackAliasManagerDisplayAction;
import de.stationadmin.gui.upload.UploadAction;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.JumpLabel;
import de.stationadmin.gui.util.ThreadedAction;
import de.stationadmin.gui.util.TitledPanel;

/**
 * Main window of the application
 * 
 * @author korf
 */
public class StationAdminWindow extends StationAdminFrame {
  private static final long serialVersionUID = 7132155219541632093L;

  private boolean exitOnClose = true;
  private boolean multiWindow = false;

  private JComponent infoPanel, playlistPanel, titleViewer;
  private JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
  private Map<String, JFrame> externalWindows = new HashMap<String, JFrame>();
  private boolean minimizesToTray = false;

  public StationAdminWindow(ClientContext ctx) throws HeadlessException {
    super(ctx);
    AppUtils.setRootFrame(this);
    this.init();
  }

  public void setIconified(boolean iconified) {
    this.setState(iconified ? JFrame.ICONIFIED : JFrame.NORMAL);
  }

  private JLabel createStatusBarLabel(String key) {
    JLabel label = new JLabel(this.ctx.getTextProvider().getString(key) + ":");
    label.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
    return label;
  }

  private void init() {
    this.initTray();
    this.getContentPane().setLayout(new BorderLayout());
    this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new WindowAdapter() {

      @Override
      public void windowClosing(WindowEvent e) {
        if (exitOnClose) {
          close();
        }
      }

      public void windowIconified(WindowEvent e) {
        if (minimizesToTray) {
          setVisible(false);
        }
      }

    });

    this.setTitle(this.ctx.getTextProvider().getString("app.title") + " " + Version.VERSION + " - " + ctx.getAdminClient().getStation());

    this.infoPanel = new StationInfoPanel(ctx);
    this.playlistPanel = new PlaylistContainer(ctx);
    this.titleViewer = new TitledPanel("Titel", new RegisteredTracksViewer(ctx));
    this.setMultiWindow(false);

    this.getContentPane().add(this.initToolbar(), BorderLayout.NORTH);

    this.ctx.getJumpHandler().addJumpListener(new JumpListener() {

      @Override
      public void jumpTo(Object target) {
        if (!multiWindow) {
          if (target instanceof Playlist || target instanceof PlaylistEntryJumpTarget) {
            tabPane.setSelectedIndex(1);
          }
        }
      }

    });

    {
      final JXStatusBar statusBar = new JXStatusBar();
      statusBar.setOpaque(false);

      JPanel stationPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
      stationPanel.add(this.createStatusBarLabel("station"));
      JLabel stationLabel = new JLabel(ctx.getAdminClient().getStation());
      stationLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
      stationPanel.add(stationLabel);
      statusBar.add(stationPanel, new JXStatusBar.Constraint());

      PresentationModel<StationStatus> statusModel = new PresentationModel<StationStatus>(this.ctx.getAdminClient().getStationStatus());

      JPanel listenersPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
      listenersPanel.add(this.createStatusBarLabel("currentListeners"));
      JLabel listenersLabel = BasicComponentFactory.createLabel(statusModel.getModel("currentListeners"), NumberFormat.getIntegerInstance());
      listenersLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
      listenersPanel.add(listenersLabel);
      statusBar.add(listenersPanel, new JXStatusBar.Constraint());

      JPanel rankPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
      rankPanel.add(this.createStatusBarLabel("rank"));
      JLabel rankLabel = BasicComponentFactory.createLabel(statusModel.getModel("rank"), NumberFormat.getIntegerInstance());
      rankLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
      rankPanel.add(rankLabel);
      statusBar.add(rankPanel, new JXStatusBar.Constraint());

      {
        JPanel playlistPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        playlistPanel.add(this.createStatusBarLabel("currentPlaylist"));
        final JumpLabel playlistlLabel = new JumpLabel(ctx.getJumpHandler()) {
          private static final long serialVersionUID = -2733145329622949529L;

          /**
           * @see de.stationadmin.gui.util.JumpLabel#getJumpTarget()
           */
          @Override
          protected Object getJumpTarget() {
            int playlistId = ctx.getAdminClient().getSchedule().getCurrent().getPlaylistId();
            return ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylist(playlistId);
          }

        };
        playlistlLabel.setToolTipText(ctx.getTextProvider().getString("currentPlaylist.tooltip"));
        updateCurrentPlaylist(playlistlLabel);
        ctx.getAdminClient().getSchedule().addPropertyChangeListener("current", new PropertyChangeListener() {

          @Override
          public void propertyChange(PropertyChangeEvent evt) {
            updateCurrentPlaylist(playlistlLabel);
            statusBar.validate();
            statusBar.repaint();
          }

        });

        playlistPanel.add(playlistlLabel);
        statusBar.add(playlistPanel, new JXStatusBar.Constraint());
      }

      // on double click jump to title in playlist
      {
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
        titlePanel.add(this.createStatusBarLabel("currentTitle"));
        final JumpLabel titleLabel = new JumpLabel(ctx.getJumpHandler()) {
          private static final long serialVersionUID = 5738599837330798922L;

          @Override
          protected Object getJumpTarget() {
            int playlistId = ctx.getAdminClient().getSchedule().getCurrent() != null ? ctx.getAdminClient().getSchedule().getCurrent().getPlaylistId() : 0;
            Playlist playlist = ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylist(playlistId);

            int titleId = ctx.getAdminClient().getStationStatus().getCurrentTrackId();
            Title title = ctx.getAdminClient().getTrackService().getTrackRegistry().getTrack(titleId);

            return new PlaylistEntryJumpTarget(playlist, title);
          }

        };
        updateTitle(titleLabel);
        titleLabel.setToolTipText(ctx.getTextProvider().getString("currentTitle.tooltip"));
        statusModel.getModel("currentTrackId").addValueChangeListener(new PropertyChangeListener() {

          @Override
          public void propertyChange(PropertyChangeEvent evt) {
            updateTitle(titleLabel);
            statusBar.validate();
            statusBar.repaint();
          }

        });
        titlePanel.add(titleLabel);
        statusBar.add(titlePanel, new JXStatusBar.Constraint());

      }

      this.getContentPane().add(statusBar, BorderLayout.SOUTH);
    }

    this.setIconImage(Toolkit.getDefaultToolkit().getImage(this.getClass().getClassLoader().getResource("icons/trayicon.png")));

    this.initMenu();

    this.ctx.getAdminClient().getSubscriptionService().addPropertyChangeListener("newMatches", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        String msg = ctx.getString("subscription.info.new.msg", evt.getNewValue().toString());
        JOptionPane.showMessageDialog(ctx.getRootWindow(), msg, ctx.getString("subscription.info.new.title"), JOptionPane.INFORMATION_MESSAGE);
      }
    });

  }

  private AbstractAction asToolbarAction(AbstractAction action, String icon) {
    String name = (String) action.getValue(Action.NAME);
    action.putValue(Action.NAME, null);
    action.putValue(Action.SMALL_ICON, ctx.getIcon(icon));
    action.putValue(Action.SHORT_DESCRIPTION, name);
    return action;
  }

  private JToolBar initToolbar() {

    boolean djOnly = ctx.getAdminClient().getSessionCtx().isDJOnly();

    JToolBar toolbar = new JToolBar();

    toolbar.add(asToolbarAction(new SynchronizeAction(ctx.getTextProvider(), ctx.getAdminClient()), "synchronize.png"));
    toolbar.add(asToolbarAction(new SaveModifiedPlaylistsAction(ctx.getTextProvider(), ctx.getAdminClient().getPlaylistService(), ctx.getAdminClient().getSchedule()), "save_all.png"));
    toolbar.add(asToolbarAction(new ResetModifiedPlaylistsAction(ctx.getTextProvider(), ctx.getAdminClient().getPlaylistService()), "undo.png"));
    toolbar.add(asToolbarAction(new UploadAction(this.ctx), "upload.png"));
    if (!djOnly) {
      toolbar.add(asToolbarAction(new ScheduleEditorDisplayAction(this.ctx), "schedule.png"));
      toolbar.add(asToolbarAction(new TaskManagerDisplayAction(this.ctx), "tasks.png"));
    }
    toolbar.addSeparator();
    toolbar.add(asToolbarAction(new MultiPlaylistShuffleDisplayAction(this.ctx), "shuffle.png"));
    toolbar.addSeparator();

    final JToggleButton winBtn = new JToggleButton(this.ctx.getIcon("windowtoggle.png"));
    winBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        if (winBtn.isSelected()) {
          setMultiWindow(true);
        } else {
          setMultiWindow(false);
        }
      }
    });
    winBtn.setToolTipText(this.ctx.getTextProvider().getString("action.windowtoggle.tooltip"));
    toolbar.add(winBtn);

    return toolbar;
  }

  private void initTray() {
    if (SystemTray.isSupported() && !SystemUtils.IS_OS_MAC_OSX) {
      this.minimizesToTray = true;
      Icon icon = ctx.getIcon("trayicon.png");
      Image img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_4BYTE_ABGR);
      icon.paintIcon(new Panel(), img.getGraphics(), 0, 0);

      final TrayIcon trayIcon = new TrayIcon(img, "Station Admin");
      trayIcon.setImageAutoSize(true);
      try {
        SystemTray.getSystemTray().add(trayIcon);
        // this.setExitOnClose(false);

        trayIcon.addActionListener(new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent e) {
            if (StationAdminWindow.this.isVisible() == false) {
              StationAdminWindow.this.setVisible(true);
            }
            StationAdminWindow.this.setIconified(false);
          }

        });

        final PopupMenu popup = new PopupMenu();
        MenuItem close = new MenuItem(ctx.getTextProvider().getString("action.exit"));
        close.addActionListener(new ExitAction(ctx));
        popup.add(close);

        trayIcon.setPopupMenu(popup);

        ctx.getAdminClient().getStationStatus().addPropertyChangeListener(new PropertyChangeListener() {

          @Override
          public void propertyChange(PropertyChangeEvent evt) {
            int titleId = ctx.getAdminClient().getStationStatus().getCurrentTrackId();
            Title title = ctx.getAdminClient().getTrackService().getTrackRegistry().getTrack(titleId);
            String tooltip = ctx.getAdminClient().getStationStatus().getCurrentListeners() + " Hörer";
            if (title != null) {
              tooltip += " - " + title.getArtist() + ": " + title.getTitle();
            }
            tooltip += " (" + ctx.getTextProvider().getString("rank") + " " + ctx.getAdminClient().getStationStatus().getRank() + ")";
            trayIcon.setToolTip(tooltip);
          }

        });

      } catch (Exception e) {
      }

    }

  }

  protected void close() {
    new ExitAction(ctx).actionPerformed(new ActionEvent(this, (int) System.currentTimeMillis(), "close"));
  }

  private void updateTitle(JumpLabel label) {
    RegisteredTrack title = ctx.getAdminClient().getTrackService().getTrackRegistry().getTrack(ctx.getAdminClient().getStationStatus().getCurrentTrackId());
    if (title != null) {
      label.setText(title.getArtist() + " - " + title.getTitle());
    } else {
      label.setText("");
    }
  }

  private void updateCurrentPlaylist(JumpLabel label) {
    Schedule.Entry entry = this.ctx.getAdminClient().getSchedule().getCurrent();
    int playlistId = entry != null ? entry.getPlaylistId() : 0;
    Playlist playlist = this.ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylist(playlistId);
    if (playlist != null) {
      label.setText(playlist.getDisplayName());
    } else {
      label.setText("");
    }
  }

  private void initMenu() {
    JMenuBar menuBar = new JMenuBar();

    boolean djOnly = ctx.getAdminClient().getSessionCtx().isDJOnly();

    {
      JMenu menuServer = new JMenu(this.ctx.getTextProvider().getString("menu.server"));
      menuServer.add(new SynchronizeAction(this.ctx.getTextProvider(), ctx.getAdminClient()));
      menuServer.add(new ReloadOwnTitlesAction(ctx.getTextProvider(), ctx.getAdminClient().getTrackService()));
      menuServer.add(new SaveModifiedPlaylistsAction(ctx.getTextProvider(), ctx.getAdminClient().getPlaylistService(), ctx.getAdminClient().getSchedule()));
      menuServer.add(new ResetModifiedPlaylistsAction(ctx.getTextProvider(), ctx.getAdminClient().getPlaylistService()));
      menuServer.addSeparator();
      menuServer.add(new UploadAction(this.ctx));
      if (!djOnly) {
        menuServer.add(new ScheduleEditorDisplayAction(this.ctx));
        menuServer.addSeparator();
        menuServer.add(new MP3StreamerOpenAction(this.ctx));
        menuServer.addSeparator();
        menuServer.add(new StartRadioAction(this.ctx));
        menuServer.addSeparator();
      }
      menuServer.add(new ExitAction(this.ctx));
      menuBar.add(menuServer);
    }

    {
      JMenu menuPlaylists = new JMenu(this.ctx.getTextProvider().getString("menu.playlist"));
      menuPlaylists.add(new PlaylistNewAction(this.ctx, null));
      menuPlaylists.add(new TempPlaylistDisplayAction(this.ctx));
      menuPlaylists.add(new MultiPlaylistShuffleDisplayAction(this.ctx));
      menuPlaylists.add(new PlaylistTrackSearchOpenAction(this.ctx));
      menuPlaylists.add(new DupeFinderDisplayAction(this.ctx));
      if (!djOnly) {
        menuPlaylists.add(new ForecastDisplayAction(this.ctx));
      }
      menuBar.add(menuPlaylists);
    }

    {
      JMenu menuTitles = new JMenu(this.ctx.getTextProvider().getString("menu.title"));
      menuTitles.add(new TagManagerDisplayAction(this.ctx));
      menuTitles.add(new TrackAliasManagerDisplayAction(this.ctx));
      // menuTitles.add(new SubscriptionManagerDisplayAction(ctx));
      menuTitles.add(new MP3ExplorerDisplayAction(this.ctx));
      menuBar.add(menuTitles);
    }

    if (!djOnly) {
      JMenu menuAnalyze = new JMenu(this.ctx.getTextProvider().getString("menu.analyze"));
      menuAnalyze.add(new PlaysAnalyzerOpenAction(this.ctx));
      menuAnalyze.add(new UnplayedTracksViewerOpenAction(this.ctx));
      menuAnalyze.add(new ListenersAnalyzerOpenAction(this.ctx));
      menuAnalyze.add(new DailySummariesOpenAction(this.ctx));

      menuBar.add(menuAnalyze);
    }

    {
      JMenu menuBackup = new JMenu(this.ctx.getTextProvider().getString("menu.backup"));
      if (!djOnly) {
        menuBackup.add(new TaskManagerDisplayAction(ctx));
        menuBackup.addSeparator();
        menuBackup.add(new BackupCreateAction(ctx.getTextProvider(), ctx.getAdminClient()));
        menuBackup.add(new BackupRestoreAction(this.ctx));
        menuBackup.addSeparator();
        menuBackup.add(new MigrationDlgDisplayAction(ctx));
        menuBackup.addSeparator();
      }
      menuBackup.add(new SettingsDisplayAction(this.ctx));
      menuBar.add(menuBackup);
    }

    {
      JMenu menuHelp = new JMenu(this.ctx.getTextProvider().getString("menu.help"));
      menuHelp.add(new HelpWindowDisplayAction(this.ctx));
      menuHelp.addSeparator();
      menuHelp.add(new AboutDisplayAction(this.ctx));

      menuBar.add(menuHelp);
    }

    this.setJMenuBar(menuBar);

  }

  /**
   * @return the exitOnClose
   */
  public boolean isExitOnClose() {
    return exitOnClose;
  }

  public void initAdminClient() {
    Initializer action = new Initializer(ctx);
    action.actionPerformed(new ActionEvent(this, 0, "init"));
  }

  /**
   * @param exitOnClose
   *          the exitOnClose to set
   */
  public void setExitOnClose(boolean exitOnClose) {
    this.exitOnClose = exitOnClose;
  }

  private class Initializer extends ThreadedAction {
    /**
     * @param ctx
     */
    public Initializer(ClientContext ctx) {
      super(ctx);
    }

    private static final long serialVersionUID = -8327492919610047417L;

    @Override
    protected String getStatus() {
      Status status = ctx.getAdminClient().getStatus();
      if (status != null) {
        return ctx.getTextProvider().getString("status." + status.getKey(), status.getParameters());
      } else {
        return ctx.getTextProvider().getString("action.synchronize.msg");
      }
    }

    @Override
    protected void performAction() throws Exception {
      Exception exception = null;
      try {
        ctx.getAdminClient().load();
        ctx.getAdminClient().initBackgroundTasks();
      } catch (Exception e) {
        exception = e;
      }

      ctx.getAdminClient().getPlaylistService().getPlaylistModificationDetector().addPropertyChangeListener("modified", new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          if (ctx.getAdminClient().getPlaylistService().getPlaylistModificationDetector().isModified()) {
            SynchronizationDialog dlg = new SynchronizationDialog(ctx.getTextProvider(), ctx.getAdminClient());
            dlg.setModal(true);
            dlg.setVisible(true);
          }
        }

      });

      if (exception != null) {
        throw exception;
      }
    }

    @Override
    protected void showError(Exception e) {
      JXErrorPane.showDialog(null, ctx.getTextProvider().createErrorInfo(e, "action.clientinit.error"));
    }

  }

  /**
   * @return the multiWindow
   */
  public boolean isMultiWindow() {
    return multiWindow;
  }

  /**
   * @param multiWindow
   *          the multiWindow to set
   */
  public void setMultiWindow(boolean multiWindow) {
    this.multiWindow = multiWindow;

    if (this.multiWindow) {
      this.tabPane.removeAll();
      this.getContentPane().remove(this.tabPane);

      this.getContentPane().add(this.infoPanel, BorderLayout.CENTER);

      StationAdminFrame plFrame = new StationAdminFrame(this.ctx, "playlists", this.playlistPanel);
      this.externalWindows.put("playlists", plFrame);
      StationAdminFrame titleFrame = new StationAdminFrame(this.ctx, "registeredtitles", this.titleViewer);
      this.externalWindows.put("titles", titleFrame);

      plFrame.setVisible(true);
      titleFrame.setVisible(true);

    } else {
      this.getContentPane().remove(this.infoPanel);
      this.disposeExternalWindow("playlists");
      this.disposeExternalWindow("titles");

      tabPane.addTab(ctx.getTextProvider().getString("tab.stationinfo"), this.infoPanel);
      tabPane.addTab(ctx.getTextProvider().getString("tab.playlists"), this.playlistPanel);
      tabPane.addTab(ctx.getTextProvider().getString("tab.registeredtitles"), this.titleViewer);
      this.getContentPane().add(tabPane, BorderLayout.CENTER);
    }
    this.validate();
    this.repaint();
  }

  private void disposeExternalWindow(String key) {
    if (this.externalWindows.containsKey(key)) {
      this.externalWindows.get(key).getContentPane().removeAll();
      this.externalWindows.get(key).dispose();
      this.externalWindows.remove(key);
    }

  }

}
