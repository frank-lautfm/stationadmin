package de.stationadmin.gui.playlist.scheduleditems;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.scheduled.ScheduledItem;
import de.stationadmin.base.playlist.scheduled.TrackSelectionMode;
import de.stationadmin.base.playlist.scheduled.TrackType;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.TrackComparator;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.EnumListCellRenderer;

public class ScheduledItemEditor extends JPanel {
  private static final long serialVersionUID = 6018314382865318033L;
  private TextProvider textProvider;
  private TagManager tagManager;
  private TrackRegistry trackRegistry;
  private PresentationModel<ScheduledItem> model;

  public ScheduledItemEditor(TagManager tagManager, TrackRegistry trackRegistry, TextProvider textProvider, PresentationModel<ScheduledItem> model) {
    this.textProvider = textProvider;
    this.trackRegistry = trackRegistry;
    this.tagManager = tagManager;
    this.model = model;
    this.initialize();
  }

  @SuppressWarnings("unchecked")
  private void initialize() {
    this.setLayout(new FormLayout("5dlu,pref,5dlu,200dlu:grow,5dlu", "5dlu,pref,5dlu,pref,5dlu,pref,5dlu,pref,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    int row = 2;

    // Name
    {
      JTextField tf = BasicComponentFactory.createTextField(model.getBufferedModel("name"));
      tf.setColumns(20);
      this.add(new JLabel(textProvider.getString("scheduleditem.name")), cc.xy(2, row));
      this.add(tf, cc.xy(4, row));
      row += 2;
    }

    // Type
    {
      SelectionInList<TrackType> typeSelection = new SelectionInList<>(TrackType.values(), model.getBufferedModel("trackType"));
      JComboBox<TrackType> cmb = BasicComponentFactory.createComboBox(typeSelection, new EnumListCellRenderer(textProvider, "scheduleditem.tracktype"));
      this.add(new JLabel(textProvider.getString("scheduleditem.tracktype")), cc.xy(2, row));
      this.add(cmb, cc.xy(4, row));
      row += 2;
    }

    // Tag
    {
      List<String> tagNames = new ArrayList<>();
      tagManager.getStaticTags().forEach(t -> tagNames.add(t.getName()));
      Collections.sort(tagNames);

      SelectionInList<String> tagSelection = new SelectionInList<>(tagNames, model.getBufferedModel("tag"));
      JComboBox<String> cmb = BasicComponentFactory.createComboBox(tagSelection);
      this.add(new JLabel(textProvider.getString("scheduleditem.tag")), cc.xy(2, row));
      this.add(cmb, cc.xy(4, row));
      row += 2;
    }

    // IntroJingle
    {
      ArrayList<BasicTrack> jingles = new ArrayList<>();
      for (BasicTrack track : trackRegistry.getAllTracks()) {
        if (track.getType() == BasicTrack.TYPE_JINGLE) {
          jingles.add(track);
        }
      }
      Collections.sort(jingles, new TrackComparator());
      ArrayList<Integer> jingleIds = new ArrayList<>();
      jingleIds.add(-1);
      jingles.forEach(t -> jingleIds.add(t.getId()));

      SelectionInList<Integer> jingleSelection = new SelectionInList<>(jingleIds, model.getBufferedModel("introJingleId"));
      JComboBox<Integer> cmb = BasicComponentFactory.createComboBox(jingleSelection, new JingleRenderer());
      this.add(new JLabel(textProvider.getString("scheduleditem.introJingle")), cc.xy(2, row));
      this.add(cmb, cc.xy(4, row));
      row += 2;

    }

    // Selection
    {
      SelectionInList<TrackSelectionMode> modeSelection = new SelectionInList<>(TrackSelectionMode.values(), model.getBufferedModel("selection"));
      JComboBox<TrackType> cmb = BasicComponentFactory.createComboBox(modeSelection, new EnumListCellRenderer(textProvider, "scheduleditem.selection"));
      this.add(new JLabel(textProvider.getString("scheduleditem.selection")), cc.xy(2, row));
      this.add(cmb, cc.xy(4, row));
      row += 2;
    }

  }

  private class JingleRenderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = -7293913840112084061L;

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (value instanceof Integer) {
        int id = (Integer) value;
        if (id == -1) {
          setText("kein Jingle");
        } else {
          try {
            DetailedTrack track = trackRegistry.getTrack(id);
            if (track != null) {
              setText(track.toString());
            }
          } catch (Exception e) {

          }
        }
      }
      return this;
    }

  }

}
