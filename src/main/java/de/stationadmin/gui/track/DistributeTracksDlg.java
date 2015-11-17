/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.jdesktop.swingx.JXLabel;

import com.jgoodies.binding.list.IndirectListModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.util.TrackDistributor;
import de.stationadmin.base.track.Title;
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
public class DistributeTracksDlg extends JDialog {
  private static final long serialVersionUID = -3958107010200832125L;
  private ClientContext ctx;
  private TextProvider textProvider;
  private List<Title> titles;
  
  public DistributeTracksDlg(ClientContext ctx, List<Title> titles) {
    super();
    this.ctx = ctx;
    this.textProvider = ctx.getTextProvider();
    this.titles = titles;
    this.init();
  }

  private void init() {
    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu","5dlu,pref:grow,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();
    
    ValueModel selectionHolder = new ValueHolder();
    PlaylistSelector selector = new PlaylistSelector(ctx, null, selectionHolder);
    selector.setSelectionModel(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    selector.setListCellRenderer(new SimplePlaylistListCellRender());
    this.getContentPane().add(selector, cc.xy(2,2, CellConstraints.FILL, CellConstraints.FILL));

    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
    buttonPanel.add(new JButton(new DistributeAction(selectionHolder)));
    buttonPanel.add(new JButton(new DisposeAction(this, textProvider.getString("cancel"))));
    
    this.getContentPane().add(buttonPanel, cc.xy(2,4, CellConstraints.CENTER, CellConstraints.CENTER));
    
    this.setSize(300, 400);
    this.setTitle(textProvider.getString("distributetitles.title"));
    SwingTools.centerWithin(ctx.getRootWindow(), this);
  }
  
  private class DistributeAction extends AbstractAction implements PropertyChangeListener {
    private static final long serialVersionUID = -3090928011040468748L;
    private ValueModel selectionHolder;

    DistributeAction(ValueModel selectionHolder) {
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
      ArrayList<Title> failedTitles = new ArrayList<Title>();
      TrackDistributor dist = new TrackDistributor();
      if(dist.distributeTitles(getSelectedPlaylists(), titles, failedTitles)) {
        dispose();
      }
      else {
        // display error information
        JList list = new JList(new IndirectListModel<Title>(failedTitles));
        JDialog errorDlg = new JDialog();
        JXLabel label = new JXLabel(textProvider.getString("distributetitles.errormsg"));
        label.setLineWrap(true);
        errorDlg.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu","5dlu,pref,5dlu,pref:grow,5dlu,pref,5dlu"));
        CellConstraints cc = new CellConstraints();
        errorDlg.getContentPane().add(label, cc.xy(2,2));
        errorDlg.getContentPane().add(new JScrollPane(list), cc.xy(2,4, CellConstraints.FILL, CellConstraints.FILL));
        
        errorDlg.getContentPane().add(new JButton(new DisposeAction(errorDlg, textProvider.getString("ok"))), cc.xy(2, 6, CellConstraints.CENTER, CellConstraints.CENTER));
        
        errorDlg.setSize(200, 300);
        SwingTools.centerWithin(ctx.getRootWindow(), errorDlg);
        errorDlg.setModal(true);
        errorDlg.setVisible(true);
        
      }
      
    }

    @SuppressWarnings("unchecked")
    private List<Playlist> getSelectedPlaylists() {
      ArrayList<Playlist> playlists = new ArrayList<Playlist>();
      if(selectionHolder.getValue() instanceof Playlist) {
        playlists.add((Playlist)selectionHolder.getValue());
      }
      if(selectionHolder.getValue() instanceof List<?>) {
        playlists.addAll((List<Playlist>)selectionHolder.getValue());
      }
      
      return playlists;
    }
    
    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      this.setEnabled(this.getSelectedPlaylists().size() > 0);
    }
    
  }

}
