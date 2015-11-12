/**
 * 
 */
package de.stationadmin.gui.playlist.config;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author korf
 * 
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class TagSequenceEditor extends JPanel {
  private static final long serialVersionUID = 2693849493703383326L;
  private ValueModel tagHolder;
  private JPopupMenu tagPopup;
  private JList tagList;
  private TagLabel currentLabel;
  private boolean multiValue = true;

  public TagSequenceEditor(String[] tags, ValueModel tagHolder, boolean multiValue) {
    this.tagHolder = tagHolder;
    this.multiValue = multiValue;
    this.initPopup(tags);
    this.setOpaque(true);
    this.setBackground(Color.WHITE);
    this.setLayout(new FlowLayout(FlowLayout.LEFT));

    rebuildTagsUI();
    tagHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        rebuildTagsUI();
      }
    });

  }

  private void rebuildTagsUI() {
    this.removeAll();
    String[] value = (String[]) tagHolder.getValue();
    if (value != null) {
      for (String tag : value) {
        this.addTag(tag);
      }
    }
    if (multiValue || this.getComponentCount() == 0) {
      this.addTag(" ");
    }
    this.validate();
    this.repaint();
  }

  private void rebuildTagsModel() {
    ArrayList<String> tags = new ArrayList<String>();
    for (Component comp : this.getComponents()) {
      if (comp instanceof TagLabel) {
        String tag = ((TagLabel) comp).getText();
        if (StringUtils.trimToNull(tag) != null) {
          tags.add(tag);
        }
      }
    }
    this.tagHolder.setValue(tags.toArray(new String[tags.size()]));
  }

  private void addTag(String tag) {
    final TagLabel label = new TagLabel();
    label.setText(tag);
    this.add(label);
    label.addMouseListener(new MouseAdapter() {

      @Override
      public void mouseClicked(MouseEvent e) {
        currentLabel = label;
        tagList.setSelectedValue(label.getText(), true);
        tagPopup.show(label, 0, label.getHeight());
      }
    });
  }

  private void initPopup(String[] tags) {
    DefaultListModel tagListModel = new DefaultListModel();
    tagListModel.addElement(" ");
    for (String tag : tags) {
      tagListModel.addElement(tag);
    }
    this.tagList = new JList(tagListModel);

    JPanel tagListPanel = new JPanel(new FormLayout("2dlu,80dlu,2dlu", "2dlu,min(pref;100dlu),2dlu"));
    tagListPanel.add(new JScrollPane(tagList), new CellConstraints(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    this.tagPopup = new JPopupMenu();
    this.tagPopup.add(tagListPanel);

    this.tagList.addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          tagSelected((String) tagList.getSelectedValue());
          tagPopup.setVisible(false);
        }
      }
    });
  }

  private void tagSelected(String tag) {
    if (currentLabel != null) {
      boolean changed = !StringUtils.equals(this.currentLabel.getText(), tag);
      boolean wasEmpty = StringUtils.isEmpty(this.currentLabel.getText());
      this.currentLabel.setText(tag);
      if (changed) {
        if (wasEmpty && multiValue) {
          this.addTag(" ");
        }
        if (StringUtils.trimToNull(tag) == null) {
          this.remove(this.currentLabel);
          this.currentLabel = null;
        }
        validate();
        repaint();
        rebuildTagsModel();
      }
    }
  }

  private static class TagLabel extends JLabel {
    private static final long serialVersionUID = 5967983241310889275L;

    TagLabel() {
      this.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 10));
      this.setOpaque(true);
      this.setBackground(new Color(230, 230, 230));
    }
  }
}
