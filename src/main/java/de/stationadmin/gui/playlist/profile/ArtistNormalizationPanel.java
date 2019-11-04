/**
 * 
 */
package de.stationadmin.gui.playlist.profile;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXTable;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.StringListTableModel;

/**
 * @author korf
 *
 */
public class ArtistNormalizationPanel extends JPanel {
  private static final long serialVersionUID = -8936006124867698611L;
  private ClientContext ctx;
  private ArtistNormalizationModel model;

  /**
   * @param ctx
   * @param model
   */
  public ArtistNormalizationPanel(ClientContext ctx, PlaylistProfileModel profileModel) {
    super();
    this.ctx = ctx;
    this.model = profileModel.getArtistNormalization();
    this.init();
  }

  private void init() {

    this.setLayout(new FormLayout("5dlu,100dlu:grow,5dlu", "5dlu,pref,5dlu,50dlu:grow,8dlu,pref,5dlu,50dlu:grow,5dlu,pref"));
    CellConstraints cc = new CellConstraints();

    final StringListTableModel sepModel = new StringListTableModel(ctx.getTextProvider().getString("settings.artistnormalizer.separators.header"));

    this.add(new JLabel(ctx.getTextProvider().getString("settings.artistnormalizer.separators")), cc.xy(2, 2));
    this.add(new JScrollPane(new JXTable(sepModel)), cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));

    final ArtistAliasTableModel aliasModel = new ArtistAliasTableModel(ctx.getTextProvider());

    this.add(new JLabel(ctx.getTextProvider().getString("settings.artistnormalizer.alias")), cc.xy(2, 6));
    this.add(new JScrollPane(new JXTable(aliasModel)), cc.xy(2, 8, CellConstraints.FILL, CellConstraints.FILL));

    model.getBeanChannel().addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        sepModel.setList(model.getSeparators());
        aliasModel.setAliasMap(model.getAliases());
      }
    });

  }
}
