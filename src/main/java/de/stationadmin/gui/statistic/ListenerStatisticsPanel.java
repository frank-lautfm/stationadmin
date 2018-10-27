/**
 * 
 */
package de.stationadmin.gui.statistic;

import java.awt.Color;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.StationStatus;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.AppUtils;

/**
 * 
 * @author Frank Korf
 * 
 */
public class ListenerStatisticsPanel extends JPanel {
  private static final long serialVersionUID = 5107968984351221173L;
  private ClientContext ctx;
  private PresentationModel<StationStatus> model;
  private Color background = AppUtils.getTextBackgroundColor();

  public ListenerStatisticsPanel(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.model = new PresentationModel<StationStatus>(ctx.getAdminClient().getStationStatus());
    this.init();
  }

  private void init() {
    StringBuilder rowSpec = new StringBuilder();
    rowSpec.append("5dlu,");
    rowSpec.append("pref,6dlu,"); // current listeners
    rowSpec.append("pref,8dlu,"); // peak listeners
    rowSpec.append("pref,6dlu,"); // rank
    rowSpec.append("pref,7dlu:grow"); // peak

    this.setLayout(new FormLayout("5dlu,pref,10dlu:grow,pref,5dlu", rowSpec.toString()));
    CellConstraints cc = new CellConstraints();
    int row = 2;
    this.setBackground(background);

    NumberFormat nf = NumberFormat.getInstance();
    nf.setGroupingUsed(false);

    TextProvider txtProvider = ctx.getTextProvider();

    this.add(new JLabel(txtProvider.getString("statistics.property.currentListeners")), cc.xy(2, row));
    this.add(BasicComponentFactory.createLabel(this.model.getModel("currentListeners"), nf),
        cc.xy(4, row, CellConstraints.RIGHT, CellConstraints.CENTER));
    row += 2;

    this.add(new JLabel(txtProvider.getString("statistics.property.peakListeners")), cc.xy(2, row));
    this.add(BasicComponentFactory.createLabel(this.model.getModel("peakListeners"), nf),
        cc.xy(4, row, CellConstraints.RIGHT, CellConstraints.CENTER));
    row += 2;

    this.add(new JLabel(txtProvider.getString("statistics.property.rank")), cc.xy(2, row));
    this.add(BasicComponentFactory.createLabel(this.model.getModel("rank"), nf),
        cc.xy(4, row, CellConstraints.RIGHT, CellConstraints.CENTER));
    row += 2;

    this.add(new JLabel(txtProvider.getString("statistics.property.peakRank")), cc.xy(2, row));
    this.add(BasicComponentFactory.createLabel(this.model.getModel("peakRank"), nf),
        cc.xy(4, row, CellConstraints.RIGHT, CellConstraints.CENTER));
    row += 2;

  }

}
