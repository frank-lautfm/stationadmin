/**
 * 
 */
package de.stationadmin.gui.settings;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.Settings;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.HintLabel;
import de.stationadmin.gui.util.StringListTableModel;


/**
 * @author korf
 *
 */
public class ArtistNormalizePanel extends JPanel {
  private static final long serialVersionUID = -8936006124867698611L;
  private ClientContext ctx;
  private PresentationModel<Settings> model;

  /**
   * @param ctx
   * @param model
   */
  public ArtistNormalizePanel(ClientContext ctx, PresentationModel<Settings> model) {
    super();
    this.ctx = ctx;
    this.model = model;
    this.init();
  }
  
  private void init() {
    
    this.setLayout(new FormLayout("100dlu:grow", "pref,5dlu,50dlu:grow,8dlu,pref,5dlu,50dlu:grow,5dlu,pref"));
    CellConstraints cc = new CellConstraints();
    
    StringListTableModel sepModel = new StringListTableModel(ctx.getTextProvider().getString("settings.artistnormalizer.separators.header"), new ArrayList<String>(model.getBean().getArtistNormalizerSeperators()));
    this.model.getBufferedModel("artistNormalizerSeperators").setValue(sepModel.getList());
    
    this.add(new JLabel(ctx.getTextProvider().getString("settings.artistnormalizer.separators")), cc.xy(1,1));
    this.add(new JScrollPane(new JXTable(sepModel)), cc.xy(1, 3, CellConstraints.FILL, CellConstraints.FILL));
    
    HashMap<String, String> aliasMap = model.getBean().getArtistNormalizerAliases() != null ? new HashMap<String, String>(model.getBean().getArtistNormalizerAliases()) : new HashMap<String, String>();
    ArtistAliasTableModel aliasModel = new ArtistAliasTableModel(ctx.getTextProvider(), aliasMap);
    this.model.getBufferedModel("artistNormalizerAliases").setValue(aliasMap);

    this.add(new JLabel(ctx.getTextProvider().getString("settings.artistnormalizer.alias")), cc.xy(1,5));
    this.add(new JScrollPane(new JXTable(aliasModel)), cc.xy(1, 7, CellConstraints.FILL, CellConstraints.FILL));

    JLabel hint = new HintLabel(ctx.getString("settings.shuffle.hint"));
    this.add(hint,  cc.xy(1,  9));

    
  }
}
