/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.tag.TagManager;
import de.stationadmin.gui.TextProvider;

/**
 * @author Frank
 *
 */
public class MultiTagSelector extends JDialog {
  private static final long serialVersionUID = -4875855928853887635L;
  private TextProvider textProvider;
  private TagManager tagManager;
  private ValueModel tagHolder;

  private DefaultListModel listModel;
  private JList list;

  /**
   * @param textProvider
   * @param tagManager
   * @param tagHolder
   */
  public MultiTagSelector(TextProvider textProvider, TagManager tagManager, ValueModel tagHolder) {
    super();
    this.textProvider = textProvider;
    this.tagManager = tagManager;
    this.tagHolder = tagHolder;

    // close and unregister if single tag is selected
    final PropertyChangeListener tagChangeListener = new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (!(evt.getNewValue() instanceof List)) {
          MultiTagSelector.this.tagHolder.removeValueChangeListener(this);
          dispose();
        }
      }
    };
    this.tagHolder.addValueChangeListener(tagChangeListener);

    this.listModel = new DefaultListModel();
    List<String> tags = this.tagManager.getTags();
    Collections.sort(tags);
    this.listModel.addElement(RegisteredTracksTableModel.USED_TITLES);
    this.listModel.addElement(RegisteredTracksTableModel.UNUSED_TITLES);
    for (String tag : tags) {
      this.listModel.addElement(tag);
    }

    list = new JList(this.listModel);
    list.setCellRenderer(new TagSelectionListCellRenderer(textProvider));
    list.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(new JScrollPane(list), BorderLayout.CENTER);

    JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
    btnPanel.add(new JButton(new AcceptAction()));
    this.getContentPane().add(btnPanel, BorderLayout.SOUTH);
    this.setSize(200, 300);
    this.setTitle(textProvider.getString("titlelist.customdlg.title"));

  }

  private class AcceptAction extends AbstractAction {
    private static final long serialVersionUID = 940241119823742494L;

    AcceptAction() {
      super(textProvider.getString("titlelist.customdlg.accept"));
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      Object[] values = list.getSelectedValues();
      List<String> tags = new ArrayList<String>();
      if (values != null) {
        for (Object v : values) {
          tags.add((String) v);
        }
      }
      tagHolder.setValue(tags);
    }

  }

}
