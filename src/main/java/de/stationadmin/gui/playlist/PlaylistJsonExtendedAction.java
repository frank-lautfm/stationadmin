/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FileUtils;
import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.SwingTools;

/**
 * Displays extended raw playlist data (automation_algorithm_name, shuffle_opts, sorted tracks)
 * in a dialog from which the user can copy to clipboard or save to a JSON file.
 * The output format is compatible with StationAdmin.ts for developer testing.
 *
 * @author korf
 */
public class PlaylistJsonExtendedAction extends AbstractAction {
  private static final long serialVersionUID = 7312045891234567890L;
  private ClientContext ctx;
  private ValueModel playlistHolder;

  /**
   * @param ctx
   * @param playlistHolder
   */
  public PlaylistJsonExtendedAction(ClientContext ctx, ValueModel playlistHolder) {
    super();
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.playlist.json"));
    this.ctx = ctx;
    this.playlistHolder = playlistHolder;
    this.setEnabled(false);
    playlistHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        setEnabled(evt.getNewValue() instanceof Playlist
            && ((Playlist) evt.getNewValue()).getType() == PlaylistType.ONLINE);
      }
    });
  }

  @Override
  public void actionPerformed(ActionEvent evt) {
    final Playlist pl = (Playlist) playlistHolder.getValue();

    try {
      final String json = ctx.getAdminClient().getPlaylistService().getPlaylistExtendedJson(pl.getId());

      final JDialog dlg = new JDialog();
      dlg.setTitle(pl.getName());

      final JTextArea ta = new JTextArea(30, 80);
      ta.setEditable(false);
      ta.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
      ta.setText(json);
      ta.setCaretPosition(0);

      // Button panel
      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

      JButton clipboardBtn = new JButton(ctx.getString("action.clipboard.copy"));
      clipboardBtn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          StringSelection str = new StringSelection(json);
          dlg.getToolkit().getSystemClipboard().setContents(str, str);
        }
      });

      JButton saveBtn = new JButton(ctx.getString("save"));
      saveBtn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          JFileChooser fileChooser = new JFileChooser();
          fileChooser.setFileFilter(new FileNameExtensionFilter("JSON", "json"));
          fileChooser.setSelectedFile(new File(
              fileChooser.getCurrentDirectory().getAbsolutePath()
              + File.separatorChar + pl.getName() + ".json"));
          if (fileChooser.showSaveDialog(dlg) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".json")) {
              file = new File(file.getAbsolutePath() + ".json");
            }
            try {
              FileUtils.writeStringToFile(file, json, "UTF-8");
            } catch (IOException ex) {
              JXErrorPane.showDialog(dlg, ctx.createErrorInfo(ex, "action.error.generic"));
            }
          }
        }
      });

      buttonPanel.add(clipboardBtn);
      buttonPanel.add(saveBtn);

      dlg.getContentPane().setLayout(new BorderLayout());
      dlg.getContentPane().add(new JScrollPane(ta), BorderLayout.CENTER);
      dlg.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
      dlg.setSize(700, 800);
      SwingTools.centerOnScreen(dlg);
      dlg.setVisible(true);

    } catch (Exception e) {
      JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "action.error.generic"));
    }
  }

}
