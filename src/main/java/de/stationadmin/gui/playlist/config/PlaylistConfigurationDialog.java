/**
 * 
 */
package de.stationadmin.gui.playlist.config;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;
import org.jdesktop.swingx.error.ErrorLevel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.PlaylistService;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.playlist.PlaylistEntryJumpTarget;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.EnumListCellRenderer;
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

  @SuppressWarnings("unchecked")
  private JPanel createBasePanel() {
    StringBuilder rowSpec = new StringBuilder();
    rowSpec.append("5dlu,");
    rowSpec.append("pref,5dlu,"); // name
    rowSpec.append("pref,5dlu,"); // description
    rowSpec.append("pref,5dlu,"); // color
    rowSpec.append("pref,5dlu,"); // shuffle 1
    rowSpec.append("pref,5dlu,"); // shuffle 2
    rowSpec.append("pref,5dlu,"); // tags
    rowSpec.append("pref,5dlu,"); // comment

    JPanel panel = new JPanel(new FormLayout("5dlu,pref,5dlu,pref,5dlu", rowSpec.toString()));
    CellConstraints cc = new CellConstraints();
    int row = 2;

    boolean archive = model.getBean().getType() == PlaylistType.ARCHIVED;

    Font tfFont = null;
    Color tfBackground = null;
    // name
    {
      panel.add(new JLabel(this.textProvider.getString("playlistcfg.property.name")), cc.xy(2, row));
      JTextField tf = ctx.getComponentFactory().createTextField(this.model.getBufferedModel("name"));
      tfBackground = tf.getBackground();
      tf.setColumns(20);
      tfFont = tf.getFont();
      panel.add(tf, cc.xy(4, row, CellConstraints.FILL, CellConstraints.FILL));
      row += 2;
      tf.setEditable(!archive);
    }

    // description
    {
      panel.add(new JLabel(this.textProvider.getString("playlistcfg.property.description")), cc.xy(2, row));
      JTextArea tf = ctx.getComponentFactory().createTextArea(this.model.getBufferedModel("description"));
      tf.setWrapStyleWord(true);
      tf.setEditable(true);
      tf.setLineWrap(true);
      tf.setRows(2);
      tf.setColumns(20);
      tf.setFont(tfFont);
      tf.setBackground(tfBackground);
      panel.add(new JScrollPane(tf), cc.xy(4, row, CellConstraints.FILL, CellConstraints.FILL));
      row += 2;
    }

    // color
    {
      panel.add(new JLabel(this.textProvider.getString("playlistcfg.property.color")), cc.xy(2, row));

      JPanel colorPanel = new JPanel(new FlowLayout());

      ValueModel colorModel = this.model.getBufferedModel("color");

      JTextField tf = ctx.getComponentFactory().createTextField(colorModel);
      tf.setColumns(8);

      colorPanel.add(tf);
      colorPanel.add(new ColorButton(colorModel));

      panel.add(colorPanel, cc.xy(4, row, CellConstraints.LEFT, CellConstraints.FILL));

      row += 2;

    }

    // shuffle
    {

      panel.add(new JLabel(this.textProvider.getString("playlistcfg.property.trackOrder")), cc.xy(2, row));
      SelectionInList<TrackOrderOption> trackOrderSelection = new SelectionInList<>(TrackOrderOption.values(), model.getTrackOrderType());
      JComboBox<TrackOrderOption> trackOrderCmb = BasicComponentFactory.createComboBox(trackOrderSelection,
          new EnumListCellRenderer(ctx.getTextProvider(), "playlistcfg.property.trackOrder"));
      panel.add(trackOrderCmb, cc.xy(4, row));
      row += 2;

      List<String> shuffleFuncOptions = new ArrayList<>();
      if (model.getBean().getShuffleType() == null) {
        shuffleFuncOptions.add(null);
      }
      shuffleFuncOptions.add(PlaylistConfigurationModel.SHUFFLE_CLASSIC);
      shuffleFuncOptions.add(PlaylistConfigurationModel.SHUFFLE_BUCKET);
      shuffleFuncOptions.add(PlaylistConfigurationModel.SHUFFLE_STATIONADMIN);
      shuffleFuncOptions.add(PlaylistConfigurationModel.SHUFFLE_BLOCKSELECT);

      panel.add(new JLabel(this.textProvider.getString("playlistcfg.property.shuffleFunc")), cc.xy(2, row));
      SelectionInList<String> shuffleFuncSelection = new SelectionInList<>(shuffleFuncOptions, model.getBufferedModel("shuffleType"));
      final JComboBox<String> shuffleFuncCmb = BasicComponentFactory.createComboBox(shuffleFuncSelection, new DefaultListCellRenderer() {
        private static final long serialVersionUID = 666514488594966718L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

          if (value == null) {
            setText(ctx.getTextProvider().getString("playlistcfg.property.shuffleFunc.custom"));
          } else if (value.equals(PlaylistConfigurationModel.SHUFFLE_CLASSIC)) {
            setText(ctx.getTextProvider().getString("playlistcfg.property.shuffleFunc.classic"));
          } else if (value.equals(PlaylistConfigurationModel.SHUFFLE_BUCKET)) {
            setText(ctx.getTextProvider().getString("playlistcfg.property.shuffleFunc.bucket"));
          } else if (value.equals(PlaylistConfigurationModel.SHUFFLE_STATIONADMIN)) {
            setText(ctx.getTextProvider().getString("playlistcfg.property.shuffleFunc.stationadmin"));
          } else if (value.equals(PlaylistConfigurationModel.SHUFFLE_BLOCKSELECT)) {
            setText(ctx.getTextProvider().getString("playlistcfg.property.shuffleFunc.blockselect"));
          }

          return this;
        }

      });
      
      shuffleFuncCmb.setEnabled(model.getTrackOrderType().getValue().equals(TrackOrderOption.SHUFFLE_SERVER));
      this.model.getTrackOrderType().addValueChangeListener(new PropertyChangeListener() {
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          shuffleFuncCmb.setEnabled(evt.getNewValue().equals(TrackOrderOption.SHUFFLE_SERVER));
        }
      });
      
      panel.add(shuffleFuncCmb, cc.xy(4, row));
      row += 2;

    }

    // tags
    {
      panel.add(new JLabel(this.textProvider.getString("playlistcfg.property.tags")), cc.xy(2, row, CellConstraints.LEFT, CellConstraints.TOP));
      JTextArea tf = ctx.getComponentFactory().createTextArea(this.model.getBufferedModel("tags"));
      tf.setEditable(!archive);
      tf.setToolTipText(this.textProvider.getString("playlistcfg.property.tags.tooltip"));
      tf.setRows(5);
      tf.setColumns(20);
      tf.setFont(tfFont);
      panel.add(new JScrollPane(tf), cc.xy(4, row, CellConstraints.FILL, CellConstraints.FILL));
      row += 2;
    }

    // comment
    {
      panel.add(new JLabel(this.textProvider.getString("playlistcfg.property.comment")), cc.xy(2, row, CellConstraints.LEFT, CellConstraints.TOP));
      JTextArea tf = ctx.getComponentFactory().createTextArea(this.model.getBufferedModel("comment"));
      tf.setRows(2);
      tf.setColumns(20);
      tf.setFont(tfFont);
      panel.add(new JScrollPane(tf), cc.xy(4, row, CellConstraints.FILL, CellConstraints.FILL));
      row += 2;

    }

    return panel;

  }

  private static Color parseColor(String color) {
    int red = 255;
    int green = 255;
    int blue = 255;
    if (color == null) {
      color = "#FFFFFF";
    }
    if (color.startsWith("#") && color.length() > 1) {
      color = color.substring(1);
    }

    if (color.length() > 6) {
      color = color.substring(0, 6);
    } else if (color.length() < 6) {
      for (int i = color.length(); i < 6; i++) {
        color += "0";
      }
    }

    if (color.length() == 6) {
      try {
        red = Integer.parseInt(color.substring(0, 2), 16);
        green = Integer.parseInt(color.substring(2, 4), 16);
        blue = Integer.parseInt(color.substring(4, 6), 16);
      } catch (Exception e) {

      }
    }

    return new Color(red, green, blue);

  }

  private boolean checkIfShuffleOptsEnabled() {
    return model.getTrackOrderType().getValue().equals(TrackOrderOption.SHUFFLE_SERVER)
        && (model.getBufferedModel("shuffleType").getString().equals(PlaylistConfigurationModel.SHUFFLE_BUCKET) || model.getBufferedModel("shuffleType").getString().equals(PlaylistConfigurationModel.SHUFFLE_STATIONADMIN)  || model.getBufferedModel("shuffleType").getString().equals(PlaylistConfigurationModel.SHUFFLE_BLOCKSELECT))
        && model.getBean().getType() != PlaylistType.ARCHIVED;
  }
  
  private boolean checkIfAutoFillEnabled() {
    return (model.getTrackOrderType().getValue().equals(TrackOrderOption.SHUFFLE_SERVER) || model.getTrackOrderType().getValue().equals(TrackOrderOption.SHUFFLE_LOCAL)) && model.getBean().getType() != PlaylistType.ARCHIVED;
  }


  private boolean checkIfGenerateEnabled() {
    return model.getTrackOrderType().getValue().equals(TrackOrderOption.GENERATE) && model.getBean().getType() != PlaylistType.ARCHIVED;
  }

  private void init() {

    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "1dlu,pref,0dlu,pref:grow,3dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.add(new PlaylistSettingsCopyAction(ctx, model));
    this.getContentPane().add(toolbar, cc.xy(2, 2));

    final JTabbedPane tabPane = new JTabbedPane();
    tabPane.addTab(textProvider.getString("playlistcfg.tab.base"), this.createBasePanel());
    tabPane.addTab(textProvider.getString("playlistcfg.tab.shuffleopts"), new ShuffleOptionsPanel(ctx, model));
    tabPane.addTab(textProvider.getString("playlistcfg.tab.generate.base"), new PlaylistGeneratorBaseConfigurationPanel(ctx, model));
    tabPane.addTab(textProvider.getString("playlistcfg.tab.generate.advice"), new PlaylistGeneratorAdviceConfigurationPanel(ctx, model));
    tabPane.addTab(textProvider.getString("playlistcfg.tab.autofill"), new PlaylistAutoFillPanel(ctx, model));

    tabPane.setEnabledAt(1, checkIfShuffleOptsEnabled());
    tabPane.setEnabledAt(2, checkIfGenerateEnabled());
    tabPane.setEnabledAt(3, checkIfGenerateEnabled());
    tabPane.setEnabledAt(4, checkIfAutoFillEnabled());

    model.getTrackOrderType().addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        tabPane.setEnabledAt(1, checkIfShuffleOptsEnabled());
        tabPane.setEnabledAt(2, checkIfGenerateEnabled());
        tabPane.setEnabledAt(3, checkIfGenerateEnabled());
      }
    });
    
    model.getBufferedModel("shuffle").addPropertyChangeListener(new PropertyChangeListener() {
      
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        tabPane.setEnabledAt(4, checkIfAutoFillEnabled());
      }
    });

    model.getBufferedModel("shuffleType").addPropertyChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        tabPane.setEnabledAt(1, checkIfShuffleOptsEnabled());
      }
    });

    this.getContentPane().add(tabPane, cc.xy(2, 4));

    // buttons
    {
      JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));

      JButton okBtn = new JButton(textProvider.getString("ok"));
      okBtn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent evt) {
          List<String> messages = model.validate();
          if(messages.size() > 0) {
            ErrorInfo errorInfo = new ErrorInfo(ctx.getTextProvider().getString("error.title"), messages.get(0), null,
                "general", null, ErrorLevel.SEVERE, null);
            JXErrorPane.showDialog(AppUtils.getRootFrame(), errorInfo);
            return;
          }
          
          model.triggerCommit();
          try {
            playlistService.savePlaylist(model.getBean());
            dispose();

            ctx.getJumpHandler().jumpTo(new PlaylistEntryJumpTarget(model.getBean(), null));

          } catch (Exception e) {
            JXErrorPane.showFrame(PlaylistConfigurationDialog.this, textProvider.createErrorInfo(e, "playlistcfg.msg.savefailed"));
          }

        }

      });

      buttonPanel.add(okBtn);
      buttonPanel.add(new JButton(new DisposeAction(this, textProvider.getString("cancel"))));

      this.getContentPane().add(buttonPanel, cc.xy(2, 6, CellConstraints.CENTER, CellConstraints.CENTER));
    }

    Dimension prefSize = this.getPreferredSize();
    this.setSize(Math.max(280, (int) prefSize.getWidth() + 30), (int) prefSize.getHeight() + 80);
    this.setTitle(textProvider.getString("playlistcfg.title"));
    SwingTools.centerWithin(ctx.getRootWindow(), this);

  }

  private class ColorButton extends JLabel {
    private static final long serialVersionUID = 108077490057216245L;
    private ValueModel colorModel;

    /**
     * @param colorModel
     */
    public ColorButton(ValueModel colorModel) {
      super();
      this.colorModel = colorModel;
      this.setBackground(parseColor((String) colorModel.getValue()));
      this.setOpaque(true);
      this.setPreferredSize(new Dimension(20, 20));
      colorModel.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          String str = (String) evt.getNewValue();
          Color c = parseColor(str);
          if (!toHex(c).equals(str)) {
            ColorButton.this.colorModel.setValue(toHex(c));
          }
          setBackground(c);
        }
      });
      this.addMouseListener(new MouseAdapter() {

        @Override
        public void mouseClicked(MouseEvent e) {
          onClick();
        }
      });
    }

    public void onClick() {
      Color color = JColorChooser.showDialog(AppUtils.getRootFrame(), "Playlistfarbe", this.getBackground());
      if (color != null) {
        String hex = toHex(color);
        colorModel.setValue(hex);
      }
    }

    private String toHex(Color color) {
      String hex = "#" + pad(Integer.toHexString(color.getRed())) + pad(Integer.toHexString(color.getGreen())) + pad(Integer.toHexString(color.getBlue()));
      return hex;
    }

    private String pad(String hex) {
      return hex.length() == 2 ? hex : "0" + hex;
    }

  }

}
