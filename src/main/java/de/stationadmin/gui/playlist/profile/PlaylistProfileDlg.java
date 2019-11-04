package de.stationadmin.gui.playlist.profile;

import java.awt.BorderLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import com.jgoodies.binding.list.IndirectListModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.PlaylistService;
import de.stationadmin.base.playlist.profile.PlaylistProfile;
import de.stationadmin.base.playlist.shuffle.PlaylistProfileType;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;
import de.stationadmin.gui.util.SwingTools;

public class PlaylistProfileDlg extends StationAdminFrame {
  private static final long serialVersionUID = 6316924409434987515L;

  private ValueHolder selection = new ValueHolder();

  private PlaylistProfileModel profileModel;

  private JPanel container = new JPanel(new BorderLayout());
  private JTabbedPane tabbedPane = new JTabbedPane();

  public PlaylistProfileDlg(ClientContext ctx) throws HeadlessException {
    super(ctx, "playlistprofiles");
    selection.setIdentityCheckEnabled(true);
    this.profileModel = new PlaylistProfileModel(selection);
    init();
  }

  private void init() {
    this.getContentPane().setLayout(new FormLayout("5dlu,120dlu,5dlu,pref:grow,5dlu", "5dlu,pref:grow,5dlu"));
    this.setTitle(ctx.getString("playlistprofilemanager.title"));

    initSelection();
    initMainPanel();

    this.setSize(800, 600);
    SwingTools.centerWithin(ctx.getRootWindow(), this);
  }

  @SuppressWarnings("unchecked")
  private void initSelection() {
    final IndirectListModel<PlaylistProfile> model = new IndirectListModel<PlaylistProfile>(ctx.getAdminClient().getPlaylistService().getProfiles());
    final JList<PlaylistProfile> list = new JList<PlaylistProfile>(model);

    list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          selection.setValue(list.getSelectedValue());
        }
      }

    });
    selection.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (list.getSelectedValue() != evt.getNewValue()) {
          list.setSelectedValue(evt.getNewValue(), true);
        }
      }
    });

    this.ctx.getAdminClient().getPlaylistService().addPropertyChangeListener("profiles", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        model.fireContentsChanged(0, ctx.getAdminClient().getPlaylistService().getProfiles().size());
        list.getSelectionModel().clearSelection();
      }

    });

    this.getContentPane().add(new JScrollPane(list), new CellConstraints(2, 2, CellConstraints.FILL, CellConstraints.FILL));

  }

  private void initMainPanel() {
    JPanel mainPanel = new JPanel(new BorderLayout());

    {
      JToolBar toolbar = new JToolBar();

      final JPopupMenu newPopup = new JPopupMenu();
      newPopup.add(new AddAction(PlaylistProfileType.StationAdminShuffle));
      newPopup.add(new AddAction(PlaylistProfileType.LocalShuffle));
      newPopup.add(new AddAction(PlaylistProfileType.Generate));

      JButton newBtn = new JButton(ctx.getIcon("filenew.png"));
      newBtn.setToolTipText(ctx.getString("playlistprofilemanager.action.new.tooltip"));
      newBtn.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent evt) {
          JButton src = (JButton) evt.getSource();
          newPopup.show(src, 0, src.getHeight());
        }
      });

      toolbar.add(newBtn);
      toolbar.addSeparator();

      SaveAction save = new SaveAction();
      selection.addValueChangeListener(save);
      toolbar.add(save);

      DeleteAction delete = new DeleteAction();
      selection.addValueChangeListener(delete);
      toolbar.add(delete);

      mainPanel.add(toolbar, BorderLayout.NORTH);
    }

    {
      tabbedPane = new JTabbedPane();
      tabbedPane.addTab("Allgemein", new ProfileGeneralPanel(ctx.getTextProvider(), profileModel));
      tabbedPane.addTab("Werbetrigger", new ProfileAdTriggerPanel(ctx, profileModel));
      tabbedPane.addTab("Gebundene Jingles", new ProfileTrackRulePanel(ctx, profileModel));
      tabbedPane.addTab("Normalisierung", new ArtistNormalizationPanel(ctx, profileModel));

      selection.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {

          PlaylistProfile profile = (PlaylistProfile) evt.getNewValue();
          if (profile != null) {
            if (container.getComponentCount() == 0) {
              container.add(tabbedPane);
              validate();
              repaint();
            }

            if (profile.getType() == PlaylistProfileType.Generate) {
              if (tabbedPane.getTabCount() < 6) {
                tabbedPane.addTab("Gewichtung", new JPanel());
                tabbedPane.addTab("Vorauswahl", new JPanel());
              }
            } else if (tabbedPane.getTabCount() > 4) {
              tabbedPane.removeTabAt(tabbedPane.getTabCount() - 1);
              tabbedPane.removeTabAt(tabbedPane.getTabCount() - 1);
            }
          } else {
            container.removeAll();
            validate();
            repaint();
          }

        }
      });

      mainPanel.add(container, BorderLayout.CENTER);

      this.add(mainPanel, new CellConstraints(4, 2, CellConstraints.FILL, CellConstraints.FILL));
    }

  }

  private class SaveAction extends AbstractAction implements PropertyChangeListener {
    private static final long serialVersionUID = -1055278847691917936L;

    SaveAction() {
      this.putValue(Action.SMALL_ICON, ctx.getIcon("save.png"));
      this.putValue(Action.SHORT_DESCRIPTION, ctx.getString("playlistprofilemanager.action.save.tooltip"));
      this.setEnabled(false);
    }

    public void actionPerformed(ActionEvent evt) {
      try {
        profileModel.triggerCommit();
        ctx.getAdminClient().getPlaylistService().saveProfiles();
      } catch (IOException e) {
        ErrorInfo errorInfo = ctx.createErrorInfo(e, "playlistprofilemanager.action.save.failed");
        JXErrorPane.showDialog(PlaylistProfileDlg.this, errorInfo);
      }

    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      this.setEnabled(evt.getNewValue() != null);
    }
  }

  private class DeleteAction extends AbstractAction implements PropertyChangeListener {
    private static final long serialVersionUID = 6615868256397913660L;
    private PlaylistProfile profile;

    DeleteAction() {
      this.putValue(Action.SMALL_ICON, ctx.getIcon("delete.png"));
      this.putValue(Action.SHORT_DESCRIPTION, ctx.getString("playlistprofilemanager.action.delete.tooltip"));
      this.setEnabled(false);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      if (JOptionPane.showConfirmDialog(PlaylistProfileDlg.this, ctx.getString("playlistprofilemanager.action.delete.confirm", profile.getName()), "",
          JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        try {
          ctx.getAdminClient().getPlaylistService().removeProfile(profile.getId());
          ctx.getAdminClient().getPlaylistService().saveProfiles();
        } catch (IOException e) {
          ErrorInfo errorInfo = ctx.createErrorInfo(e, "playlistprofilemanager.action.delete.failed");
          JXErrorPane.showDialog(PlaylistProfileDlg.this, errorInfo);
        }
      }
    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      this.profile = (PlaylistProfile) evt.getNewValue();
      this.setEnabled(this.profile != null);
    }
  }

  private class AddAction extends AbstractAction {
    private static final long serialVersionUID = -1971327228376903559L;
    private PlaylistProfileType type;

    AddAction(PlaylistProfileType type) {
      this.putValue(Action.NAME, ctx.getString("playlistprofilemanager.action.add." + type.name().toLowerCase()));
      this.type = type;
      this.setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {

      PlaylistProfile profile = new PlaylistProfile();
      profile.setType(type);
      profile.setName(ctx.getString("playlistprofilemanager.defaultname." + type.name().toLowerCase()));

      PlaylistService service = ctx.getAdminClient().getPlaylistService();
      try {
        service.addProfile(profile);
        service.saveProfiles();
        selection.setValue(profile);
      } catch (Throwable e) {
        service.removeProfile(profile.getId());
        ErrorInfo errorInfo = ctx.createErrorInfo(e, "playlistprofilemanager.action.save.failed");
        JXErrorPane.showDialog(PlaylistProfileDlg.this, errorInfo);
      }

    }
  }

}
