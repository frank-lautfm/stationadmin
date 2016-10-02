/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.playlist.trackimport.MP3TrackImportTask;
import de.stationadmin.base.playlist.trackimport.TrackImportHandler;
import de.stationadmin.base.playlist.trackimport.TrackImportTask;
import de.stationadmin.base.playlist.trackimport.TrackImportTask.Status;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.track.SearchPanel;
import de.stationadmin.gui.upload.UploadWindow;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.MenuLabel;
import de.stationadmin.gui.util.SwingTools;

/**
 * Dialog that handles the import of titles based on {@link TrackImportHandler}.
 * <p>
 * It is assumed that tags {@link TrackImportHandler#resolveTags()} and
 * {@link TrackImportHandler#resolveTitlesLocal()} has already been invoked -
 * this dialog guides the user through the rest of the process.
 * 
 * @author Frank Korf
 */
public class TrackImportDlg extends JDialog {
  private static final long serialVersionUID = 7169645093666157021L;
  private StationAdminClient client;
  private TextProvider textProvider;
  private ClientContext ctx;
  private TrackImportHandler handler;
  private RepeatSearchAction repeatSearchAction = new RepeatSearchAction();
  private volatile boolean searchInProgress = false;
  private ValueModel hideResolved = new ValueHolder(Boolean.FALSE);
  private boolean uploadAvailable = false;

  private JPopupMenu activeActionPopup;

  public TrackImportDlg(ClientContext ctx, TrackImportHandler handler) {
    super();
    this.ctx = ctx;
    this.client = ctx.getAdminClient();
    this.textProvider = ctx.getTextProvider();
    this.handler = handler;
    this.init();
  }

  private void init() {

    try {
      this.uploadAvailable = client.isUploadAvailable();
    } catch (Exception e) {
    }

    Container panel = this.getContentPane();
    panel.setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,300dlu,5dlu,pref,8dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    panel.add(new JScrollPane(this.initTaskPanel()), cc.xy(2, 2, CellConstraints.FILL, CellConstraints.TOP));

    JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 10, 10));
    JButton importBtn = new JButton(this.textProvider.getString("playlistimport.addToPlaylist"));
    importBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        handler.addTracksToPlaylist();
        dispose();
      }

    });

    JCheckBox hideResolveCb = BasicComponentFactory.createCheckBox(this.hideResolved, "gefundene Titel ausblenden");
    panel.add(hideResolveCb, cc.xy(2, 4));

    JButton cancelBtn = new JButton(new DisposeAction(this, this.textProvider.getString("cancel")));
    buttonPanel.add(importBtn);
    buttonPanel.add(new JButton(this.repeatSearchAction));
    buttonPanel.add(cancelBtn);

    panel.add(buttonPanel, cc.xy(2, 6, CellConstraints.CENTER, CellConstraints.CENTER));

    this.setTitle(this.textProvider.getString("playlistimport.title"));
    this.setSize(600, 630);
    SwingTools.centerOnScreen(this);

  }

  public void startTitleResolve() {
    setSearchInProgress(true);
    Thread thread = new Thread() {

      @Override
      public void run() {
        try {
          handler.resolveTitlesRemote();
        } catch (final Exception e) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              JXErrorPane.showDialog(e);
            }
          });
        } finally {
          setSearchInProgress(false);
        }
      }

    };
    thread.start();
  }

  private void setSearchInProgress(boolean progress) {
    this.searchInProgress = progress;
    SwingUtilities.invokeLater(new Runnable() {

      @Override
      public void run() {
        repeatSearchAction.setEnabled(!searchInProgress);
      }
    });
  }

  private JPanel initTaskPanel() {
    StringBuilder rowSpec = new StringBuilder();
    int[] rowGroup = new int[handler.getTasks().size()];
    for (int i = 0; i < handler.getTasks().size(); i++) {
      rowSpec.append("pref,");
      rowGroup[i] = i + 1;
    }

    FormLayout layout = new FormLayout("pref:grow", rowSpec.toString());
    // layout.setRowGroups(new int[][] { rowGroup });
    JPanel panel = new JPanel(layout);

    CellConstraints cc = new CellConstraints();
    panel.setBackground(Color.WHITE);
    panel.setOpaque(false);
    int row = 1;
    final List<TitleImportTaskPanel> taskPanels = new ArrayList<TitleImportTaskPanel>();
    for (TrackImportTask task : handler.getTasks()) {
      TitleImportTaskPanel taskPanel = new TitleImportTaskPanel(task);
      panel.add(taskPanel, cc.xy(1, row, CellConstraints.FILL, CellConstraints.FILL));
      taskPanels.add(taskPanel);
      row++;
    }

    this.hideResolved.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        boolean value = (Boolean) evt.getNewValue();
        for (TitleImportTaskPanel panel : taskPanels) {
          if (panel.getTask().getStatus() == Status.RESOLVED) {
            panel.setVisible(!value);
          }
        }
      }
    });

    return panel;
  }

  private class TitleImportTaskPanel extends JPanel implements PropertyChangeListener {
    private static final long serialVersionUID = 8487490352138839679L;
    private TrackImportTask task;
    private JLabel statusLabel = new JLabel();
    private JComponent resolved = null;
    private boolean markedForUpload = false;
    private JPopupMenu actionPopup;

    public TitleImportTaskPanel(TrackImportTask task) {
      super();
      this.task = task;
      this.task.addPropertyChangeListener("status", this);
      this.setBackground(Color.WHITE);
      this.setOpaque(true);
      this.init();
    }

    private void init() {
      this.setLayout(new FormLayout("5dlu,pref:grow,5dlu,50px,5dlu,40dlu,5dlu", "3dlu,pref,3dlu,pref,5dlu"));
      this.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
      CellConstraints cc = new CellConstraints();

      JLabel filenameLabel = new JLabel(this.task.getSourceString());
      this.add(filenameLabel, cc.xy(2, 2));

      this.add(this.statusLabel, cc.xywh(6, 2, 1, 3, CellConstraints.CENTER, CellConstraints.CENTER));
      this.updateStatus();

      Dimension dim = this.getPreferredSize();
      this.setPreferredSize(new Dimension((int) dim.getWidth(), Math.max(55, (int) dim.getHeight())));

      this.setVisible(this.task.getStatus() != Status.RESOLVED
          || ((Boolean) hideResolved.getValue()).booleanValue() == false);

      JPanel actionPanel = new JPanel(new FormLayout("5dlu,pref,5dlu,pref,5dlu", "3dlu,pref,3dlu"));
      MenuLabel searchBtn = new MenuLabel(textProvider.getString("playlistimport.action.search"));
      searchBtn.setActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          showSearchPanel();
        }

      });
      actionPanel.add(searchBtn, new CellConstraints(2, 2));
      final MenuLabel uploadBtn = new MenuLabel(textProvider.getString("playlistimport.action.upload"));
      uploadBtn.setEnabled(false); // FIXME task instanceof MP3TitleImportTask && uploadAvailable);
      uploadBtn.setActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          UploadWindow win = ctx.getUploadWindowManager().getUploadWindow();
          if (!win.isVisible()) {
            win.setVisible(true);
          }
          if (task instanceof MP3TrackImportTask) {
            win.addFiles(new File[] { ((MP3TrackImportTask) task).getFile() }, false);
          }
          markedForUpload = true;
          actionPopup.setVisible(false);
          activeActionPopup = null;
          updateStatus();
          uploadBtn.setEnabled(false);
        }
      });

      actionPanel.add(uploadBtn, new CellConstraints(4, 2));
      actionPanel.setBackground(Color.WHITE);
      this.actionPopup = new JPopupMenu();
      this.actionPopup.add(actionPanel);
      this.actionPopup.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, Color.LIGHT_GRAY));

      this.addMouseListener(new MouseAdapter() {

        /**
         * @see java.awt.event.MouseAdapter#mouseEntered(java.awt.event.MouseEvent)
         */
        @Override
        public void mouseEntered(MouseEvent e) {
          if (activeActionPopup != null) {
            actionPopup.setVisible(false);
          }
          if (task.getStatus() == Status.NO_CANDIDATES || task.getStatus() == Status.NO_TAGS) {
            actionPopup.show(TitleImportTaskPanel.this, 0, TitleImportTaskPanel.this.getHeight() - 1);
            activeActionPopup = actionPopup;
          } else {
            activeActionPopup = null;
          }
        }

      });
    }

    private void showSearchPanel() {
      if (activeActionPopup != null) {
        actionPopup.setVisible(false);
        activeActionPopup = null;
      }
      if (task.getStatus() == Status.NO_CANDIDATES || task.getStatus() == Status.NO_TAGS) {
        final JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createLoweredBevelBorder());

        final SearchPanel panel = new SearchPanel(ctx, false);
        panel.getModel().getModel("artist").setValue(task.getArtist());
        panel.getModel().getModel("title").setValue(task.getTitle());

        JPanel buttonBar = new JPanel(new GridLayout(-1, 2, 10, 10));
        JButton okBtn = new JButton(textProvider.getString("ok"));
        okBtn.addActionListener(new ActionListener() {
          @Override
          @SuppressWarnings("unchecked")
          public void actionPerformed(ActionEvent e) {
            List<BasicTrack> titles = (List<BasicTrack>) panel.getSelectionHolder().getValue();
            if (titles != null && titles.size() > 0) {
              task.setTrackLibraryTitle(titles.get(0));
              task.setStatus(Status.RESOLVED);
              // if title is already in local pool (with different artist name)
              // then register an alias
              client.getTrackService().getTrackRegistry().registerAlias(titles.get(0).getId(), task.getArtist(), task.getTitle());
              try {
                client.getTrackService().saveAliases();
              } catch (IOException ex) {
              }
            }
            menu.setVisible(false);
          }

        });
        buttonBar.add(okBtn);
        JButton cancelBtn = new JButton(textProvider.getString("cancel"));
        cancelBtn.addActionListener(new ActionListener() {

          @Override
          public void actionPerformed(ActionEvent e) {
            menu.setVisible(false);
          }
        });
        buttonBar.add(cancelBtn);

        Component comp = TitleImportTaskPanel.this;
        JPanel menuPanel = new JPanel(new BorderLayout());
        menuPanel.add(panel, BorderLayout.CENTER);
        menuPanel.add(buttonBar, BorderLayout.SOUTH);
        menuPanel.setPreferredSize(new Dimension(comp.getWidth() - 2, 250));

        menu.add(menuPanel);
        menu.show(TitleImportTaskPanel.this, 0, comp.getHeight());
      }

    }

    private void updateStatus() {
      JLabel label = null;

      switch (task.getStatus()) {
        case RESOLVED :
          this.statusLabel.setIcon(ctx.getIcon("ok.png"));
          this.statusLabel.setToolTipText(textProvider.getString("playlistimport.status.ok"));
          label = new JLabel(task.getTrackLibraryTitle().toString());
          label.setFont(ComponentFactory.italicLabelFont);
          this.setResolved(label);
          this.setToolTipText(null);
          break;
        case SEARCHING :
          this.statusLabel.setIcon(ctx.getIcon("searching.png"));
          this.statusLabel.setToolTipText(textProvider.getString("playlistimport.status.searching"));
          break;
        case NO_CANDIDATES :
          label = new JLabel(textProvider.getString("playlistimport.status.no_candidates"));
          label.setForeground(Color.RED);
          this.setResolved(label);
          if (this.markedForUpload) {
            this.statusLabel.setIcon(ctx.getIcon("upload.png"));
            this.statusLabel.setToolTipText(textProvider.getString("playlistimport.status.upload"));
          } else {
            this.statusLabel.setIcon(ctx.getIcon("failed.png"));
            this.statusLabel.setToolTipText(textProvider.getString("playlistimport.status.failed"));
          }
          break;
        case NO_TAGS :
          label = new JLabel(textProvider.getString("playlistimport.status.no_tags"));
          label.setForeground(Color.RED);
          this.setResolved(label);
          this.statusLabel.setIcon(ctx.getIcon("failed.png"));
          this.statusLabel.setToolTipText(textProvider.getString("playlistimport.status.failed"));
          break;
        case TAG_READ_ERROR :
          label = new JLabel(textProvider.getString("playlistimport.status.tag_read_error"));
          label.setForeground(Color.RED);
          this.setResolved(label);
          this.statusLabel.setIcon(ctx.getIcon("failed.png"));
          this.statusLabel.setToolTipText(textProvider.getString("playlistimport.status.failed"));
          break;
        case MULTIPLE_CANDIDATES :
          final JComboBox cmb = new JComboBox(task.getCandidates().toArray());
          cmb.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 6952180511893986432L;

            /**
             * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList,
             *      java.lang.Object, int, boolean, boolean)
             */
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
              Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
              if (value instanceof DetailedTrack) {
                DetailedTrack title = (DetailedTrack) value;
                this.setText(title.getArtist() + " - " + title.getTitle() + " (" + title.getAlbum() + ")");
              } else if (value instanceof BasicTrack) {
                this.setText(((BasicTrack) value).toString());
              }
              return comp;
            }

          });
          cmb.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
              BasicTrack title = (BasicTrack) cmb.getSelectedItem();
              task.setTrackLibraryTitle(title);
            }

          });
          this.setResolved(cmb);
          break;
      }
      TrackImportDlg.this.validate();
      TrackImportDlg.this.repaint();
    }

    private void setResolved(JComponent comp) {
      if (this.resolved != null) {
        this.remove(this.resolved);
      }
      this.resolved = comp;
      this.add(this.resolved, new CellConstraints(2, 4));
    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      updateStatus();
    }

    /**
     * @return the task
     */
    public TrackImportTask getTask() {
      return task;
    }
  }

  private class RepeatSearchAction extends AbstractAction {
    private static final long serialVersionUID = -1237609278979042829L;

    RepeatSearchAction() {
      this.putValue(Action.NAME, "Suche wiederholen");
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      for (TrackImportTask task : handler.getTasks()) {
        if (task.getStatus() == Status.NO_CANDIDATES) {
          task.setStatus(Status.OPEN);
        }
      }
      startTitleResolve();
    }

  }

}
