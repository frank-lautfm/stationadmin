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
public class StatisticsYesterdayPanel extends JPanel {
  private static final long serialVersionUID = 5107968984351221173L;
  private ClientContext ctx;
  private PresentationModel<StationStatus> model;

  public StatisticsYesterdayPanel(ClientContext ctx) {
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

    this.add(new JLabel(txtProvider.getString("statistics.property.listenersYesterday")), cc.xy(2, row));
    this.add(BasicComponentFactory.createLabel(this.model.getModel("listenersYesterday"), nf),
        cc.xy(4, row, CellConstraints.RIGHT, CellConstraints.CENTER));
    row += 2;

    this.add(new JLabel(txtProvider.getString("statistics.property.avgListeningTimeYesterday")), cc.xy(2, row));
    this.add(BasicComponentFactory.createLabel(this.model.getModel("avgListeningTimeYesterday"), nf),
        cc.xy(4, row, CellConstraints.RIGHT, CellConstraints.CENTER));
    row += 2;

    this.add(new JLabel(txtProvider.getString("statistics.property.tlh")), cc.xy(2, row));

    int minutes = model.getBean().getAvgListeningTimeYesterday() * model.getBean().getListenersYesterday();
    final ValueHolder tlhY = new ValueHolder(minutes / 60);
    this.model.getBean().addPropertyChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("durationYesterday")) {
          int hours = model.getBean().getDurationYesterday();
          tlhY.setValue(hours);
        }
      }

    });
    this.add(BasicComponentFactory.createLabel(tlhY, nf), cc.xy(4, row, CellConstraints.RIGHT, CellConstraints.CENTER));
    row += 2;

  }

}
