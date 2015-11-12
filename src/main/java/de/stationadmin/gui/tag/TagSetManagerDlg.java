/**
 * 
 */
package de.stationadmin.gui.tag;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import com.jgoodies.binding.list.IndirectListModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.tag.DynamicTag;
import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.tag.TagSet;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.SwingTools;

/**
 * 
 * @author Frank Korf
 * 
 */
public class TagSetManagerDlg extends JFrame {
  private static final long serialVersionUID = -8023463627796934049L;
  private ClientContext ctx;
  private TagManager tagManager;
  private ValueHolder selection = new ValueHolder();

  private JPanel tagEditorContainer;
  private JPanel currentEditor;
  private TagSetEditor tagSetEditor;

  public TagSetManagerDlg(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.tagManager = ctx.getAdminClient().getTagManager();
    this.init();
  }

  private void init() {
    this.tagSetEditor = new TagSetEditor(ctx);

    this.getContentPane()
        .setLayout(
            new FormLayout("5dlu,120dlu,5dlu,pref:grow,5dlu",
                "5dlu,pref:grow,5dlu"));
    this.setTitle(ctx.getString("titletagsetmanager.title"));

    {
      final IndirectListModel<TagSet> model = new IndirectListModel<TagSet>(
          this.tagManager.getTagSets());

      final JList list = new JList(model);
      list.getSelectionModel().setSelectionMode(
          ListSelectionModel.SINGLE_SELECTION);
      list.addListSelectionListener(new ListSelectionListener() {

        @Override
        public void valueChanged(ListSelectionEvent e) {
          if (!e.getValueIsAdjusting()) {
            selection.setValue(list.getSelectedValue());
          }
        }

      });

      this.tagManager.addPropertyChangeListener("tagSets",
          new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
              model.setList(tagManager.getTagSets());
              list.getSelectionModel().clearSelection();
            }

          });

      this.getContentPane()
          .add(
              new JScrollPane(list),
              new CellConstraints(2, 2, CellConstraints.FILL,
                  CellConstraints.FILL));

    }

    {
      this.tagEditorContainer = new JPanel(new BorderLayout());

      this.selection.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          JPanel newEditor = null;
          if (evt.getNewValue() instanceof TagSet) {
            newEditor = tagSetEditor;
            tagSetEditor.getModel().triggerFlush();
            tagSetEditor.getModel().setBean((TagSet) evt.getNewValue());
          }
          if (currentEditor != newEditor) {
            tagEditorContainer.removeAll();
            if (newEditor != null) {
              tagEditorContainer.add(newEditor);
            }
            currentEditor = newEditor;
            tagEditorContainer.validate();
            tagEditorContainer.repaint();
          }
        }
      });

      JPanel mainPanel = new JPanel(new BorderLayout());

      JToolBar toolbar = new JToolBar();

      JButton newBtn = new JButton(new AddAction());

      toolbar.add(newBtn);
      toolbar.addSeparator();

      SaveAction save = new SaveAction();
      selection.addValueChangeListener(save);
      toolbar.add(save);

      DeleteAction delete = new DeleteAction();
      selection.addValueChangeListener(delete);
      toolbar.add(delete);

      mainPanel.add(toolbar, BorderLayout.NORTH);
      mainPanel.add(tagEditorContainer, BorderLayout.CENTER);

      this.add(mainPanel, new CellConstraints(4, 2, CellConstraints.FILL,
          CellConstraints.FILL));

    }

    this.setSize(600, 400);
    SwingTools.centerWithin(ctx.getRootWindow(), this);
  }

  private class AddAction extends AbstractAction {
    private static final long serialVersionUID = -7096472776905831483L;

    AddAction() {
      this.putValue(Action.SMALL_ICON, ctx.getIcon("filenew.png"));
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      TagSet tagSet = new TagSet("Neu");
      selection.setValue(tagSet);
    }

  }

  private class SaveAction extends AbstractAction implements
      PropertyChangeListener {
    private static final long serialVersionUID = -7540437073407576092L;

    SaveAction() {
      this.putValue(Action.SMALL_ICON, ctx.getIcon("save.png"));
      this.putValue(Action.SHORT_DESCRIPTION,
          ctx.getString("titletagsetmanager.action.save.tooltip"));
      this.setEnabled(false);
    }

    public void actionPerformed(ActionEvent evt) {
      try {
        if (currentEditor instanceof TagSetEditor) {
          ((TagSetEditor) currentEditor).getModel().triggerCommit();
          TagSet tagSet = ((TagSetEditor) currentEditor).getModel()
              .getBean();
          tagManager.saveTagSet(tagSet);
        } 
      } catch (IOException e) {
        ErrorInfo errorInfo = ctx.createErrorInfo(e,
            "titletagsetmanager.action.save.failed");
        JXErrorPane.showDialog(TagSetManagerDlg.this, errorInfo);
      }

    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      TagSet tagSet = (TagSet) evt.getNewValue();
      this.setEnabled(tagSet != null);
    }

  }

  private class DeleteAction extends AbstractAction implements
      PropertyChangeListener {
    private static final long serialVersionUID = -1424588336495262853L;
    private TagSet tagSet;

    DeleteAction() {
      this.putValue(Action.SMALL_ICON, ctx.getIcon("delete.png"));
      this.putValue(Action.SHORT_DESCRIPTION,
          ctx.getString("titletagsetmanager.action.delete.tooltip"));
      this.setEnabled(false);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      if (JOptionPane
          .showConfirmDialog(
              TagSetManagerDlg.this,
              ctx.getString("titletagsetmanager.action.delete.confirm",
                  tagSet.getName()), "", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        try {
          tagManager.deleteTagSet(tagSet);
        } catch (IOException e) {
          ErrorInfo errorInfo = ctx.createErrorInfo(e,
              "titletagmanager.action.delete.failed");
          JXErrorPane.showDialog(TagSetManagerDlg.this, errorInfo);
        }
      }
    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      this.tagSet = (TagSet) evt.getNewValue();
      this.setEnabled(this.tagSet != null);
    }
  }

}
