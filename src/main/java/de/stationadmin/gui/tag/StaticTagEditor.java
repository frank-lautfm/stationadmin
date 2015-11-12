/**
 * 
 */
package de.stationadmin.gui.tag;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.NonObservingPresentationModel;

/**
 * @author Frank
 * 
 */
public class StaticTagEditor extends JPanel {
  private static final long serialVersionUID = 1267493346283823161L;
  private ClientContext ctx;
  private PresentationModel<StaticTag> model = new NonObservingPresentationModel<StaticTag>((StaticTag) null);

  /**
   * @param ctx
   */
  public StaticTagEditor(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.init();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void init() {
    this.setLayout(new FormLayout("3dlu,max(pref;50dlu),3dlu,pref:grow", "3dlu,pref,5dlu,pref,3dlu"));
    CellConstraints cc = new CellConstraints();

    Vector<String> grps = new Vector<String>();
    grps.add(null);
    grps.addAll(ctx.getAdminClient().getTagManager().getGroups());
    final DefaultComboBoxModel cmbModel = new DefaultComboBoxModel(grps);
    final JComboBox groupCmb = new JComboBox(cmbModel);
    groupCmb.setEditable(true);
    this.add(new JLabel(ctx.getString("titletagmanager.property.group")), cc.xy(2, 2));
    this.add(groupCmb, cc.xy(4, 2));

    groupCmb.addItemListener(new ItemListener() {

      @Override
      public void itemStateChanged(ItemEvent e) {
        model.getBufferedModel("group").setValue(groupCmb.getSelectedItem());
        if(cmbModel.getIndexOf(groupCmb.getSelectedItem()) < 0) {
          cmbModel.addElement(groupCmb.getSelectedItem());
        }
      }
    });
    model.getBufferedModel("group").addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        Object value = model.getBufferedModel("group").getValue();
        groupCmb.setSelectedItem(value);

      }
    });

    JTextField tf = BasicComponentFactory.createTextField(model.getBufferedModel("name"));

    this.add(new JLabel(ctx.getString("titletagmanager.property.name")), cc.xy(2, 4));
    this.add(tf, cc.xy(4, 4));
  }

  /**
   * @return the model
   */
  public PresentationModel<StaticTag> getModel() {
    return model;
  }

}
