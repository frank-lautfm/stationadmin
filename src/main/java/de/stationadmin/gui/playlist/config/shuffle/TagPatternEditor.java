package de.stationadmin.gui.playlist.config.shuffle;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.playlist.PopupListener;
import de.stationadmin.gui.util.ClipboardAction;

public class TagPatternEditor extends JPanel {
  private static final long serialVersionUID = -4288748303926186908L;
  private List<String> tags;
  private ValueModel tagPattern;
  private TextProvider teaxtProvider;
  private DefaultListModel<String> tagPatternListModel;
  private boolean isUpdating = false;
  private HashSet<String> validTags = new HashSet<>();

  public TagPatternEditor(TextProvider teaxtProvider, List<String> tags, ValueModel tagPattern) {
    this.teaxtProvider = teaxtProvider;
    this.tags = tags;
    this.tagPattern = tagPattern;
    this.init();
  }

  private void init() {

    this.setLayout(new FormLayout("150dlu,5dlu,pref,5dlu,150dlu", "pref,2dlu,200dlu,5dlu,pref"));
    CellConstraints cc = new CellConstraints();

    DefaultListModel<String> sourceModel = new DefaultListModel<>();
    for (String tag : tags) {
      sourceModel.addElement(tag);
      this.validTags.add(tag);
    }

    tagPatternListModel = new DefaultListModel<>();
    rebuildListModel();

    {
      final ValueHolder sourceSelection = new ValueHolder();
      JXList tagsList = new JXList(sourceModel);
      tagsList.setTransferHandler(new TagPatternTransferHandler(tagsList, true));
      tagsList.setDragEnabled(true);
      tagsList.addListSelectionListener(new SelectionListener(tagsList, sourceSelection));

      final JPopupMenu sourcePopup = new JPopupMenu();
      sourcePopup.add(new ClipboardAction(this.teaxtProvider, tagsList, sourceSelection, TransferHandler.getCopyAction()));
      tagsList.addMouseListener(new PopupListener(tagsList, sourcePopup));

      this.add(new JLabel(teaxtProvider.getString("tagpattern.available")), cc.xy(1, 1));
      this.add(new JScrollPane(tagsList), cc.xy(1, 3, CellConstraints.FILL, CellConstraints.FILL));
    }

    {
      final ValueHolder targetSelection = new ValueHolder();

      final JXList tagPatternList = new JXList(tagPatternListModel);
      tagPatternList.setTransferHandler(new TagPatternTransferHandler(tagPatternList, false));
      tagPatternList.setDragEnabled(true);
      tagPatternList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
      tagPatternList.getActionMap().put("delete", new DeleteAction(tagPatternList));
      tagPatternList.addListSelectionListener(new SelectionListener(tagPatternList, targetSelection));

      final JPopupMenu targetPopup = new JPopupMenu();
      targetPopup.add(new ClipboardAction(this.teaxtProvider, tagPatternList, targetSelection, TransferHandler.getCutAction()));
      targetPopup.add(new ClipboardAction(this.teaxtProvider, tagPatternList, targetSelection, TransferHandler.getCopyAction()));
      targetPopup.add(new ClipboardAction(this.teaxtProvider, tagPatternList, targetSelection, TransferHandler.getPasteAction()));
      targetPopup.addSeparator();
      targetPopup.add(new DeleteAction(tagPatternList));
      tagPatternList.addMouseListener(new PopupListener(tagPatternList, targetPopup));

      tagPatternList.addHighlighter(new AbstractHighlighter() {

        @Override
        protected Component doHighlight(Component comp, ComponentAdapter adapter) {
          int row = tagPatternList.convertIndexToModel(adapter.row);
          String tag = (String) tagPatternList.getModel().getElementAt(row);
          if (!validTags.contains(tag)) {
            comp.setForeground(new Color(255, 0, 0));
          }
          return comp;
        }
      });

      this.add(new JLabel(teaxtProvider.getString("tagpattern.pattern")), cc.xy(5, 1));
      this.add(new JScrollPane(tagPatternList), cc.xy(5, 3, CellConstraints.FILL, CellConstraints.FILL));

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
    
    this.add(new JLabel(teaxtProvider.getString("tagpattern.instruction")), cc.xywh(1, 5, 5, 1, CellConstraints.FILL, CellConstraints.FILL));

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
      this.putValue(Action.NAME, teaxtProvider.getString(("delete")));
      this.list = list;
      this.setEnabled(false);
      list.addListSelectionListener(new ListSelectionListener() {

        @Override
        public void valueChanged(ListSelectionEvent e) {
          setEnabled(list.getSelectedIndex() > -1);
        }
      });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (list.getSelectedIndices().length > 0) {
        ArrayList<Integer> rows = new ArrayList<>();
        for (int row : list.getSelectedIndices()) {
          rows.add(list.convertIndexToModel(row));
        }
        rows.sort((r1, r2) -> -Integer.compare(r1, r2));
        rows.forEach(r -> tagPatternListModel.remove(r));
      }

    }

  }

  private class SelectionListener implements ListSelectionListener {
    private JXList source;
    private ValueHolder selectionHolder;

    public SelectionListener(JXList source, ValueHolder selectionHolder) {
      super();
      this.source = source;
      this.selectionHolder = selectionHolder;
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
      if (!e.getValueIsAdjusting()) {
        int[] rows = source.getSelectedIndices();
        List<String> entries = new ArrayList<String>();
        for (int i = 0; i < rows.length; i++) {
          int row = source.convertIndexToModel(rows[i]);
          entries.add((String) source.getModel().getElementAt(row));
        }
        selectionHolder.setValue(entries);
      }

    }

  }

}
