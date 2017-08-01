/**
 * 
 */
package de.stationadmin.gui.tasks.editor;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.schedule.PlaylistExchangeTask;
import de.stationadmin.base.schedule.ScheduleShuffleTask;
import de.stationadmin.base.schedule.ScheduleShuffler;
import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.tasks.ScheduledTaskEditorComponent;

/**
 * Editor panel for {@link PlaylistExchangeTask}
 * 
 * @author korf
 * 
 */
public class ScheduleShuffleTaskPanel extends JPanel implements ScheduledTaskEditorComponent {
  private static final long serialVersionUID = 1319217057035869783L;
  private TextProvider textProvider;
  private PlaylistRegistry playlistRegistry;

  private JTextField taskNameTf;
  private JComboBox<String> playlistTagCmb;
  private JCheckBox slotLengthRequiredCb;

  public ScheduleShuffleTaskPanel(PlaylistRegistry playlistRegistry, TextProvider textProvider) {
    super();
    this.textProvider = textProvider;
    this.playlistRegistry = playlistRegistry;
    this.init();
  }

  @SuppressWarnings("rawtypes")
  private void init() {
    this.setLayout(new FormLayout("pref,5dlu,pref:grow", "pref,5dlu,pref,5dlu,pref"));
    CellConstraints cc = new CellConstraints();

    this.taskNameTf = new JTextField(15);
    this.add(new JLabel(this.textProvider.getString("task.name")), cc.xy(1, 1));
    this.add(taskNameTf, cc.xy(3, 1));

    this.add(new JLabel(this.textProvider.getString("schedule.action.shuffle.property.playlists")), cc.xy(1, 3));

    ArrayList<String> entries = new ArrayList<String>(playlistRegistry.getUsedTags());
    Collections.sort(entries);
    entries.add(0, null);
    entries.add(ScheduleShuffler.TAG_USED);

    this.playlistTagCmb = new JComboBox<String>(entries.toArray(new String[entries.size()]));
    playlistTagCmb.setRenderer(new DefaultListCellRenderer() {
      private static final long serialVersionUID = -7901905125762119676L;

      /**
       * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
       */
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value == null) {
          setText(textProvider.getString("playlistselector.all"));
        } else if (value.equals(ScheduleShuffler.TAG_USED)) {
          setText(textProvider.getString("playlistselector.used"));
        }
        return comp;
      }

    });
    this.add(this.playlistTagCmb, cc.xy(3, 3));

    this.slotLengthRequiredCb = new JCheckBox(textProvider.getString("schedule.action.shuffle.property.slotLengthRequired"));
    this.add(this.slotLengthRequiredCb, cc.xywh(1, 5, 3, 1));

  }

  @Override
  public void updateView(ScheduledTask task) {
    if (task.getTask() instanceof ScheduleShuffleTask) {
      ScheduleShuffleTask shuffleTask = (ScheduleShuffleTask) task.getTask();
      this.playlistTagCmb.setSelectedItem(StringUtils.trimToEmpty(shuffleTask.getPlaylistTag()));
      this.slotLengthRequiredCb.setSelected(shuffleTask.isSlotLenghForPlaylistsRequired());
      this.taskNameTf.setText(shuffleTask.getName());
    }

  }

  @Override
  public void updateTask(ScheduledTask task) {
    if (task.getTask() instanceof ScheduleShuffleTask) {
      ScheduleShuffleTask shuffleTask = (ScheduleShuffleTask) task.getTask();
      shuffleTask.setPlaylistTag((String) playlistTagCmb.getSelectedItem());
      shuffleTask.setSlotLenghForPlaylistsRequired(slotLengthRequiredCb.isSelected());
      shuffleTask.setName(StringUtils.trimToEmpty(taskNameTf.getText()));
    }

  }
}
