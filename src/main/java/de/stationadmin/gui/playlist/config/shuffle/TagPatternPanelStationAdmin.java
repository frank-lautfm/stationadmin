package de.stationadmin.gui.playlist.config.shuffle;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.playlist.config.PlaylistConfigurationModel;
import de.stationadmin.gui.playlist.config.generate.TagSequenceEditor;

public class TagPatternPanelStationAdmin extends JPanel {
  private static final long serialVersionUID = -5555289920437431308L;
  private ClientContext ctx;
  private PlaylistConfigurationModel model;

  public TagPatternPanelStationAdmin(ClientContext ctx, PlaylistConfigurationModel model) {
    this.ctx = ctx;
    this.model = model;
    this.init();
  }

  private void init() {
    this.setLayout(new FormLayout("5dlu,pref:grow,5dlu", "8dlu,pref,5dlu,pref,5dlu"));

    CellConstraints cc = new CellConstraints();
    int row = 2;

    final ValueHolder sequence = new ValueHolder();

    sequence.addPropertyChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        String[] tags = (String[]) sequence.getValue();
        if (tags != null && tags.length > 0) {
          getOptions().put("tagPattern", tags);
        } else {
          getOptions().remove("tagPattern");
        }
      }
    });

    updatePatternModel(sequence);

    List<String> tags = new ArrayList<>();
    tags.add("song");
    tags.add("jingle");
    tags.add("moderation");

    List<StaticTag> staticTags = ctx.getAdminClient().getTagManager().getStaticTags();
    Collections.sort(staticTags, (c1, c2) -> c1.getName().compareTo(c2.getName()));
    staticTags.forEach(t -> tags.add(t.getName()));

    TagPatternEditor editor = new TagPatternEditor(ctx.getTextProvider(), tags, sequence);

    this.add(new JLabel(this.ctx.getTextProvider().getString("playlistcfg.property.tagSequenceStationAdmin")), cc.xy(2, row));
    row += 2;
    this.add(editor, cc.xy(2, row));

    model.getBufferedModel("shuffleOpts").addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        updatePatternModel(sequence);
      }
    });

  }

  private void updatePatternModel(ValueHolder sequence) {
    if (getOptions().containsKey("tagPattern")) {
      Object value = getOptions().get("tagPattern");
      if(value instanceof String[]) {
        sequence.setValue(value);
      }
      else if(value instanceof List) {
        List<?> list = (List<?>)value;
        String[] tags = new String[list.size()];
        for(int i = 0; i < list.size(); i++) {
          tags[i] = list.get(i).toString();
        }
        sequence.setValue(tags);
      }
      else {
        sequence.setValue(new String[0]);
      }
      
    } else {
      sequence.setValue(new String[0]);
    }

  }

  @SuppressWarnings("unchecked")
  HashMap<String, Object> getOptions() {
    return (HashMap<String, Object>) model.getBufferedModel("shuffleOpts").getValue();
  }


}
