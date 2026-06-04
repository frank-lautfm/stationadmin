/**
 * 
 */
package de.stationadmin.gui.synchronization;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXLabel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.DisposeAction;

/**
 * @author Frank
 * 
 */
public class SynchronizationDialog extends JDialog {
  private static final long serialVersionUID = -7502374435874064610L;
  private TextProvider textProvider;
  private StationAdminClient adminClient;

  private ValueHolder synchronizeAll = new ValueHolder(Boolean.FALSE);
  private ValueHolder synchronizePlaylistsModified = new ValueHolder(Boolean.FALSE);
  private ValueHolder synchronizePlaylistsAll = new ValueHolder(Boolean.FALSE);
  private ValueHolder synchronizeTags = new ValueHolder(Boolean.FALSE);
  private ValueHolder synchronizeSchedule = new ValueHolder(Boolean.FALSE);
  private ValueHolder synchronizeTracks = new ValueHolder(Boolean.FALSE);
  
  private int[] modifiedPlaylistsIds;

  /**
   * @param textProvider
   * @param playlistService
   */
  public SynchronizationDialog(TextProvider textProvider, StationAdminClient adminClient) {
    super();
    this.textProvider = textProvider;
    this.adminClient = adminClient;
    this.init();
  }

  private void init() {
    this.getContentPane().setLayout(new BorderLayout());
    this.setTitle("Synchronisierung - " + adminClient.getStation());

    JXLabel description = new JXLabel("Es sind Daten auf dem laut.fm-Server ver�ndert worden. Station Admin ist m�glicherweise nicht auf dem aktuellen Stand. Was m�chtest Du synchronisieren?");
    description.setLineWrap(true);
    this.getContentPane().add(description, BorderLayout.NORTH);

    modifiedPlaylistsIds = adminClient.getPlaylistService().getPlaylistModificationDetector().getModifiedPlaylistIds();

    final JCheckBox all = BasicComponentFactory.createCheckBox(synchronizeAll, this.textProvider.getString("action.synchronize.all"));
    final JCheckBox playlistsModified = BasicComponentFactory.createCheckBox(synchronizePlaylistsModified, this.textProvider.getString("action.synchronize.playlists.modified"));
    final JCheckBox playlistsAll = BasicComponentFactory.createCheckBox(synchronizePlaylistsAll, this.textProvider.getString("action.synchronize.playlists"));
    final JCheckBox tags = BasicComponentFactory.createCheckBox(synchronizeTags, this.textProvider.getString("action.synchronize.tags"));
    final JCheckBox schedule = BasicComponentFactory.createCheckBox(synchronizeSchedule, this.textProvider.getString("action.synchronize.schedule"));
    final JCheckBox tracks = BasicComponentFactory.createCheckBox(synchronizeTracks, this.textProvider.getString("action.synchronize.tracks"));

    JPanel buttonPanel = new JPanel(new GridLayout(3, 2, 5, 5));
    buttonPanel.add(all);
    buttonPanel.add(playlistsModified);
    buttonPanel.add(playlistsAll);
    buttonPanel.add(tags);
    buttonPanel.add(schedule);
    buttonPanel.add(tracks);
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

    synchronizeAll.addPropertyChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        boolean all = ((Boolean) evt.getNewValue()).booleanValue();
        playlistsAll.setEnabled(!all);
        playlistsModified.setEnabled(!all && modifiedPlaylistsIds != null && modifiedPlaylistsIds.length > 0);
        tags.setEnabled(!all);
        schedule.setEnabled(!all);
        tracks.setEnabled(!all);
      }
    });

    if (modifiedPlaylistsIds != null && modifiedPlaylistsIds.length > 0) {
      synchronizePlaylistsModified.setValue(Boolean.TRUE);
    } else {
      synchronizeAll.setValue(true);
      playlistsModified.setEnabled(false);
    }


    this.getContentPane().add(buttonPanel, BorderLayout.CENTER);

    JPanel actionPanel = new JPanel(new GridLayout(1, 2, 10, 10));
    actionPanel.add(new JButton(new SynchronizeAction(textProvider, adminClient)));
    actionPanel.add(new JButton(new DisposeAction(this, textProvider.getString("cancel"))));

    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bottomPanel.add(actionPanel);
    this.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

    ((JPanel) this.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    Dimension dim = this.getPreferredSize();
    this.setSize(Math.min(500, dim.width + 20), dim.height + 60);

    AppUtils.centerWithinRoot(this);
  }

  private class SynchronizeAction extends AbstractSynchronizeAction {
    private static final long serialVersionUID = 6475851746113625534L;

    /**
     * @param textProvider
     * @param adminClient
     */
    public SynchronizeAction(TextProvider textProvider, StationAdminClient adminClient) {
      super(textProvider, adminClient);
      this.putValue(Action.NAME, textProvider.getString("ok"));
    }
    
    protected boolean beforeExecution() {
      dispose();
      return true;
    }

    @Override
    protected void performAction() throws Exception {
      if(synchronizeAll.booleanValue()) {
        adminClient.synchronize();
      }
      else {
        boolean syncClientCfg = false;
        if(synchronizePlaylistsModified.booleanValue()) {
          adminClient.getPlaylistService().synchronize(modifiedPlaylistsIds);
          syncClientCfg = true;
        }
        if(synchronizePlaylistsAll.booleanValue()) {
          adminClient.getPlaylistService().synchronize(true);
          syncClientCfg = true;
        }
        if(synchronizeTags.booleanValue()) {
          adminClient.getTagManager().synchronize();
        }
        if(synchronizeSchedule.booleanValue()) {
          adminClient.getSchedule().synchronize();
        }
        if(synchronizeTracks.booleanValue()) {
          adminClient.getTrackService().synchronize();
        }
        if(syncClientCfg) {
          adminClient.getClientConfigService().synchronize();
        }
      }
      
    }
    
  }
  
}
