/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

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
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.playlist.PlaylistSelector;
import de.stationadmin.gui.playlist.SimplePlaylistListCellRender;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.SwingTools;

/**
 *
 * @author Frank Korf
 *
 */
public class CopyTracksDlg extends JDialog {
  private static final long serialVersionUID = -3958107010200832125L;
  private ClientContext ctx;
  private TextProvider textProvider;
  private List<BasicTrack> titles;

  public CopyTracksDlg(ClientContext ctx, List<BasicTrack> titles) {
    super();
    this.ctx = ctx;
    this.textProvider = ctx.getTextProvider();
    this.titles = titles;
    this.init();
  }

  private void init() {
    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref:grow,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    ValueModel selectionHolder = new ValueHolder();
    PlaylistSelector selector = new PlaylistSelector(ctx, null, selectionHolder);
    selector.setSelectionModel(ListSelectionModel.SINGLE_SELECTION);
    selector.setListCellRenderer(new SimplePlaylistListCellRender());
    this.getContentPane().add(selector, cc.xy(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
    buttonPanel.add(new JButton(new CopyAction(selectionHolder)));
    buttonPanel.add(new JButton(new DisposeAction(this, textProvider.getString("cancel"))));

    this.getContentPane().add(buttonPanel, cc.xy(2, 4, CellConstraints.CENTER, CellConstraints.CENTER));

    this.setSize(300, 400);
    this.setTitle(textProvider.getString("copytitles.title"));
    SwingTools.centerWithin(ctx.getRootWindow(), this);
  }

  private class CopyAction extends AbstractAction implements PropertyChangeListener {
    private static final long serialVersionUID = -3090928011040468748L;
    private ValueModel selectionHolder;

    CopyAction(ValueModel selectionHolder) {
      this.putValue(Action.NAME, textProvider.getString("ok"));
      this.setEnabled(false);
      this.selectionHolder = selectionHolder;
      this.selectionHolder.addValueChangeListener(this);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      Playlist playlist = (Playlist) this.selectionHolder.getValue();
      for (BasicTrack title : titles) {
        playlist.addTrack(title);
      }
      dispose();
    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      this.setEnabled(this.selectionHolder.getValue() != null);
    }

  }

}
