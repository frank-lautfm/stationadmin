/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.beans.BeanAdapter;
import com.jgoodies.binding.list.IndirectListModel;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistNameCompator;
import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.util.TimeFormat;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.playlist.PlaylistEntryJumpTarget;
import de.stationadmin.gui.playlist.SimplePlaylistListCellRender;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.SwingTools;

/**
 * Displays basic title information and - if available - a list of playlists in which this title occurs
 * 
 * @author korf
 */
public class TrackViewer extends JDialog {
  private static final long serialVersionUID = 6053311643511061488L;
  private ClientContext ctx;
  private BasicTrack title;
  private Set<Integer> playlistIds;
  private PresentationModel<DetailedTrack> model;


  public TrackViewer(ClientContext ctx, BasicTrack title) {
    this(ctx, title, null);

  }

  public TrackViewer(ClientContext ctx, BasicTrack title, Set<Integer> playlistIds) {
    super();
    this.ctx = ctx;
    this.title = title;
    this.playlistIds = playlistIds;
    if (playlistIds == null && title instanceof RegisteredTrack) {
      this.playlistIds = ((RegisteredTrack) title).getPlaylistIds();
    }
    this.init();
  }

  private void init() {
    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref:grow,5dlu,pref,5dlu"));

    JTabbedPane tabPane = new JTabbedPane();
    tabPane.addTab(ctx.getTextProvider().getString("trackviewer.section.info"), title instanceof DetailedTrack ? this.createEditableBasicPanel() : this.createBasicPanel());

    JComponent playlistPanel = this.createPlaylistsPanel();
    if (playlistPanel != null) {
      tabPane.addTab(ctx.getTextProvider().getString("trackviewer.section.playlists"), playlistPanel);
    }

    JComponent tagPanel = this.createTagPanel();
    if (tagPanel != null) {
      tabPane.addTab(ctx.getTextProvider().getString("trackviewer.section.tags"), tagPanel);
    }

    this.getContentPane().add(tabPane, new CellConstraints(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    JButton closeButton = new JButton(ctx.getTextProvider().getString("close"));
    closeButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
      	
      	if(model != null && model.isBuffering()) {
          int response = JOptionPane.showConfirmDialog(TrackViewer.this, ctx.getString("trackviewer.confirm.modifications.message"), ctx.getString("trackviewer.confirm.modifications.title"), JOptionPane.YES_NO_OPTION);
          if(response == JOptionPane.NO_OPTION) return;
      	}
        dispose();
      }

    });

    this.getContentPane().add(closeButton, new CellConstraints(2, 4, CellConstraints.CENTER, CellConstraints.CENTER));

    Dimension prefSize = this.getPreferredSize();
    this.setSize(Math.max(250, (int) prefSize.getWidth() + 30), Math.min((int) prefSize.getHeight() + 50, 500));
    this.setTitle(ctx.getTextProvider().getString("trackviewer.title"));
    SwingTools.centerOnScreen(this);

  }

  private JComponent createBasicPanel() {
    StringBuilder rowSpec = new StringBuilder();
    rowSpec.append("3dlu,");
    rowSpec.append("pref,5dlu,");
    rowSpec.append("pref,5dlu,");
    rowSpec.append("pref,5dlu,");
    rowSpec.append("pref,5dlu,");
    rowSpec.append("pref,5dlu,");
    rowSpec.append("pref,5dlu,");
    rowSpec.append("pref,5dlu,");
    rowSpec.append("pref,");
    rowSpec.append("3dlu,");

    JPanel panel = new JPanel(new FormLayout("3dlu,pref,5dlu,pref,3dlu", rowSpec.toString()));
    CellConstraints cc = new CellConstraints();
    int row = 2;

    panel.add(new JLabel(ctx.getTextProvider().getString("trackviewer.property.artist")), cc.xy(2, row));
    panel.add(new JLabel(title.getArtist()), cc.xy(4, row));
    row += 2;

    panel.add(new JLabel(ctx.getTextProvider().getString("trackviewer.property.title")), cc.xy(2, row));
    panel.add(new JLabel(title.getTitle()), cc.xy(4, row));
    row += 2;

    if (this.title instanceof DetailedTrack) {
      panel.add(new JLabel(ctx.getTextProvider().getString("trackviewer.property.album")), cc.xy(2, row));
      panel.add(new JLabel(((DetailedTrack) title).getAlbum()), cc.xy(4, row));
      row += 2;
    }

    panel.add(new JLabel(ctx.getTextProvider().getString("trackviewer.property.length")), cc.xy(2, row));
    panel.add(new JLabel(TimeFormat.format(title.getLength(), false)), cc.xy(4, row));
    row += 2;

    if (this.title instanceof DetailedTrack) {
      DetailedTrack dtitle = (DetailedTrack) this.title;
      panel.add(new JLabel(ctx.getTextProvider().getString("trackviewer.property.year")), cc.xy(2, row));
      panel.add(new JLabel(Integer.toString(dtitle.getYear())), cc.xy(4, row));
      row += 2;

      panel.add(new JLabel(ctx.getTextProvider().getString("trackviewer.property.genre")), cc.xy(2, row));
      panel.add(new JLabel(dtitle.getGenre()), cc.xy(4, row));
      row += 2;

      SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy");
      panel.add(new JLabel(ctx.getTextProvider().getString("trackviewer.property.upload")), cc.xy(2, row));
      if (dtitle.getUploadDate() != null) {
        panel.add(new JLabel(fmt.format(dtitle.getUploadDate())), cc.xy(4, row));
      }
      row += 2;

      panel.add(new JLabel(ctx.getTextProvider().getString("trackviewer.property.private")), cc.xy(2, row));
      JCheckBox privateTrack = new JCheckBox();
      privateTrack.setSelected(dtitle.isPrivateTrack());
      privateTrack.setEnabled(false);
      panel.add(privateTrack, cc.xy(4, row));
      row += 2;
    }

    return new JScrollPane(panel);
  }

  @SuppressWarnings("rawtypes")
  private JComponent createEditableBasicPanel() {
    StringBuilder rowSpec = new StringBuilder();
    rowSpec.append("pref,5dlu,");
    rowSpec.append("pref,5dlu,");
    rowSpec.append("pref,5dlu,");
    rowSpec.append("pref,5dlu,");
    rowSpec.append("pref,5dlu,");
    rowSpec.append("pref,5dlu,");
    rowSpec.append("pref,5dlu,");
    rowSpec.append("pref,5dlu,");
    rowSpec.append("pref,5dlu,");
    rowSpec.append("pref,");
    rowSpec.append("3dlu,");

    model = new PresentationModel<DetailedTrack>((DetailedTrack) this.title) {
      private static final long serialVersionUID = -8671601899095864425L;

      @Override
      protected BeanAdapter<DetailedTrack> createBeanAdapter(ValueModel beanChannel) {
        return new BeanAdapter<DetailedTrack>(beanChannel, false);
      }

    };
    
    final List<JTextField> textFields = new ArrayList<JTextField>();

    JPanel panel = new JPanel(new FormLayout("3dlu,pref,5dlu,pref,3dlu", rowSpec.toString()));
    CellConstraints cc = new CellConstraints();

    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.add(new SaveAction(model));
    toolbar.add(new ResetAction(model));
    panel.add(toolbar, cc.xywh(1, 1, 4, 1));

    int row = 3;

    ComponentFactory componentFactory = ctx.getComponentFactory();
    JTextField artistTf = componentFactory.createTextField(model.getBufferedModel("artist"), false);
    artistTf.setColumns(20);
    textFields.add(artistTf);
    panel.add(new JLabel(ctx.getTextProvider().getString("trackviewer.property.artist")), cc.xy(2, row));
    panel.add(artistTf, cc.xy(4, row));
    row += 2;

    JTextField titleTf = componentFactory.createTextField(model.getBufferedModel("title"), false);
    titleTf.setColumns(20);
    textFields.add(titleTf);
    panel.add(new JLabel(ctx.getTextProvider().getString("trackviewer.property.title")), cc.xy(2, row));
    panel.add(titleTf, cc.xy(4, row));
    row += 2;

    SelectionInList<Integer> typeSelection = new SelectionInList<Integer>(
        new Integer[] { Integer.valueOf(BasicTrack.TYPE_MUSIC), Integer.valueOf(BasicTrack.TYPE_JINGLE), Integer.valueOf(BasicTrack.TYPE_WORD), Integer.valueOf(BasicTrack.TYPE_NEWS) }, model.getBufferedModel("type"));
    JComboBox typeCmb = BasicComponentFactory.createComboBox(typeSelection, new TrackTypeListCellRenderer(ctx.getTextProvider()));
    typeCmb.setEnabled(model.getBean().isOwnTrack());
    panel.add(new JLabel(ctx.getTextProvider().getString("trackviewer.property.type")), cc.xy(2, row));
    panel.add(typeCmb, cc.xy(4, row));
    row += 2;

    JTextField albumTf = componentFactory.createTextField(model.getBufferedModel("album"), false);
    albumTf.setColumns(20);
    textFields.add(albumTf);
    panel.add(new JLabel(ctx.getTextProvider().getString("trackviewer.property.album")), cc.xy(2, row));
    panel.add(albumTf, cc.xy(4, row));
    row += 2;

    panel.add(new JLabel(ctx.getTextProvider().getString("trackviewer.property.length")), cc.xy(2, row));
    panel.add(new JLabel(TimeFormat.format(title.getLength(), false)), cc.xy(4, row));
    row += 2;

    NumberFormat fmt = NumberFormat.getIntegerInstance();
    fmt.setGroupingUsed(false);
    JTextField yearTf = BasicComponentFactory.createIntegerField(model.getBufferedModel("year"), fmt, 0);
    yearTf.setColumns(4);
    textFields.add(yearTf);
    panel.add(new JLabel(ctx.getTextProvider().getString("trackviewer.property.year")), cc.xy(2, row));
    panel.add(yearTf, cc.xy(4, row));
    row += 2;

    JTextField genreTf = componentFactory.createTextField(model.getBufferedModel("genre"), false);
    genreTf.setColumns(20);
    textFields.add(genreTf);
    panel.add(new JLabel(ctx.getTextProvider().getString("trackviewer.property.genre")), cc.xy(2, row));
    panel.add(genreTf, cc.xy(4, row));
    row += 2;

    panel.add(new JLabel(ctx.getTextProvider().getString("trackviewer.property.upload")), cc.xy(2, row));
    if (((DetailedTrack) title).getUploadDate() != null) {
      SimpleDateFormat dateFmt = new SimpleDateFormat("dd.MM.yyyy");
      panel.add(new JLabel(dateFmt.format(((DetailedTrack) title).getUploadDate())), cc.xy(4, row));
    }
    row += 2;

    panel.add(new JLabel(ctx.getTextProvider().getString("trackviewer.property.private")), cc.xy(2, row));
    final JCheckBox privateTrack = BasicComponentFactory.createCheckBox(model.getBufferedModel("privateTrack"), null);
    privateTrack.setEnabled(model.getBean().isOwnTrack());
    panel.add(privateTrack, cc.xy(4, row));
    row += 2;

    return new JScrollPane(panel);
  }

  @SuppressWarnings("unchecked")
  private JComponent createPlaylistsPanel() {
    if (this.playlistIds != null) {
      JPanel panel = new JPanel(new BorderLayout());

      ArrayList<Playlist> playlists = new ArrayList<Playlist>();
      for (int id : this.playlistIds) {
        Playlist playlist = ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylist(id);
        if (playlist != null) {
          playlists.add(playlist);
        }
      }
      Collections.sort(playlists, new PlaylistNameCompator());

      final JList<Playlist> list = new JList<Playlist>(new IndirectListModel<Playlist>(playlists));
      list.setCellRenderer(new SimplePlaylistListCellRender());
      panel.add(new JScrollPane(list), BorderLayout.CENTER);

      list.addListSelectionListener(new ListSelectionListener() {

        @Override
        public void valueChanged(ListSelectionEvent e) {
          if (!e.getValueIsAdjusting()) {
            Playlist playlist = (Playlist) list.getSelectedValue();
            if (playlist != null) {
              ctx.getJumpHandler().jumpTo(new PlaylistEntryJumpTarget(playlist, title));
            }
          }
        }

      });

      // panel.setBorder(BorderFactory.createTitledBorder(ctx.getTextProvider().getString("trackviewer.section.playlists")));
      return new JScrollPane(panel);
    } else {
      return null;
    }
  }

  private JComponent createTagPanel() {
    final TagManager tagManager = this.ctx.getAdminClient().getTagManager();
    List<String> tags = tagManager.getTags();
    if (tags.size() > 0) {
      JPanel outerPanel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu,", "3dlu,100px:grow,3dlu,"));
      JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));

      ActionListener tagChangeListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          if (e.getSource() instanceof JCheckBox) {
            String tag = (String) ((JCheckBox) e.getSource()).getClientProperty("tag");
            boolean selected = ((JCheckBox) e.getSource()).isSelected();
            if (tag != null) {
              try {
                if (selected) {
                  tagManager.tagTracks(tag, title.getId());
                } else {
                  tagManager.untagTracks(tag, title.getId());
                }
              } catch (IOException ex) {
                ErrorInfo errorInfo = ctx.getTextProvider().createErrorInfo(ex, "trackviewer.tag.error", tag);
                JXErrorPane.showDialog(ctx.getRootWindow(), errorInfo);
              }
            }
          }
        }

      };

      for (String tag : tags) {
        JCheckBox checkBox = new JCheckBox(tag);
        checkBox.putClientProperty("tag", tag);
        checkBox.addActionListener(tagChangeListener);
        checkBox.setEnabled(ctx.getAdminClient().getTagManager().getTag(tag) instanceof StaticTag);
        try {
          checkBox.setSelected(tagManager.isTagged(tag, title.getId()));
        } catch (IOException e) {
          checkBox.setEnabled(false);
        }

        panel.add(checkBox);
      }
      outerPanel.add(new JScrollPane(panel), new CellConstraints(2, 2, CellConstraints.FILL, CellConstraints.TOP));

      return outerPanel;
    } else {
      return null;
    }
  }

  private class SaveAction extends AbstractAction {
    private static final long serialVersionUID = -622220638358501757L;
    private PresentationModel<DetailedTrack> model;

    SaveAction(final PresentationModel<DetailedTrack> model) {
      this.model = model;
      this.putValue(Action.SMALL_ICON, ctx.getIcon("save.png"));
      setEnabled(false);
      model.addPropertyChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          setEnabled(model.isBuffering());
        }
      });

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      try {
        model.triggerCommit();
        DetailedTrack track = model.getBean();
        ctx.getAdminClient().getTrackService().updateTrack(track);
      } catch (Exception e) {
        try {
          ctx.getAdminClient().getTrackService().reloadTrack(model.getBean().getId());
        } catch (Exception ex) {
        }
        JXErrorPane.showDialog(AppUtils.getRootFrame(), ctx.getTextProvider().createErrorInfo(e, "titleviever.update.msg.failed"));

      }

    }
  }

  private class ResetAction extends AbstractAction {
    private static final long serialVersionUID = -1353861266947243104L;
    private PresentationModel<DetailedTrack> model;

    ResetAction(final PresentationModel<DetailedTrack> model) {
      this.model = model;
      this.putValue(Action.SMALL_ICON, ctx.getIcon("undo.png"));
      setEnabled(false);
      model.addPropertyChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          setEnabled(model.isBuffering());
        }
      });

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      model.triggerFlush();
    }
  }

}
