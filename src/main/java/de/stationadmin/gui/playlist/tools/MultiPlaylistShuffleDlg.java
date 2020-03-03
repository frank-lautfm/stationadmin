/**
 * 
 */
package de.stationadmin.gui.playlist.tools;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.IndirectListModel;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.shuffle.PlaylistGenerator;
import de.stationadmin.base.playlist.shuffle.PlaylistShuffler;
import de.stationadmin.base.util.PlaylistGeneratorFactory;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.playlist.SimplePlaylistListCellRender;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.ThreadedAction;

/**
 * 
 * @author Frank Korf
 * 
 */
public class MultiPlaylistShuffleDlg extends StationAdminFrame {
  private static final long serialVersionUID = 7485702692678090322L;
  private TextProvider textProvider;
  private ValueModel hoursModel = new ValueHolder(24);
  private ValueModel generate = new ValueHolder(false);
  private IndirectListModel<Playlist> playlistModel = new IndirectListModel<Playlist>();
  private JList playlistList;

  private ValueModel artistPenaltyEnabled = new ValueHolder(true);
  private ValueModel titlePenaltyEnabled = new ValueHolder(true);
  private ValueModel titlePenaltyStrictEnabled = new ValueHolder(false);

  public MultiPlaylistShuffleDlg(ClientContext ctx) throws HeadlessException {
    super(ctx, "MultiPlaylistShuffleDlg");
    this.textProvider = ctx.getTextProvider();
    this.init();
  }

  private void init() {
    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,pref:grow,5dlu,pref,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    {
      JPanel headPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
      List<Integer> values = new ArrayList<Integer>();
      for (int i = 1; i < 36; i++) {
        values.add(Integer.valueOf(i));
      }
      values.add(Integer.valueOf(48));
      values.add(Integer.valueOf(72));
      values.add(Integer.valueOf(96));
      values.add(Integer.valueOf(120));
      values.add(Integer.valueOf(144));
      values.add(Integer.valueOf(168));

      SelectionInList<Integer> hours = new SelectionInList<Integer>(values, this.hoursModel);
      JComboBox hourCmb = BasicComponentFactory.createComboBox(hours);

      headPanel.add(new JLabel(textProvider.getString("multishuffle.timespan")));
      headPanel.add(hourCmb);
      headPanel.add(new JLabel(textProvider.getString("multishuffle.hour")));

      JRadioButton shuffle = BasicComponentFactory.createRadioButton(this.generate, Boolean.FALSE, textProvider.getString("multishuffle.shuffle"));
      JRadioButton generate = BasicComponentFactory.createRadioButton(this.generate, Boolean.TRUE, textProvider.getString("multishuffle.generate"));
      headPanel.add(shuffle);
      headPanel.add(generate);

      this.getContentPane().add(headPanel, cc.xy(2, 2));

      PropertyChangeListener changeListener = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          try {
            updatePlaylists();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }

      };

      this.hoursModel.addValueChangeListener(changeListener);
      this.generate.addValueChangeListener(changeListener);
    }

    {
      this.playlistList = new JList(this.playlistModel);
      this.playlistList.setCellRenderer(new SimplePlaylistListCellRender());
      this.getContentPane().add(new JScrollPane(this.playlistList), cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));
      this.updatePlaylists();
    }

    {
      JPanel genOptionsPanel = new JPanel(new GridLayout(3, 1));
      JLabel label = new JLabel(ctx.getTextProvider().getString("multishuffle.generateoptions.label"));
      genOptionsPanel.add(label);
      final JCheckBox artistCb = BasicComponentFactory.createCheckBox(this.artistPenaltyEnabled,
          ctx.getTextProvider().getString("multishuffle.generateoptions.artistPenalty"));
      genOptionsPanel.add(artistCb);
      final JCheckBox titleCb = BasicComponentFactory.createCheckBox(this.titlePenaltyEnabled,
          ctx.getTextProvider().getString("multishuffle.generateoptions.titlePenalty"));
      final JCheckBox titleStrictCb = BasicComponentFactory.createCheckBox(this.titlePenaltyStrictEnabled,
          ctx.getTextProvider().getString("multishuffle.generateoptions.titlePenalty.strict"));

      JPanel titlePenaltyPanel = new JPanel(new FormLayout("pref,3dlu,pref", "pref"));
      titlePenaltyPanel.add(titleCb, new CellConstraints(1, 1));
      titlePenaltyPanel.add(titleStrictCb, new CellConstraints(3, 1));
      genOptionsPanel.add(titlePenaltyPanel);

      artistCb.setEnabled((Boolean) this.generate.getValue());
      titleCb.setEnabled((Boolean) this.generate.getValue());
      generate.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          artistCb.setEnabled((Boolean) evt.getNewValue());
          titleCb.setEnabled((Boolean) evt.getNewValue());
          titleStrictCb.setEnabled((Boolean) evt.getNewValue());
        }
      });

      this.getContentPane().add(genOptionsPanel, cc.xy(2, 6, CellConstraints.FILL, CellConstraints.FILL));
    }

    {
      JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
      buttonPanel.add(new JButton(new ShuffleAction(this.ctx)));
      buttonPanel.add(new JButton(new DisposeAction(this, "Abbruch")));
      this.getContentPane().add(buttonPanel, cc.xy(2, 8, CellConstraints.CENTER, CellConstraints.CENTER));
    }

    this.setTitle(textProvider.getString("multishuffle.title"));
  }
  
  @Override
  protected Dimension getDefaultSize() {
    return new Dimension(400, 400);
  }

  private void updatePlaylists() {
    Integer hours = (Integer) this.hoursModel.getValue();
    List<Playlist> allPlaylists = this.ctx.getAdminClient().getSchedule().getPlaylistsAfter(new Date(), hours);

    ArrayList<Playlist> playlists = new ArrayList<Playlist>();
    boolean generate = (Boolean) this.generate.getValue();
    for (Playlist playlist : allPlaylists) {
      if (playlist.getType() == PlaylistType.ONLINE && (!generate && playlist.isLocalShuffleAllowed()) || (generate && playlist.isGenerate())) {
        playlists.add(playlist);
      }
    }

    int[] selection = new int[playlists.size()];
    for (int i = 0; i < playlists.size(); i++) {
      selection[i] = i;
    }
    this.playlistModel.setList(playlists);
    this.playlistList.setSelectedIndices(selection);
  }

  class ShuffleAction extends ThreadedAction {
    private static final long serialVersionUID = -662763631116085196L;
    private String status;
    private ClientContext ctx;

    ShuffleAction(ClientContext ctx) {
      super(MultiPlaylistShuffleDlg.this.getRootPane());
      this.ctx = ctx;
      this.putValue(Action.NAME, "Start");
    }

    /**
     * @see de.stationadmin.gui.util.ThreadedAction#getStatus()
     */
    @Override
    protected String getStatus() {
      return this.status;
    }

    /**
     * @see de.stationadmin.gui.util.ThreadedAction#performAction()
     */
    @Override
    protected void performAction() throws Exception {
      Object[] values = playlistList.getSelectedValues();
      boolean generate = (Boolean) MultiPlaylistShuffleDlg.this.generate.getValue();
      if (generate) {
        PlaylistGenerator generator = PlaylistGeneratorFactory.createGenerator(ctx.getAdminClient());
        generator.setArtistPenaltyEnabled((Boolean) artistPenaltyEnabled.getValue());
        generator.setTrackPenaltyEnabled((Boolean) titlePenaltyEnabled.getValue());
        if (generator.isTrackPenaltyEnabled() && (Boolean) titlePenaltyStrictEnabled.getValue()) {
          generator.setTrackPenaltyMax(600);
          generator.setTrackPenaltyPeriod(0);
        }
        for (Object obj : values) {
          Playlist playlist = (Playlist) obj;
          status = textProvider.getString("multishuffle.playlist.generate", playlist.getDisplayName());
          generator.generate(playlist);
          ctx.getAdminClient().getPlaylistService().savePlaylist(playlist);
        }

      } else {
        PlaylistShuffler shuffler = PlaylistGeneratorFactory.createShuffler(ctx.getAdminClient());
        for (Object obj : values) {
          Playlist playlist = (Playlist) obj;
          status = textProvider.getString("multishuffle.playlist", playlist.getDisplayName());
          shuffler.shuffle(playlist);
          ctx.getAdminClient().getPlaylistService().savePlaylist(playlist);
        }
      }
    }

    /**
     * @see de.stationadmin.gui.util.ThreadedAction#showError(java.lang.Exception)
     */
    @Override
    protected void showError(Exception e) {
      ErrorInfo info = textProvider.createErrorInfo(e, "multishuffle.error");
      JXErrorPane.showDialog(AppUtils.getRootFrame(), info);
    }

    /**
     * @see de.stationadmin.gui.util.ThreadedAction#onSuccess()
     */
    @Override
    protected void onSuccess() {
      dispose();
    }

  }

}
