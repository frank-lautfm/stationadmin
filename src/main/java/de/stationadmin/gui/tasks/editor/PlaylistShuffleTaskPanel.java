/**
 * 
 */
package de.stationadmin.gui.tasks.editor;

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
import de.stationadmin.base.playlist.PlaylistNameCompator;
import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.playlist.PlaylistShuffleTask;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.tasks.ScheduledTaskEditorComponent;

/**
 * Editor panel for {@link PlaylistShuffleTask}
 * 
 * @author korf
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class PlaylistShuffleTaskPanel extends JPanel implements ScheduledTaskEditorComponent {
  private static final long serialVersionUID = 1319217057035869783L;
  private PlaylistRegistry playlistRegistry;
  private TextProvider textProvider;

  private JTextField taskNameTf;
  private ValueHolder singlePlaylist = new ValueHolder(Boolean.TRUE);
  private JSpinner hourSpinner;
  private JComboBox playlistCmb;
  private JLabel playlistNameLabel, hourLabel;
  private JCheckBox restartCb;
  private JCheckBox synchronizeCb;
  
  public PlaylistShuffleTaskPanel(PlaylistRegistry playlistRegistry, TextProvider textProvider) {
    super();
    this.playlistRegistry = playlistRegistry;
    this.textProvider = textProvider;
    this.init();
  }

  private void init() {
    this.setLayout(new FormLayout("max(30dlu;pref),5dlu,pref:grow", "pref,5dlu,pref,3dlu,pref,5dlu,pref,pref,5dlu,pref,5dlu,pref"));
    CellConstraints cc = new CellConstraints();

    this.taskNameTf = new JTextField(15);
    this.add(new JLabel(this.textProvider.getString("task.name")), cc.xy(1, 1));
    this.add(taskNameTf, cc.xy(3, 1));

    JRadioButton btnSingle = BasicComponentFactory.createRadioButton(this.singlePlaylist, Boolean.TRUE, this.textProvider.getString("task.shuffle.editor.type.single"));
    JRadioButton btnMulti = BasicComponentFactory.createRadioButton(this.singlePlaylist, Boolean.FALSE, this.textProvider.getString("task.shuffle.editor.type.multi"));
    singlePlaylist.addValueChangeListener( new PropertyChangeListener() {
      
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        updateVisbility();
      }
    });

    this.add(new JLabel(this.textProvider.getString("task.shuffle.editor.property.type")), cc.xy(1, 3));
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

    this.synchronizeCb = new JCheckBox(this.textProvider.getString("task.shuffle.editor.property.synchronize"));
    this.add(synchronizeCb, cc.xywh(1, 10, 3, 1, CellConstraints.LEFT, CellConstraints.CENTER));

    this.restartCb = new JCheckBox(this.textProvider.getString("task.shuffle.editor.property.restart"));
    this.add(restartCb, cc.xywh(1, 12, 3, 1, CellConstraints.LEFT, CellConstraints.CENTER));

  }

  private void updateVisbility() {
    boolean single = this.singlePlaylist.getValue().equals(Boolean.TRUE);
    this.hourLabel.setVisible(!single);
    this.hourSpinner.setVisible(!single);
    this.playlistCmb.setVisible(single);
    this.playlistNameLabel.setVisible(single);
    this.validate();
    this.repaint();
  }

  @Override
  public void updateView(ScheduledTask task) {
    if (task.getTask() instanceof PlaylistShuffleTask) {
      PlaylistShuffleTask shuffleTask = (PlaylistShuffleTask) task.getTask();
      this.taskNameTf.setText(shuffleTask.getName());
      if (shuffleTask.getPlaylistName() != null) {
        singlePlaylist.setValue(true);
        for (Playlist pl : this.playlistRegistry.getPlaylists(PlaylistType.ONLINE)) {
          if (pl.getName().equals(shuffleTask.getPlaylistName())) {
            playlistCmb.setSelectedItem(pl);
          }
        }
      } else {
        singlePlaylist.setValue(false);
        this.hourSpinner.setValue(shuffleTask.getHours());
      }
      this.synchronizeCb.setSelected(shuffleTask.isSynchronize());
      this.restartCb.setSelected(shuffleTask.isRestartStation());
      this.updateVisbility();
    }

  }

  @Override
  public void updateTask(ScheduledTask task) {
    if (task.getTask() instanceof PlaylistShuffleTask) {
      PlaylistShuffleTask shuffleTask = (PlaylistShuffleTask) task.getTask();
      shuffleTask.setName(StringUtils.trimToNull(this.taskNameTf.getText()));
      if (singlePlaylist.getValue().equals(Boolean.TRUE)) {
        shuffleTask.setPlaylistName(((Playlist) playlistCmb.getSelectedItem()).getName());
        shuffleTask.setHours(0);
      } else {
        shuffleTask.setPlaylistName(null);
        shuffleTask.setHours((Integer) this.hourSpinner.getValue());
      }
      shuffleTask.setSynchronize(synchronizeCb.isSelected());
      shuffleTask.setRestartStation(this.restartCb.isSelected());
    }

  }
}
