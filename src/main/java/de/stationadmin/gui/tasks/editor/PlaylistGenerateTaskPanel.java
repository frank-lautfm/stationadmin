/**
 * 
 */
package de.stationadmin.gui.tasks.editor;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistGenerateTask;
import de.stationadmin.base.playlist.PlaylistNameCompator;
import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.tasks.ScheduledTaskEditorComponent;

/**
 * Editor panel for {@link PlaylistGenerateTask}
 * 
 * @author korf
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class PlaylistGenerateTaskPanel extends JPanel implements ScheduledTaskEditorComponent {
  private static final long serialVersionUID = 1319217057035869783L;
  private PlaylistRegistry playlistRegistry;
  private TextProvider textProvider;

  private ValueHolder singlePlaylist = new ValueHolder(Boolean.TRUE);
  private JSpinner hourSpinner;
  private JComboBox playlistCmb;
  private JLabel playlistNameLabel, hourLabel;

  private JTextField taskNameTf;
  private JCheckBox artistCb, titleCb, titleStrictCb, restartCb, synchronizeCb;
  private JPanel genOptionsPanel;

  public PlaylistGenerateTaskPanel(PlaylistRegistry playlistRegistry, TextProvider textProvider) {
    super();
    this.playlistRegistry = playlistRegistry;
    this.textProvider = textProvider;
    this.init();
  }

  private void init() {
    this.setLayout(new FormLayout("max(30dlu;pref),5dlu,pref:grow", "pref,5dlu,pref,3dlu,pref,5dlu,pref,pref,5dlu,pref,5dlu,pref,5dlu,pref"));
    CellConstraints cc = new CellConstraints();
    
    this.taskNameTf = new JTextField(15);
    this.add(new JLabel(this.textProvider.getString("task.name")), cc.xy(1, 1));
    this.add(taskNameTf, cc.xy(3, 1));

    JRadioButton btnSingle = BasicComponentFactory.createRadioButton(this.singlePlaylist, Boolean.TRUE,
        this.textProvider.getString("task.shuffle.editor.type.single"));
    JRadioButton btnMulti = BasicComponentFactory.createRadioButton(this.singlePlaylist, Boolean.FALSE,
        this.textProvider.getString("task.shuffle.editor.type.multi"));
    singlePlaylist.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        updateVisbility();
      }
    });

    this.add(new JLabel(this.textProvider.getString("task.generate.editor.property.type")), cc.xy(1, 3));
    this.add(btnSingle, cc.xy(3, 3));
    this.add(btnMulti, cc.xy(3, 5));

    this.playlistNameLabel = new JLabel(this.textProvider.getString("task.shuffle.editor.property.playlist"));
    List<Playlist> playlists = this.playlistRegistry.getPlaylists(PlaylistType.ONLINE);
    Collections.sort(playlists, new PlaylistNameCompator());
    this.playlistCmb = new JComboBox(playlists.toArray());
    this.add(this.playlistNameLabel, cc.xy(1, 7));
    this.add(this.playlistCmb, cc.xy(3, 7));

    this.hourLabel = new JLabel(this.textProvider.getString("task.shuffle.editor.property.hours"));
    SpinnerNumberModel hourModel = new SpinnerNumberModel(6, 1, 168, 1);
    this.hourSpinner = new JSpinner(hourModel);
    this.add(this.hourLabel, cc.xy(1, 8));
    this.add(this.hourSpinner, cc.xy(3, 8, CellConstraints.LEFT, CellConstraints.CENTER));
    this.hourLabel.setVisible(false);
    this.hourSpinner.setVisible(false);

    genOptionsPanel = new JPanel(new GridLayout(3, 1));
    JLabel label = new JLabel(this.textProvider.getString("multishuffle.generateoptions.label"));
    genOptionsPanel.add(label);
    artistCb = new JCheckBox(this.textProvider.getString("multishuffle.generateoptions.artistPenalty"));
    genOptionsPanel.add(artistCb);
    titleCb = new JCheckBox(this.textProvider.getString("multishuffle.generateoptions.titlePenalty"));
    titleStrictCb = new JCheckBox(this.textProvider.getString("multishuffle.generateoptions.titlePenalty.strict"));

    JPanel titlePenaltyPanel = new JPanel(new FormLayout("pref,3dlu,pref", "pref"));
    titlePenaltyPanel.add(titleCb, new CellConstraints(1, 1));
    titlePenaltyPanel.add(titleStrictCb, new CellConstraints(3, 1));
    genOptionsPanel.add(titlePenaltyPanel);
    this.genOptionsPanel.setVisible(false);

    this.add(this.genOptionsPanel, cc.xywh(1, 10, 3, 1, CellConstraints.LEFT, CellConstraints.CENTER));

    this.synchronizeCb = new JCheckBox(this.textProvider.getString("task.shuffle.editor.property.synchronize"));
    this.add(synchronizeCb, cc.xywh(1, 12, 3, 1, CellConstraints.LEFT, CellConstraints.CENTER));

    this.restartCb = new JCheckBox(this.textProvider.getString("task.shuffle.editor.property.restart"));
    this.add(restartCb, cc.xywh(1, 14, 3, 1, CellConstraints.LEFT, CellConstraints.CENTER));

  }

  private void updateVisbility() {
    boolean single = this.singlePlaylist.getValue().equals(Boolean.TRUE);
    this.hourLabel.setVisible(!single);
    this.hourSpinner.setVisible(!single);
    this.genOptionsPanel.setVisible(!single);
    this.playlistCmb.setVisible(single);
    this.playlistNameLabel.setVisible(single);
    this.validate();
    this.repaint();
  }

  @Override
  public void updateView(ScheduledTask task) {
    if (task.getTask() instanceof PlaylistGenerateTask) {
      PlaylistGenerateTask generateTask = (PlaylistGenerateTask) task.getTask();
      this.taskNameTf.setText(generateTask.getName());
      if (generateTask.getPlaylistName() != null) {
        singlePlaylist.setValue(true);
        for (Playlist pl : this.playlistRegistry.getPlaylists(PlaylistType.ONLINE)) {
          if (pl.getName().equals(generateTask.getPlaylistName())) {
            playlistCmb.setSelectedItem(pl);
          }
        }
      } else {
        singlePlaylist.setValue(false);
        this.hourSpinner.setValue(generateTask.getHours());
      }
      this.artistCb.setSelected(generateTask.isArtistPenaltyEnabled());
      this.titleCb.setSelected(generateTask.isTitlePenaltyEnabled());
      this.titleStrictCb.setSelected(generateTask.isTitlePenaltyStrictEnabled());
      this.restartCb.setSelected(generateTask.isRestartStation());
      this.synchronizeCb.setSelected(generateTask.isSynchronize());
      this.updateVisbility();
    }

  }

  @Override
  public void updateTask(ScheduledTask task) {
    if (task.getTask() instanceof PlaylistGenerateTask) {
      PlaylistGenerateTask generateTask = (PlaylistGenerateTask) task.getTask();
      generateTask.setName(StringUtils.trimToNull(this.taskNameTf.getText()));
      if (singlePlaylist.getValue().equals(Boolean.TRUE)) {
        generateTask.setPlaylistName(((Playlist) playlistCmb.getSelectedItem()).getName());
        generateTask.setHours(0);
      } else {
        generateTask.setPlaylistName(null);
        generateTask.setHours((Integer) this.hourSpinner.getValue());
      }
      generateTask.setArtistPenaltyEnabled(this.artistCb.isSelected());
      generateTask.setTitlePenaltyEnabled(this.titleCb.isSelected());
      generateTask.setTitlePenaltyStrictEnabled(this.titleStrictCb.isSelected());
      generateTask.setRestartStation(this.restartCb.isSelected());
      generateTask.setSynchronize(this.synchronizeCb.isSelected());
    }

  }
}
