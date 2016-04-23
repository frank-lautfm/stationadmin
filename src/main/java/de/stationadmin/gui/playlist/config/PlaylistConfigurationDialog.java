/**
 * 
 */
package de.stationadmin.gui.playlist.config;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.PlaylistService;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.playlist.PlaylistEntryJumpTarget;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.SwingTools;

/**
 * Dialog for playlist settings
 * 
 * @author Frank Korf
 */
public class PlaylistConfigurationDialog extends JDialog {
  private static final long serialVersionUID = 3125298975653805674L;
  private ClientContext ctx;
  private PlaylistService playlistService;
  private TextProvider textProvider;
  private PlaylistConfigurationModel model;

  public PlaylistConfigurationDialog(ClientContext ctx, PlaylistConfigurationModel model) {
    this.ctx = ctx;
    this.textProvider = ctx.getTextProvider();
    this.playlistService = ctx.getAdminClient().getPlaylistService();
    this.model = model;
    this.init();
  }

  private JPanel createBasePanel() {
    StringBuilder rowSpec = new StringBuilder();
    rowSpec.append("5dlu,");
    rowSpec.append("pref,5dlu,"); // name
    rowSpec.append("pref,5dlu,"); // description
    rowSpec.append("pref,5dlu,"); // shuffle 1
    rowSpec.append("pref,5dlu,"); // shuffle 2
    rowSpec.append("pref,5dlu,"); // tags
    rowSpec.append("pref,5dlu,"); // comment

    JPanel panel = new JPanel(new FormLayout("5dlu,pref,5dlu,pref,5dlu", rowSpec.toString()));
    CellConstraints cc = new CellConstraints();
    int row = 2;

    Font tfFont = null;
    Color tfBackground = null;
    // name
    {
      panel.add(new JLabel(this.textProvider.getString("playlistcfg.property.name")), cc.xy(2, row));
      JTextField tf = BasicComponentFactory.createTextField(this.model.getBufferedModel("name"));
      tf.setColumns(20);
      tfFont = tf.getFont();
      tfBackground = tf.getBackground();
      panel.add(tf, cc.xy(4, row, CellConstraints.FILL, CellConstraints.FILL));
      row += 2;
    }

    // description
    {
      panel.add(new JLabel(this.textProvider.getString("playlistcfg.property.description")), cc.xy(2, row));
      JTextArea tf = BasicComponentFactory.createTextArea(this.model.getBufferedModel("description"));
      tf.setWrapStyleWord(true);
      tf.setLineWrap(true);
      tf.setRows(2);
      tf.setColumns(20);
      tf.setFont(tfFont);
      tf.setBackground(tfBackground);
      panel.add(new JScrollPane(tf), cc.xy(4, row, CellConstraints.FILL, CellConstraints.FILL));
      row += 2;
    }

    // shuffle
    {
      panel.add(new JLabel(this.textProvider.getString("playlistcfg.property.shuffle")), cc.xy(2, row));
      JCheckBox cbLaut = BasicComponentFactory.createCheckBox(this.model.getBufferedModel("shuffle"),
          this.textProvider.getString("playlistcfg.property.shuffle.laut"));
      panel.add(cbLaut, cc.xy(4, row));
      row += 2;

      JCheckBox cbLocal = BasicComponentFactory.createCheckBox(this.model.getBufferedModel("localShuffleAllowed"),
          this.textProvider.getString("playlistcfg.property.shuffle.local"));
      panel.add(cbLocal, cc.xy(4, row));
      row += 2;
    }

    // tags
    {
      panel.add(new JLabel(this.textProvider.getString("playlistcfg.property.tags")),
          cc.xy(2, row, CellConstraints.LEFT, CellConstraints.TOP));
      JTextArea tf = BasicComponentFactory.createTextArea(this.model.getBufferedModel("tags"));
      tf.setToolTipText(this.textProvider.getString("playlistcfg.property.tags.tooltip"));
      tf.setRows(5);
      tf.setColumns(20);
      tf.setFont(tfFont);
      panel.add(new JScrollPane(tf), cc.xy(4, row, CellConstraints.FILL, CellConstraints.FILL));
      row += 2;
    }
    
    // comment
    {
      panel.add(new JLabel(this.textProvider.getString("playlistcfg.property.comment")),
          cc.xy(2, row, CellConstraints.LEFT, CellConstraints.TOP));
      JTextArea tf = BasicComponentFactory.createTextArea(this.model.getBufferedModel("comment"));
      tf.setRows(2);
      tf.setColumns(20);
      tf.setFont(tfFont);
      panel.add(new JScrollPane(tf), cc.xy(4, row, CellConstraints.FILL, CellConstraints.FILL));
      row += 2;
      
    }

    return panel;

  }

  private void init() {

    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "pref:grow,3dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    JTabbedPane tab = new JTabbedPane();
    tab.addTab(textProvider.getString("playlistcfg.tab.base"), this.createBasePanel());
    tab.addTab(textProvider.getString("playlistcfg.tab.generate.base"), new PlaylistGeneratorBaseConfigurationPanel(ctx, model));
    tab.addTab(textProvider.getString("playlistcfg.tab.generate.advice"), new PlaylistGeneratorAdviceConfigurationPanel(ctx, model));
    tab.setEnabledAt(1, !this.model.getBean().isShuffle());
    tab.setEnabledAt(2, !this.model.getBean().isShuffle());

    this.getContentPane().add(tab, cc.xy(2, 1));

    // buttons
    {
      JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));

      JButton okBtn = new JButton(textProvider.getString("ok"));
      okBtn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent evt) {
          model.triggerCommit();
          try {
            playlistService.savePlaylist(model.getBean());
            dispose();
            
            ctx.getJumpHandler().jumpTo(new PlaylistEntryJumpTarget(model.getBean(), null));
            
          } catch (Exception e) {
            JXErrorPane.showFrame(PlaylistConfigurationDialog.this,
                textProvider.createErrorInfo(e, "playlistcfg.msg.savefailed"));
          }

        }

      });

      buttonPanel.add(okBtn);
      buttonPanel.add(new JButton(new DisposeAction(this, textProvider.getString("cancel"))));

      this.getContentPane().add(buttonPanel, cc.xy(2, 3, CellConstraints.CENTER, CellConstraints.CENTER));
    }

    Dimension prefSize = this.getPreferredSize();
    this.setSize(Math.max(250, (int) prefSize.getWidth() + 30), (int) prefSize.getHeight() + 80);
    this.setTitle(textProvider.getString("playlistcfg.title"));
    SwingTools.centerWithin(ctx.getRootWindow(), this);

  }

}
