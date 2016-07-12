/**
 * 
 */
package de.stationadmin.gui.statistic;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.StationStatus;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;

/**
 * 
 * @author Frank Korf
 * 
 */
public class StatisticsTodayPanel extends JPanel {
  private static final long serialVersionUID = 5107968984351221173L;
  private ClientContext ctx;
  private PresentationModel<StationStatus> model;

  public StatisticsTodayPanel(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.model = new PresentationModel<StationStatus>(ctx.getAdminClient().getStationStatus());
    this.init();
  }

  private void init() {
    StringBuilder rowSpec = new StringBuilder();
    rowSpec.append("5dlu,");
    rowSpec.append("pref,7dlu,"); // avg listeners
    rowSpec.append("pref,7dlu,"); // avg listening time
    rowSpec.append("pref,7dlu:grow,"); // TLH

    this.setLayout(new FormLayout("5dlu,pref,10dlu:grow,pref,5dlu", rowSpec.toString()));
    CellConstraints cc = new CellConstraints();
    int row = 2;
    this.setBackground(Color.WHITE);

    NumberFormat nf = NumberFormat.getInstance();
    nf.setGroupingUsed(false);

    TextProvider txtProvider = ctx.getTextProvider();

    this.add(new JLabel(txtProvider.getString("statistics.property.listenersToday")), cc.xy(2, row));
    this.add(BasicComponentFactory.createLabel(this.model.getModel("listenersToday"), nf),
        cc.xy(4, row, CellConstraints.RIGHT, CellConstraints.CENTER));
    row += 2;

    this.add(new JLabel(txtProvider.getString("statistics.property.avgListeningTimeToday")), cc.xy(2, row));
    this.add(BasicComponentFactory.createLabel(this.model.getModel("avgListeningTimeToday"), nf),
        cc.xy(4, row, CellConstraints.RIGHT, CellConstraints.CENTER));
    row += 2;

    this.add(new JLabel(txtProvider.getString("statistics.property.tlh")), cc.xy(2, row));
    int minutes = model.getBean().getAvgListeningTimeToday() * model.getBean().getListenersToday();
    final ValueHolder tlhT = new ValueHolder(minutes / 60);
    this.model.getBean().addPropertyChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("durationToday")) {
          int hours = model.getBean().getDurationToday();
          tlhT.setValue(hours);
        }
      }

    });
    this.add(BasicComponentFactory.createLabel(tlhT, nf), cc.xy(4, row, CellConstraints.RIGHT, CellConstraints.CENTER));
    row += 2;
  }

}
