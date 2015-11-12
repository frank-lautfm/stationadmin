/**
 * 
 */
package de.stationadmin.gui.tasks.editor;

import java.awt.Component;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistNameCompator;
import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.schedule.PlaylistExchangeTask;
import de.stationadmin.base.schedule.Schedule.Weekday;
import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.tasks.ScheduledTaskEditorComponent;

/**
 * Editor panel for {@link PlaylistExchangeTask}
 * 
 * @author korf
 * 
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class PlaylistExchangeTaskPanel extends JPanel implements ScheduledTaskEditorComponent {
  private static final long serialVersionUID = 1319217057035869783L;
  private PlaylistRegistry playlistRegistry;
  private TextProvider textProvider;

  private JTextField taskNameTf;
  private JSpinner hourSpinner;
  private JComboBox playlistCmb;
  private JComboBox weekdayCmb;

  public PlaylistExchangeTaskPanel(PlaylistRegistry playlistRegistry, TextProvider textProvider) {
    super();
    this.playlistRegistry = playlistRegistry;
    this.textProvider = textProvider;
    this.init();
  }

  private void init() {
    this.setLayout(new FormLayout("max(30dlu;pref),5dlu,pref:grow", "pref,5dlu,pref,5dlu,pref,5dlu,pref"));
    CellConstraints cc = new CellConstraints();

    this.taskNameTf = new JTextField(15);
    this.add(new JLabel(this.textProvider.getString("task.name")), cc.xy(1, 1));
    this.add(taskNameTf, cc.xy(3, 1));

    this.weekdayCmb = new JComboBox(Weekday.values());
    this.weekdayCmb.setRenderer(new DefaultListCellRenderer() {
      private static final long serialVersionUID = 1339482205078145709L;

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Weekday) {
          setText(textProvider.getString("weekday." + ((Weekday) value).name().toLowerCase()));
        }
        return c;
      }

    });
    JLabel weekdayLabel = new JLabel(this.textProvider.getString("task.schedule.playlist.editor.property.weekday"));
    this.add(weekdayLabel, cc.xy(1, 3));
    this.add(this.weekdayCmb, cc.xy(3, 3));

    SpinnerNumberModel hourModel = new SpinnerNumberModel(0, 0, 23, 1);
    this.hourSpinner = new JSpinner(hourModel);
    this.add(new JLabel(this.textProvider.getString("task.schedule.playlist.editor.property.hours")), cc.xy(1, 5));
    this.add(this.hourSpinner, cc.xy(3, 5, CellConstraints.LEFT, CellConstraints.CENTER));

    JLabel playlistNameLabel = new JLabel(this.textProvider.getString("task.schedule.playlist.editor.property.playlist"));
    List<Playlist> playlists = this.playlistRegistry.getPlaylists(PlaylistType.ONLINE);
    Collections.sort(playlists, new PlaylistNameCompator());
    this.playlistCmb = new JComboBox(playlists.toArray());
    this.add(playlistNameLabel, cc.xy(1, 7));
    this.add(this.playlistCmb, cc.xy(3, 7));

  }

  @Override
  public void updateView(ScheduledTask task) {
    if (task.getTask() instanceof PlaylistExchangeTask) {
      PlaylistExchangeTask exchangeTask = (PlaylistExchangeTask) task.getTask();
      this.taskNameTf.setText(exchangeTask.getName());
      this.hourSpinner.setValue(exchangeTask.getHour());
      this.weekdayCmb.setSelectedItem(exchangeTask.getWeekday());
      for (Playlist pl : this.playlistRegistry.getPlaylists(PlaylistType.ONLINE)) {
        if (pl.getName().equals(exchangeTask.getPlaylistName())) {
          playlistCmb.setSelectedItem(pl);
        }
      }
    }

  }

  @Override
  public void updateTask(ScheduledTask task) {
    if (task.getTask() instanceof PlaylistExchangeTask) {
      PlaylistExchangeTask exchangeTask = (PlaylistExchangeTask) task.getTask();
      exchangeTask.setName(StringUtils.trimToNull(this.taskNameTf.getText()));
      exchangeTask.setHour((Integer) this.hourSpinner.getValue());
      exchangeTask.setWeekday((Weekday) this.weekdayCmb.getSelectedItem());
      exchangeTask.setPlaylistName(((Playlist) this.playlistCmb.getSelectedItem()).getName());
    }

  }
}
