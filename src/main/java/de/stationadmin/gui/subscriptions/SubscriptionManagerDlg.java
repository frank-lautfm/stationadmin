/**
 * 
 */
package de.stationadmin.gui.subscriptions;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.error.ErrorInfo;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.beans.BeanAdapter;
import com.jgoodies.binding.list.IndirectListModel;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.subscription.Subscription;
import de.stationadmin.base.subscription.SubscriptionService;
import de.stationadmin.base.subscription.Subscription.Field;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.SwingTools;

/**
 * 
 * @author Frank Korf
 * 
 */
public class SubscriptionManagerDlg extends JFrame {
  private static final long serialVersionUID = -8023463627796934049L;
  private ClientContext ctx;
  private SubscriptionService service;
  private ValueHolder selection = new ValueHolder(null, true);
  private PresentationModel<Subscription> presentationModel;

  public SubscriptionManagerDlg(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.service = ctx.getAdminClient().getSubscriptionService();
    this.init();
  }

  private void init() {
    this.getContentPane().setLayout(new FormLayout("5dlu,120dlu,5dlu,pref:grow,5dlu", "5dlu,pref:grow,5dlu"));
    this.setTitle(ctx.getString("subscriptionmanager.title"));

    {
      final IndirectListModel<Subscription> model = new IndirectListModel<Subscription>(getAvailableSubscriptions());

      final JList list = new JList(model);
      list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      list.addListSelectionListener(new ListSelectionListener() {

        @Override
        public void valueChanged(ListSelectionEvent e) {
          if (!e.getValueIsAdjusting()) {
            selection.setValue(list.getSelectedValue());
          }
        }

      });

      this.service.addPropertyChangeListener("subscriptions", new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          model.setList(getAvailableSubscriptions());
          list.getSelectionModel().clearSelection();
        }

      });

      this.getContentPane().add(new JScrollPane(list), new CellConstraints(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    }

    {

      JPanel mainPanel = new JPanel(new BorderLayout());

      JToolBar toolbar = new JToolBar();
      toolbar.add(new AddAction());
      toolbar.addSeparator();

      SaveAction save = new SaveAction();
      selection.addValueChangeListener(save);
      toolbar.add(save);

      DeleteAction delete = new DeleteAction();
      selection.addValueChangeListener(delete);
      toolbar.add(delete);

      mainPanel.add(toolbar, BorderLayout.NORTH);
      mainPanel.add(createEditorPanel(), BorderLayout.CENTER);

      this.add(mainPanel, new CellConstraints(4, 2, CellConstraints.FILL, CellConstraints.FILL));

    }

    this.setSize(600, 400);
    SwingTools.centerWithin(ctx.getRootWindow(), this);
  }

  private List<Subscription> getAvailableSubscriptions() {
    ArrayList<Subscription> subscriptions = new ArrayList<Subscription>(service.getSubscriptions());
    Collections.sort(subscriptions, new Comparator<Subscription>() {

      @Override
      public int compare(Subscription o1, Subscription o2) {
        return o1.toString().compareTo(o2.toString());
      }
    });

    return subscriptions;

  }

  private JPanel createEditorPanel() {
    this.presentationModel = new PresentationModel<Subscription>(this.selection) {

      @Override
      protected BeanAdapter<Subscription> createBeanAdapter(ValueModel beanChannel) {
        return new BeanAdapter<Subscription>(beanChannel, false);
      }

    };

    JPanel panel = new JPanel(new FormLayout("5dlu,pref,5dlu,pref,pref:grow,5dlu", "7dlu,pref,8dlu,pref,5dlu,pref,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    JXLabel desc = new JXLabel(ctx.getString("subscription.description"));
    desc.setLineWrap(true);
    panel.add(desc, cc.xywh(2, 2, 4, 1));

    SelectionInList<Field> fieldSelection = new SelectionInList<Field>(Field.values(), this.presentationModel.getBufferedModel("field"));
    final JComboBox cmb = BasicComponentFactory.createComboBox(fieldSelection, new DefaultListCellRenderer() {

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value != null) {
          setText(ctx.getString("subscription.property.field." + ((Field) value).name().toLowerCase()));
        }
        return cmp;
      }

    });
    cmb.setEnabled(false);

    panel.add(new JLabel(ctx.getTextProvider().getString("subscription.property.field")), cc.xy(2, 4));
    panel.add(cmb, cc.xy(4, 4));

    final JTextField tf = ctx.getComponentFactory().createTextField(this.presentationModel.getBufferedModel("query"), false);
    tf.setColumns(15);
    tf.setEditable(false);
    panel.add(new JLabel(ctx.getTextProvider().getString("subscription.property.query")), cc.xy(2, 6));
    panel.add(tf, cc.xy(4, 6));

    final JCheckBox cb = BasicComponentFactory.createCheckBox(this.presentationModel.getBufferedModel("equals"),
        ctx.getTextProvider().getString("subscription.property.equals"));
    cb.setEnabled(false);
    panel.add(cb, cc.xy(4, 8));

    this.selection.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        boolean editable = evt.getNewValue() != null;
        cmb.setEnabled(editable);
        tf.setEditable(editable);
        cb.setEnabled(editable);

      }
    });

    return panel;
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
      selection.setValue(new Subscription());
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
        presentationModel.triggerCommit();
        Subscription subscription = (Subscription) selection.getValue();
        if (subscription.isNew()) {
          if (StringUtils.isNotEmpty(subscription.getQuery())) {
            service.add(subscription);
          }
        }
        service.saveSubscriptions();
      } catch (Exception e) {
        ErrorInfo errorInfo = ctx.createErrorInfo(e, "subscriptionmanager.action.save.failed");
        JXErrorPane.showDialog(SubscriptionManagerDlg.this, errorInfo);
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

  private class DeleteAction extends AbstractAction implements PropertyChangeListener {
    private static final long serialVersionUID = -1424588336495262853L;
    private Subscription subscription;

    DeleteAction() {
      this.putValue(Action.SMALL_ICON, ctx.getIcon("delete.png"));
      this.putValue(Action.SHORT_DESCRIPTION, ctx.getString("titletagmanager.action.delete.tooltip"));
      this.setEnabled(false);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      if (JOptionPane.showConfirmDialog(SubscriptionManagerDlg.this,
          ctx.getString("subscriptionmanager.action.delete.confirm", subscription.getQuery()), "", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        try {
          service.remove(subscription);
        } catch (Exception e) {
          ErrorInfo errorInfo = ctx.createErrorInfo(e, "subscriptionmanager.action.delete.failed");
          JXErrorPane.showDialog(SubscriptionManagerDlg.this, errorInfo);
        }
      }
    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      this.subscription = (Subscription) evt.getNewValue();
      this.setEnabled(this.subscription != null);
    }
  }

}
