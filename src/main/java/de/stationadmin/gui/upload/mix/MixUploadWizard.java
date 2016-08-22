/**
 * 
 */
package de.stationadmin.gui.upload.mix;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.mp3splitter.MP3Splitter;
import de.stationadmin.base.mp3splitter.SplitPoint;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistNameCompator;
import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.playlist.SimplePlaylistListCellRender;
import de.stationadmin.gui.upload.UploadWindow;
import de.stationadmin.gui.util.MP3Launcher;
import de.stationadmin.gui.util.SwingTools;

/**
 * @author Frank
 * 
 */
public class MixUploadWizard extends JFrame {
  private static final long serialVersionUID = -3384061974642095236L;
  private ClientContext ctx;
  private StationAdminClient client;
  private TextProvider textProvider;
  private UploadWindow uploadWindow;
  private MP3Launcher mp3Launcher;
  private PlaylistRegistry playlistRegistry;
  private ValueHolder playlistHolder = new ValueHolder();
  private ValueHolder pathHolder = new ValueHolder();
  private ValueHolder fileListHolder = new ValueHolder();
  private ValueHolder tagHolder = new ValueHolder();
  private SplitPointTableModel splitPointModel;

  private ValueModel lastDirHolder = new ValueHolder();

  /**
   * @param textProvider
   * @param uploadWindow
   */
  public MixUploadWizard(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.textProvider = ctx.getTextProvider();
    this.uploadWindow = ctx.getUploadWindowManager().getUploadWindow();
    this.mp3Launcher = new MP3Launcher(ctx);
    this.client = ctx.getAdminClient();
    this.playlistRegistry = client.getPlaylistService().getPlaylistRegistry();
    if (ctx.getAdminClient() != null && ctx.getAdminClient().getSettings().getMp3Root() != null) {
      this.lastDirHolder.setValue(new File(ctx.getAdminClient().getSettings().getMp3Root()));
    }
    this.init();
  }

  private void init() {
    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,pref,5dlu,pref:grow,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    this.getContentPane().add(this.createFilePanel(), cc.xy(2, 2));

    SplitPointPanel splitPointPanel = new SplitPointPanel(ctx);
    splitPointPanel.setLastDirHolder(this.lastDirHolder);
    this.splitPointModel = splitPointPanel.getTableModel();
    this.getContentPane().add(splitPointPanel, cc.xy(2, 6, CellConstraints.FILL, CellConstraints.FILL));

    this.getContentPane().add(this.createButtonPanel(), cc.xy(2, 8));

    this.setSize(600, 600);
    this.setTitle(this.textProvider.getString("upload.mix.title"));
    SwingTools.centerOnScreen(this);
  }

  @SuppressWarnings("rawtypes")
  private JPanel createFilePanel() {
    JPanel panel = new JPanel(new FormLayout("pref,5dlu,pref:grow,5dlu,pref", "pref,5dlu,pref,5dlu,pref"));
    CellConstraints cc = new CellConstraints();

    {
      panel.add(new JLabel(this.textProvider.getString("upload.mix.path") + ":"), cc.xy(1, 1));
      panel.add(BasicComponentFactory.createTextField(this.pathHolder), cc.xy(3, 1));
      panel.add(new JButton(new SelectAction()), cc.xy(5, 1));
    }

    {

      panel.add(new JLabel(this.textProvider.getString("upload.mix.playlist") + ":"), cc.xy(1, 3));

      List<Playlist> all = new ArrayList<Playlist>(this.playlistRegistry.getAllPlaylists());
      Collections.sort(all, new PlaylistNameCompator());
      all.add(0, null);
      SelectionInList<Playlist> playlistSelection = new SelectionInList<Playlist>(all, this.playlistHolder);
      JComboBox playlistCmb = BasicComponentFactory.createComboBox(playlistSelection, new SimplePlaylistListCellRender());
      panel.add(playlistCmb, cc.xy(3, 3, CellConstraints.LEFT, CellConstraints.CENTER));
    }

    if (this.ctx.getAdminClient().getTagManager().getStaticTags().size() > 0) {
      panel.add(new JLabel(this.textProvider.getString("upload.mix.tag") + ":"), cc.xy(1, 5));
      List<StaticTag> tags = new ArrayList<StaticTag>(this.client.getTagManager().getStaticTags());
      Collections.sort(tags);
      tags.add(0, null);
      SelectionInList<StaticTag> tagSelection = new SelectionInList<StaticTag>(tags, this.tagHolder);
      JComboBox tagCmb = BasicComponentFactory.createComboBox(tagSelection);
      panel.add(tagCmb, cc.xy(3, 5, CellConstraints.LEFT, CellConstraints.CENTER));

    }

    return panel;
  }

  private JPanel createButtonPanel() {
    JPanel panel = new JPanel(new GridLayout(1, 3, 10, 10));
    panel.add(new JButton(new SplitAction()));
    panel.add(new JButton(new PrelistenAction()));
    panel.add(new JButton(new UploadAction()));
    return panel;
  }

  private class SplitAction extends AbstractAction {
    private static final long serialVersionUID = -4269765138958022182L;

    SplitAction() {
      super(textProvider.getString("upload.mix.action.split"));
      this.setEnabled(false);
      pathHolder.addPropertyChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent arg0) {
          String filename = (String) pathHolder.getValue();
          setEnabled(filename != null && new File(filename).exists());
        }
      });

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {

      MP3Splitter splitter = new MP3Splitter();
      String path = (String) pathHolder.getValue();
      File file = new File(path);
      List<SplitPoint> splitPoints = splitPointModel.getSplitPoints();

      try {
        List<File> files = splitter.split(file, splitPoints, file.getParentFile());
        fileListHolder.setValue(files);
        JOptionPane.showMessageDialog(MixUploadWizard.this,
            textProvider.getString("upload.mix.action.split.success", Integer.toString(files.size())), null, JOptionPane.INFORMATION_MESSAGE);
      } catch (Exception e) {
        ErrorInfo errorInfo = textProvider.createErrorInfo(e, "upload.mix.action.split.failed");
        JXErrorPane.showDialog(MixUploadWizard.this, errorInfo);
      }

    }

  }

  private class PrelistenAction extends AbstractAction {
    private static final long serialVersionUID = 5539557162522446934L;

    PrelistenAction() {
      super(textProvider.getString("upload.mix.action.prelisten"));
      this.setEnabled(false);

      fileListHolder.addPropertyChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent arg0) {
          setEnabled(fileListHolder.getValue() != null);
        }
      });

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent evt) {
      List<File> files = (List<File>) fileListHolder.getValue();
      if (files != null && files.size() > 0) {
        String[] paths = new String[files.size()];
        for (int i = 0; i < files.size(); i++) {
          paths[i] = files.get(i).getAbsolutePath();
        }
        mp3Launcher.play(paths);
      }
    }

  }

  private class UploadAction extends AbstractAction {
    private static final long serialVersionUID = 1860806735638848853L;

    UploadAction() {
      super(textProvider.getString("upload.mix.action.upload"));
      this.setEnabled(false);
      fileListHolder.addPropertyChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent arg0) {
          setEnabled(fileListHolder.getValue() != null);
        }
      });
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent evt) {
      List<File> files = (List<File>) fileListHolder.getValue();
      if (files != null) {
        Playlist playlist = (Playlist) playlistHolder.getValue();
        String tag = tagHolder.getValue() != null ? ((StaticTag) tagHolder.getValue()).getName() : null;
        uploadWindow.addFiles(files.toArray(new File[files.size()]), true);
        
        MixTrackUploadWatcher watcher = new MixTrackUploadWatcher(uploadWindow.getUploadManager(), ctx.getAdminClient().getTagManager(), files, playlist, tag);
        watcher.startWatching();
        
        MixUploadWizard.this.dispose();
      }
    }

  }

  private class SelectAction extends AbstractAction {
    private static final long serialVersionUID = -6883125317326735068L;

    SelectAction() {
      super("...");
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      JFileChooser chooser = new JFileChooser();
      if (lastDirHolder.getValue() != null) {
        chooser.setCurrentDirectory((File)lastDirHolder.getValue());
      }
      chooser.setDialogTitle("DJ Mix öffnen"); // FIXME
      chooser.setFileFilter(new FileNameExtensionFilter("MP3", "mp3"));
      if (chooser.showOpenDialog(MixUploadWizard.this) == JFileChooser.APPROVE_OPTION) {
        pathHolder.setValue(chooser.getSelectedFile().getAbsolutePath());
        lastDirHolder.setValue(chooser.getSelectedFile().getParentFile());
      }
    }
  }

}
