/**
 * 
 */
package de.stationadmin.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.jdesktop.swingx.JXLabel;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.gui.util.AppUtils;

/**
 * @author Frank
 * 
 */
public class SynchronizationDialog extends JDialog {
  private static final long serialVersionUID = -7502374435874064610L;
  private TextProvider textProvider;
  private StationAdminClient adminClient;

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

    JXLabel description = new JXLabel(
        "Es sind Daten auf dem laut.fm-Server verändert worden. Station Admin ist möglicherweise nicht auf dem aktuellen Stand. Was möchtest Du tun?");
    description.setLineWrap(true);
    this.getContentPane().add(description, BorderLayout.NORTH);

    JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 5, 5));
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

    JButton fullSync = new JButton(
        "<html><b>Komplette Synchronisierung</b><br>Alle Playlists und Sendeplan neu laden, nach neuen eigenen Titeln suchen</html>");
    fullSync.setMargin(new Insets(10, 10, 10, 10));
    fullSync.setHorizontalTextPosition(SwingConstants.LEFT);
    fullSync.setHorizontalAlignment(SwingConstants.LEFT);
    fullSync.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        SynchronizeAction action = new SynchronizeAction(textProvider, adminClient);
        action.actionPerformed(e);
        dispose();
      }
    });

    final int[] modifiedPlaylistsIds = adminClient.getPlaylistService().getPlaylistModificationDetector().getModifiedPlaylistIds();

    JButton partSync = new JButton(
        "<html><b>Geänderte Playlists aktualisieren</b><br>Nur Playlists mit geänderter Länge neu laden, Sendeplan beibehalten</html>");
    partSync.setEnabled(modifiedPlaylistsIds.length > 0);
    partSync.setMargin(new Insets(10, 10, 10, 10));
    partSync.setHorizontalTextPosition(SwingConstants.LEFT);
    partSync.setHorizontalAlignment(SwingConstants.LEFT);
    partSync.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        SynchronizeAction action = new PartialSynchronizeAction(textProvider, adminClient, modifiedPlaylistsIds);
        action.actionPerformed(e);
        dispose();
      }
    });

    JButton noSync = new JButton("<html><b>Ignorieren</b><br>Playlists und Sendeplan beibehalten</html>");
    noSync.setMargin(new Insets(10, 10, 10, 10));
    noSync.setHorizontalTextPosition(SwingConstants.LEFT);
    noSync.setHorizontalAlignment(SwingConstants.LEFT);
    noSync.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        adminClient.getPlaylistService().getPlaylistModificationDetector().markClean();
        dispose();
      }
    });

    buttonPanel.add(fullSync);
    buttonPanel.add(partSync);
    buttonPanel.add(noSync);

    this.setTitle("Synchronisieren");
    this.getContentPane().add(buttonPanel, BorderLayout.CENTER);

    ((JPanel) this.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    Dimension dim = this.getPreferredSize();
    this.setSize(Math.min(500, dim.width + 20), dim.height + 60);

    AppUtils.centerWithinRoot(this);
  }

}
