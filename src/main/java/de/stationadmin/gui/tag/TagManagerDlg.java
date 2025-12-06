/**
 * 
 */
package de.stationadmin.gui.tag;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
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
import de.stationadmin.base.tag.Tag;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;
import de.stationadmin.gui.util.SwingTools;

/**
 * 
 * @author Frank Korf
 * 
 */
public class TagManagerDlg extends StationAdminFrame {
  private static final long serialVersionUID = -8023463627796934049L;
  private TagManager tagManager;
  private ValueHolder selection = new ValueHolder();

  private JPanel tagEditorContainer;
  private JPanel currentEditor;
  private StaticTagEditor staticTagEditor;
  private DynamicTagEditor dynamicTagEditor;

  public TagManagerDlg(ClientContext ctx) {
    super(ctx, "TagManager");
    this.tagManager = ctx.getAdminClient().getTagManager();
    this.init();
  }

  private void init() {
    this.getContentPane().setLayout(new FormLayout("5dlu,120dlu,5dlu,pref:grow,5dlu", "5dlu,pref:grow,5dlu"));
    this.setTitle(ctx.getString("titletagmanager.title"));

    final IndirectListModel<Tag> model = new IndirectListModel<Tag>(this.tagManager.getTagObjects());
    final JList list = new JList(model);
    {

      list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      list.addListSelectionListener(new ListSelectionListener() {

        @Override
        public void valueChanged(ListSelectionEvent e) {
          if (!e.getValueIsAdjusting()) {
            selection.setValue(list.getSelectedValue());
          }
        }

      });

      this.tagManager.addPropertyChangeListener("tags", new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          model.setList(tagManager.getTagObjects());
          list.getSelectionModel().clearSelection();
        }

      });

      this.getContentPane().add(new JScrollPane(list), new CellConstraints(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    }

    {
      this.staticTagEditor = new StaticTagEditor(ctx);
      this.dynamicTagEditor = new DynamicTagEditor(ctx);
      this.tagEditorContainer = new JPanel(new BorderLayout());

      this.selection.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          JPanel newEditor = null;
          if (evt.getNewValue() instanceof DynamicTag) {
            dynamicTagEditor.prepareDisplay();
            newEditor = dynamicTagEditor;
            dynamicTagEditor.getModel().triggerFlush();
            dynamicTagEditor.getModel().setBean((DynamicTag) evt.getNewValue());
          } else if (evt.getNewValue() instanceof StaticTag) {
            newEditor = staticTagEditor;
            staticTagEditor.getModel().triggerFlush();
            staticTagEditor.getModel().setBean((StaticTag) evt.getNewValue());
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

      final JPopupMenu newPopup = new JPopupMenu();
      newPopup.add(new AddAction(true));
      newPopup.add(new AddAction(false));

      JButton newBtn = new JButton(ctx.getIcon("filenew.png"));
      newBtn.setToolTipText(ctx.getString("titletagmanager.action.new.tooltip"));
      newBtn.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent evt) {
          JButton src = (JButton) evt.getSource();
          newPopup.show(src, 0, src.getHeight());
        }
      });

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
      
      JPopupMenu popup = new JPopupMenu();
      DeleteAction deletePopup = new DeleteAction();
      deletePopup.asPopupItem();
      selection.addValueChangeListener(deletePopup);
      popup.add(deletePopup);
      
      RenameAction rename = new RenameAction();
      selection.addValueChangeListener(rename);
      popup.add(rename);
      
      
      list.addMouseListener(new MouseAdapter() {

        private void checkPopup(MouseEvent e) {
          if (e.isPopupTrigger()) {
            popup.show(list, e.getX(), e.getY());
          }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
          this.checkPopup(e);
        }

        /**
         * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
         */
        @Override
        public void mousePressed(MouseEvent e) {
          this.checkPopup(e);
        }

        /**
         * @see java.awt.event.MouseAdapter#mouseReleased(java.awt.event.MouseEvent)
         */
        @Override
        public void mouseReleased(MouseEvent e) {
          this.checkPopup(e);
        }

      });
      

      this.add(mainPanel, new CellConstraints(4, 2, CellConstraints.FILL, CellConstraints.FILL));

    }
  }
  
  protected Dimension getDefaultSize() {
    return new Dimension(600, 400);
  }

  private class AddAction extends AbstractAction {
    private static final long serialVersionUID = -7096472776905831483L;
    boolean staticTag;

    AddAction(boolean staticTag) {
      this.staticTag = staticTag;
      if (staticTag) {
        this.putValue(Action.NAME, ctx.getString("titletagmanager.action.new.static"));
      } else {
        this.putValue(Action.NAME, ctx.getString("titletagmanager.action.new.dynamic"));
      }
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      if (staticTag) {
        StaticTag tag = new StaticTag();
        tag.setName(StaticTagEditor.NAME_NEW);
        selection.setValue(tag);
      } else {
        DynamicTag tag = new DynamicTag();
        tag.setName("Neu");
        selection.setValue(tag);
      }
    }

  }

  private class SaveAction extends AbstractAction implements PropertyChangeListener {
    private static final long serialVersionUID = -7540437073407576092L;

    SaveAction() {
      this.putValue(Action.SMALL_ICON, ctx.getIcon("save.png"));
      this.putValue(Action.SHORT_DESCRIPTION, ctx.getString("titletagmanager.action.save.tooltip"));
      this.setEnabled(false);
    }

    public void actionPerformed(ActionEvent evt) {
      try {
        if (currentEditor instanceof StaticTagEditor) {
          ((StaticTagEditor) currentEditor).getModel().triggerCommit();
          StaticTag tag = ((StaticTagEditor) currentEditor).getModel().getBean();
          if (tag.getName().trim().length() == 0) {
            ErrorInfo errorInfo = ctx.createErrorInfo(null, "titletagmanager.action.save.illegalname.empty");
            JXErrorPane.showDialog(TagManagerDlg.this, errorInfo);
          } else if (tag.getName().contains("/")) {
            ErrorInfo errorInfo = ctx.createErrorInfo(null, "titletagmanager.action.save.illegalname.slash");
            JXErrorPane.showDialog(TagManagerDlg.this, errorInfo);
          } else {
            tagManager.saveStaticTag(tag);
          }
        } else if (currentEditor instanceof DynamicTagEditor) {
          ((DynamicTagEditor) currentEditor).getModel().triggerCommit();
          DynamicTag tag = ((DynamicTagEditor) currentEditor).getModel().getBean();
          tagManager.saveDynamicTag(tag);
        }
        ctx.getAdminClient().getClientConfigService().write();
      } catch (IOException e) {
        ErrorInfo errorInfo = ctx.createErrorInfo(e, "titletagmanager.action.save.failed");
        JXErrorPane.showDialog(TagManagerDlg.this, errorInfo);
      }

    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      Tag tag = (Tag) evt.getNewValue();
      this.setEnabled(tag != null);
    }

  }

  private class DeleteAction extends AbstractAction implements PropertyChangeListener {
    private static final long serialVersionUID = -1424588336495262853L;
    private Tag tag;

    DeleteAction() {
      this.putValue(Action.SMALL_ICON, ctx.getIcon("delete.png"));
      this.putValue(Action.SHORT_DESCRIPTION, ctx.getString("titletagmanager.action.delete.tooltip"));
      this.setEnabled(false);
    }
    
    public void asPopupItem() {
      this.putValue(Action.SMALL_ICON, null);
      this.putValue(Action.NAME, ctx.getTextProvider().getString("delete"));
    }
    


    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      this.tag = (Tag) evt.getNewValue();
      this.setEnabled(this.tag != null && tagManager.getTag(tag.getName()) != null);
    }
  }
  
  private class RenameAction extends AbstractAction implements PropertyChangeListener {
		private static final long serialVersionUID = -4792388743411183829L;
		private Tag tag;

    RenameAction() {
			this.putValue(Action.NAME, ctx.getTextProvider().getString("rename"));
      this.setEnabled(false);
    }
    
    


    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
    	RenameTagDlg dlg = new RenameTagDlg(ctx, tag);
    	SwingTools.centerWithin(TagManagerDlg.this, dlg);
    	dlg.setVisible(true);
    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      this.tag = (Tag) evt.getNewValue();
      this.setEnabled(this.tag != null && tagManager.getTag(tag.getName()) != null);
    }
  }


}
