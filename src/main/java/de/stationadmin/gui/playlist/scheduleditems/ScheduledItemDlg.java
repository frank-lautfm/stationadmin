package de.stationadmin.gui.playlist.scheduleditems;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import com.jgoodies.binding.list.IndirectListModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.PlaylistService;
import de.stationadmin.base.playlist.scheduled.ScheduledItem;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.NonObservingPresentationModel;
import de.stationadmin.gui.util.ThreadedAction;

public class ScheduledItemDlg extends StationAdminFrame {
  private class DeleteAction extends AbstractAction implements PropertyChangeListener {
    private static final long serialVersionUID = -1424588336495262853L;
    private ScheduledItem item;

    DeleteAction() {
      this.putValue(Action.SMALL_ICON, ctx.getIcon("delete.png"));
      // this.putValue(Action.SHORT_DESCRIPTION,
      // ctx.getString("titletagmanager.action.delete.tooltip"));
      this.setEnabled(false);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      if (JOptionPane.showConfirmDialog(ScheduledItemDlg.this, ctx.getString("scheduleditems.action.delete.confirm", item.getName()), "",
          JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        try {
          ctx.getAdminClient().getPlaylistService().updateScheduledItemsOpts(item.getId(), true);
          ctx.getAdminClient().getPlaylistService().removeScheduledItem(item.getId());
          ctx.getAdminClient().getPlaylistService().saveScheduledItems();
          ctx.getAdminClient().getClientConfigService().write();
        } catch (Exception e) {
          ErrorInfo errorInfo = ctx.createErrorInfo(e, "scheduleditems.action.delete.failed");
          JXErrorPane.showDialog(ScheduledItemDlg.this, errorInfo);
        }
      }
    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      item = (ScheduledItem) evt.getNewValue();
      this.setEnabled(item != null && ctx.getAdminClient().getPlaylistService().getScheduledItem(item.getId()) != null);
    }
  }

  private class SaveAction extends AbstractAction implements PropertyChangeListener {
    private static final long serialVersionUID = -7540437073407576092L;

    SaveAction() {
      this.putValue(Action.SMALL_ICON, ctx.getIcon("save.png"));
      this.setEnabled(false);
    }

    public void actionPerformed(ActionEvent evt) {
      try {
        ScheduledItem item = (ScheduledItem) selection.getValue();
        if(StringUtils.isEmpty((String)model.getBufferedValue("name"))) {
          ErrorInfo errorInfo = ctx.createErrorInfo(null, "scheduleditems.title.error.missing.name");
          JXErrorPane.showDialog(ScheduledItemDlg.this, errorInfo);
        	return;
        }
        if(model.getBufferedValue("tag") == null) {
          ErrorInfo errorInfo = ctx.createErrorInfo(null, "scheduleditems.title.error.missing.tag");
          JXErrorPane.showDialog(ScheduledItemDlg.this, errorInfo);
        	return;
        }
        model.triggerCommit();
        if (item != null) {
          boolean isNew = false;
          if (ctx.getAdminClient().getPlaylistService().getScheduledItem(item.getId()) == null) {
            ctx.getAdminClient().getPlaylistService().addScheduledItem(item);
            isNew = true;
          }
          ctx.getAdminClient().getPlaylistService().saveScheduledItems();
          ctx.getAdminClient().getClientConfigService().write();
          if (!isNew) {
            UpdateScheduledItemsAction updateAction = new UpdateScheduledItemsAction(ctx, item.getId());
            updateAction.actionPerformed(new ActionEvent(this, 0, "update"));
          }
        }
      } catch (IOException e) {
        ErrorInfo errorInfo = ctx.createErrorInfo(e, "action.error.generic");
        JXErrorPane.showDialog(ScheduledItemDlg.this, errorInfo);
      }

    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      this.setEnabled(evt.getNewValue() != null);
    }

  }

  private class UpdateScheduledItemsAction extends ThreadedAction {
    private static final long serialVersionUID = -6933201007843938751L;
    private String itemId;

    public UpdateScheduledItemsAction(ClientContext ctx, String itemId) {
      super(ctx);
      this.itemId = itemId;
    }

    @Override
    protected String getStatus() {
      return ctx.getString("shuffleopts.update.status");
    }

    @Override
    protected void performAction() throws Exception {
      ctx.getAdminClient().getPlaylistService().updateScheduledItemsOpts(itemId, false);

    }

    @Override
    protected void showError(Exception e) {
      JXErrorPane.showDialog(AppUtils.getRootFrame(), ctx.createErrorInfo(e, "shuffleopts.update.error"));

    }

  }

  private static final long serialVersionUID = 2613859345797901157L;
  private NonObservingPresentationModel<ScheduledItem> model;
  private ValueHolder selection = new ValueHolder(null, true);

  public ScheduledItemDlg(ClientContext ctx) throws HeadlessException {
    super(ctx, "ScheduledItem");
    model = new NonObservingPresentationModel<ScheduledItem>(selection);
    initialize();
  }

  @SuppressWarnings("unchecked")
  private void initialize() {
    this.getContentPane().setLayout(new FormLayout("5dlu,120dlu,5dlu,pref:grow,5dlu", "5dlu,pref:grow,5dlu"));
    this.setTitle(ctx.getString("scheduleditems.title"));

    final PlaylistService service = this.ctx.getAdminClient().getPlaylistService();
    {
    	List<ScheduledItem> items = new ArrayList<>(service.getScheduledItems());
    	items.sort((a, b) -> StringUtils.trimToEmpty(a.getName()).compareToIgnoreCase(StringUtils.trimToEmpty(b.getName())));
    	
      final IndirectListModel<ScheduledItem> model = new IndirectListModel<ScheduledItem>(items);

      final JList<ScheduledItem> list = new JList<ScheduledItem>(model);
      list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      list.addListSelectionListener(new ListSelectionListener() {

        @Override
        public void valueChanged(ListSelectionEvent e) {
          if (!e.getValueIsAdjusting()) {
            selection.setValue(list.getSelectedValue());
          }
        }

      });

      service.addPropertyChangeListener("scheduledItems", new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
        	model.setList(ctx.getAdminClient().getPlaylistService().getScheduledItems());
          model.fireContentsChanged(0, ctx.getAdminClient().getPlaylistService().getScheduledItems().size());
          list.getSelectionModel().clearSelection();
        }

      });

      this.getContentPane().add(new JScrollPane(list), new CellConstraints(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    }

    {

      final JPanel container = new JPanel(new BorderLayout());
      final JPanel editor = new ScheduledItemEditor(ctx.getAdminClient().getTagManager(), ctx.getAdminClient().getTrackService().getTrackRegistry(), ctx.getTextProvider(), model);
      this.selection.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          if (evt.getNewValue() != null) {
            if (container.getComponentCount() == 0) {
              System.out.println("add edtior");
              container.add(editor, BorderLayout.CENTER);

            }
          } else {
            container.removeAll();
          }
          container.validate();
          container.repaint();
        }
      });

      JPanel mainPanel = new JPanel(new BorderLayout());

      JToolBar toolbar = new JToolBar();

      JButton newBtn = new JButton(ctx.getIcon("filenew.png"));
      newBtn.setToolTipText(ctx.getString("scheduleditems.action.new.tooltip"));
      newBtn.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent evt) {
          selection.setValue(new ScheduledItem());
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
      mainPanel.add(container, BorderLayout.CENTER);

      this.add(mainPanel, new CellConstraints(4, 2, CellConstraints.FILL, CellConstraints.FILL));

    }

  }

  @Override
  protected Dimension getDefaultSize() {
    return new Dimension(600, 500);
  }

}
