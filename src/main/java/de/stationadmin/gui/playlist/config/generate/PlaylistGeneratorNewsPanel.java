/**
 * 
 */
package de.stationadmin.gui.playlist.config.generate;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.playlist.config.PlaylistConfigurationModel;
import de.stationadmin.gui.util.EnumListCellRenderer;

/**
 * @author korf
 * 
 */
@SuppressWarnings({ "unchecked" })
public class PlaylistGeneratorNewsPanel extends JPanel {
  private static final long serialVersionUID = 5393828754134006427L;
  private TextProvider textProvider;
  private PlaylistConfigurationModel model;

  public PlaylistGeneratorNewsPanel(ClientContext ctx, PlaylistConfigurationModel model) {
    this.textProvider = ctx.getTextProvider();
    this.model = model;
    this.init();
  }

  private void init() {
    this.setLayout(new FormLayout("5dlu,pref,5dlu,pref,5dlu", "5dlu,pref,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    this.add(new JLabel(textProvider.getString("playlistcfg.property.news")), cc.xy(2, 2));

    SelectionInList<Integer> opts = new SelectionInList<>(new Integer[] { 0, 60, 120, 180, 240, 300, 360 }, model.getBufferedModel("generateNewsInterval"));
    JComboBox<Integer> optsCmb = BasicComponentFactory.createComboBox(opts, new EnumListCellRenderer(textProvider, "playlistcfg.property.newsInterval.option"));
    this.add(optsCmb, cc.xy(4, 2, CellConstraints.LEFT, CellConstraints.CENTER));

    JCheckBox protectCb = BasicComponentFactory.createCheckBox(model.getBufferedModel("generateFirstJingleAfterNews"),
        textProvider.getString("playlistcfg.property.firstJingleAfterNews"));
    this.add(protectCb, cc.xy(4, 4, CellConstraints.LEFT, CellConstraints.CENTER));

  }

}
