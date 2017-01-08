/**
 * 
 */
package de.stationadmin.gui.playlist.config;

import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.playlist.PlaylistSelector;
import de.stationadmin.gui.playlist.SimplePlaylistListCellRender;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.SwingTools;

/**
 * @author korf
 *
 */
public class PlaylistSettingsCopyAction extends AbstractAction {
  private static final long serialVersionUID = -4854253992951362066L;

  private ClientContext ctx;
  private PlaylistConfigurationModel model;

  /**
   * @param ctx
   * @param model
   */
  public PlaylistSettingsCopyAction(ClientContext ctx, PlaylistConfigurationModel model) {
    super();
    this.ctx = ctx;
    this.model = model;
    this.putValue(Action.SMALL_ICON, ctx.getIcon("editcopy.png"));
    this.putValue(Action.SHORT_DESCRIPTION, "Einstellung von anderer Playlists kopieren");
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    showSelectionDialog();
  }

  private void showSelectionDialog() {

    final JDialog dlg = new JDialog();

    TextProvider textProvider = ctx.getTextProvider();

    dlg.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref:grow,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    final ValueModel selectionHolder = new ValueHolder();
    PlaylistSelector selector = new PlaylistSelector(ctx, null, selectionHolder);
    selector.setSelectionModel(ListSelectionModel.SINGLE_SELECTION);
    selector.setListCellRenderer(new SimplePlaylistListCellRender());
    dlg.getContentPane().add(selector, cc.xy(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));

    JButton okButton = new JButton(textProvider.getString("ok"));
    okButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {

        Playlist playlist = (Playlist) selectionHolder.getValue();
        if (playlist != null) {
          copyFrom(playlist);
          dlg.dispose();
        }
        else {
          Toolkit.getDefaultToolkit().beep();
        }
      }
    });

    buttonPanel.add(okButton);
    buttonPanel.add(new JButton(new DisposeAction(dlg, textProvider.getString("cancel"))));

    dlg.getContentPane().add(buttonPanel, cc.xy(2, 4, CellConstraints.CENTER, CellConstraints.CENTER));

    dlg.setSize(300, 400);
    dlg.setTitle(textProvider.getString("playlistcfg.dlg.copyfrom.title"));
    SwingTools.centerWithin(ctx.getRootWindow(), dlg);
    
    dlg.setVisible(true);

  }

  private void copyFrom(Playlist playlist) {

    model.getBufferedModel("color").setValue(playlist.getColor());
    model.getBufferedModel("shuffle").setValue(playlist.isShuffle());
    model.getBufferedModel("localShuffleAllowed").setValue(playlist.isLocalShuffleAllowed());
    model.getBufferedModel("tags").setValue(PlaylistConfigurationModel.tagsToString(playlist.getTags()));

    model.getBufferedModel("generateTags").setValue(playlist.getGenerateTags());
    model.getBufferedModel("generateTagsAll").setValue(playlist.isGenerateTagsAll());
    model.getBufferedModel("generateLength").setValue(playlist.getGenerateLength());
    model.getBufferedModel("generateMaxArtistTitles").setValue(playlist.getGenerateMaxArtistTitles());
    model.getBufferedModel("generateMinimizeArtistRepeats").setValue(playlist.isGenerateMinimizeArtistRepeats());
    model.getBufferedModel("generateTitleRepeatLevel").setValue(playlist.getGenerateTitleRepeatLevel());
    model.getBufferedModel("generatePushTag").setValue(playlist.getGeneratePushTag());

    // model.getBufferedModel("generateAdvices").setValue(playlist.getGenerateAdvices());
    model.initAdvices(playlist);
  }

}
