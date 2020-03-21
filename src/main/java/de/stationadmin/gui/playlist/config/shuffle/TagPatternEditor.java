package de.stationadmin.gui.playlist.config.shuffle;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.jdesktop.swingx.JXList;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class TagPatternEditor extends JPanel {
  private static final long serialVersionUID = -4288748303926186908L;
  private List<String> tags;
  private ValueModel tagPattern;
  private DefaultListModel<String> tagPatternListModel;
  private boolean isUpdating = false;

  public TagPatternEditor(List<String> tags, ValueModel tagPattern) {
    this.tags = tags;
    this.tagPattern = tagPattern;
    this.init();
  }

  private void init() {

    this.setLayout(new FormLayout("150dlu,5dlu,pref,5dlu,150dlu", "200dlu"));
    CellConstraints cc = new CellConstraints();

    DefaultListModel<String> sourceModel = new DefaultListModel<>();
    for (String tag : tags) {
      sourceModel.addElement(tag);
    }

    tagPatternListModel = new DefaultListModel<>();
    rebuildListModel();

    JXList tagsList = new JXList(sourceModel);
    tagsList.setTransferHandler(new TagPatternTransferHandler(tagsList, true));
    tagsList.setDragEnabled(true);
    this.add(new JScrollPane(tagsList), cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));

    JXList tagPatternList = new JXList(tagPatternListModel);
    tagPatternList.setTransferHandler(new TagPatternTransferHandler(tagPatternList, false));
    tagPatternList.setDragEnabled(true);
    tagPatternList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
    tagPatternList.getActionMap().put("delete", new DeleteAction(tagPatternList));
    
    this.add(new JScrollPane(tagPatternList), cc.xy(5, 1, CellConstraints.FILL, CellConstraints.FILL));

    tagPatternListModel.addListDataListener(new ListDataListener() {

      @Override
      public void intervalRemoved(ListDataEvent e) {
        rebuildTagPattern();
      }

      @Override
      public void intervalAdded(ListDataEvent e) {
        rebuildTagPattern();
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
        rebuildTagPattern();
      }
    });

  }

  private void rebuildTagPattern() {
    if (!isUpdating) {
      try {
        isUpdating = true;
        String[] tags = new String[tagPatternListModel.getSize()];
        for (int i = 0; i < tags.length; i++) {
          tags[i] = tagPatternListModel.get(i);
        }
        tagPattern.setValue(tags);
      } finally {
        isUpdating = false;
      }
    }
  }

  private void rebuildListModel() {
    if (!isUpdating) {
      try {
        isUpdating = true;
        tagPatternListModel.clear();
        if (tagPattern.getValue() != null) {
          for (String tag : (String[]) tagPattern.getValue()) {
            tagPatternListModel.addElement(tag);
          }
        }
      } finally {
        isUpdating = false;
      }
    }
  }
  
  private class DeleteAction extends AbstractAction {
    private static final long serialVersionUID = -4795810683654044027L;
    private JXList list;
    
    public DeleteAction(JXList list) {
      super();
      this.list = list;
    }


      

    @Override
    public void actionPerformed(ActionEvent e) {
      if(list.getSelectedIndices().length > 0) {
        ArrayList<Integer> rows = new ArrayList<>();
        for(int row : list.getSelectedIndices()) {
          rows.add(list.convertIndexToModel(row));
        }
        rows.sort((r1, r2) -> -Integer.compare(r1, r2));
        rows.forEach(r -> tagPatternListModel.remove(r));
      }
      
    }
    
  }

}
