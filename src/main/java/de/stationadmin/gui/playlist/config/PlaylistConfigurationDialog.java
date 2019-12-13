/**
 * 
 */
package de.stationadmin.gui.playlist.config;

import java.awt.BorderLayout;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;
import org.jdesktop.swingx.error.ErrorLevel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.config.ClientConfigurationService;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.PlaylistService;
import de.stationadmin.base.playlist.ShuffleScriptMeta;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.playlist.PlaylistEntryJumpTarget;
import de.stationadmin.gui.playlist.config.generate.PlaylistGeneratorAdviceConfigurationPanel;
import de.stationadmin.gui.playlist.config.generate.PlaylistGeneratorBaseConfigurationPanel;
import de.stationadmin.gui.playlist.config.generate.TagSequenceEditor;
import de.stationadmin.gui.playlist.config.shuffle.BlockSelectPanel;
import de.stationadmin.gui.playlist.config.shuffle.StationAdminOptsPanel;
import de.stationadmin.gui.playlist.config.shuffle.TagPatternPanel;
import de.stationadmin.gui.playlist.config.shuffle.TagSequenceRuleEditor;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.EnumListCellRenderer;
import de.stationadmin.gui.util.PanelSelection;
import de.stationadmin.gui.util.SwingTools;

/**
 * Dialog for playlist settings
 * 
 * @author Frank Korf
 */
public class PlaylistConfigurationDialog extends JDialog {

  private static final long serialVersionUID = 3125298975653805674L;

  private static final int BASE = 0;
  private static final int AUTOFILL = 1;
  private static final int SHUFFLE_STATIONADMIN = 2;
  private static final int SHUFFLE_STATIONADMIN_TAGSEQ = 64;
  private static final int SHUFFLE_TAGPATTERN = 4;
  private static final int SHUFFLE_BLOCKSELECT = 8;
  private static final int GENERATE = 16;
  private static final int GENERATE_ADVICE = 32;

  private ClientContext ctx;
  private PlaylistService playlistService;
  private TextProvider textProvider;
  private PlaylistConfigurationModel model;
  private ClientConfigurationService clientCfgService;

  private JPanel container = new JPanel(new BorderLayout());
  private Map<Integer, PanelSelection> panels = new HashMap<Integer, PanelSelection>();
  private int nodeStatus = 0;

  public PlaylistConfigurationDialog(ClientContext ctx, PlaylistConfigurationModel model) {
    this.ctx = ctx;
    this.textProvider = ctx.getTextProvider();
    this.playlistService = ctx.getAdminClient().getPlaylistService();
    this.clientCfgService = ctx.getAdminClient().getClientConfigService();
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
    rowSpec.append("pref,5dlu,"); // profile
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

      List<ShuffleScriptMeta> shuffleFuncOptions = new ArrayList<>();
      if (model.getBean().getShuffleType() == null) {
        shuffleFuncOptions.add(null);
      }
      for (ShuffleScriptMeta script : ctx.getAdminClient().getPlaylistService().getShuffleScripts()) {
        shuffleFuncOptions.add(script);
      }

      panel.add(new JLabel(this.textProvider.getString("playlistcfg.property.shuffleFunc")), cc.xy(2, row));
      SelectionInList<ShuffleScriptMeta> shuffleFuncSelection = new SelectionInList<>(shuffleFuncOptions, model.getShuffleScript());
      final JComboBox<ShuffleScriptMeta> shuffleFuncCmb = BasicComponentFactory.createComboBox(shuffleFuncSelection, new DefaultListCellRenderer() {
        private static final long serialVersionUID = 666514488594966718L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

          if (value == null) {
            setText(ctx.getTextProvider().getString("playlistcfg.property.shuffleFunc.custom"));
          } else {
            ShuffleScriptMeta script = (ShuffleScriptMeta) value;
            String key = "playlistcfg.property.shuffleFunc." + script.getKey().toLowerCase();
            String text = ctx.getTextProvider().getString(key);
            if (!key.equals(text)) {
              setText(text);
            } else {
              setText(script.getKey());
            }
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

    // profile
    {
      SelectionInList<String> profileSelection = new SelectionInList<>(model.getProfileListModel(), model.getBufferedModel("profileId"));
      JComboBox<String> profileCmb = BasicComponentFactory.createComboBox(profileSelection, new DefaultListCellRenderer() {
        private static final long serialVersionUID = 8912776835119146763L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          if (value == null) {
            setText(" ");
          } else {
            setText(model.getProfileName((String) value));
          }
          return comp;
        }

      });

      panel.add(new JLabel(this.textProvider.getString("playlistcfg.property.profile")), cc.xy(2, row));
      panel.add(profileCmb, cc.xy(4, row));
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

  private boolean checkIfStationAdminEnabled() {
    return model.getTrackOrderType().getValue().equals(TrackOrderOption.SHUFFLE_SERVER)
        && model.getBean().getType() != PlaylistType.ARCHIVED
        && isOptsKey(ShuffleScriptMeta.STATIONADMIN);
  }

  private boolean checkIfTagPatternEnabled() {
    return model.getTrackOrderType().getValue().equals(TrackOrderOption.SHUFFLE_SERVER)
        && model.getBean().getType() != PlaylistType.ARCHIVED
        && isOptsKey(ShuffleScriptMeta.BUCKET);
  }
  
  private boolean checkIfBlockSelectEnabled() {
    return model.getTrackOrderType().getValue().equals(TrackOrderOption.SHUFFLE_SERVER)
        && model.getBean().getType() != PlaylistType.ARCHIVED
        && isOptsKey(ShuffleScriptMeta.BLOCKSELECT);
  }

  
  private boolean isOptsKey(String key) {
    return model.getShuffleScript().getValue() != null && 
        StringUtils.isNotEmpty(((ShuffleScriptMeta) model.getShuffleScript().getValue()).getOptsKey()) &&
        ((ShuffleScriptMeta) model.getShuffleScript().getValue()).getOptsKey().equals(key);
  }

  private boolean checkIfAutoFillEnabled() {
    return ((model.getTrackOrderType().getValue().equals(TrackOrderOption.SHUFFLE_SERVER) && !isOptsKey(ShuffleScriptMeta.BLOCKSELECT))
        || model.getTrackOrderType().getValue().equals(TrackOrderOption.SHUFFLE_LOCAL))
        && model.getBean().getType() != PlaylistType.ARCHIVED;
  }

  private boolean checkIfGenerateEnabled() {
    return model.getTrackOrderType().getValue().equals(TrackOrderOption.GENERATE) && model.getBean().getType() != PlaylistType.ARCHIVED;
  }

  private boolean rebuildTree(DefaultTreeModel model, DefaultMutableTreeNode root) {
    int newStatus = 0;
    if (checkIfAutoFillEnabled()) {
      newStatus |= AUTOFILL;
    }
    if (checkIfStationAdminEnabled()) {
      newStatus |= SHUFFLE_STATIONADMIN;
    }
    else if(checkIfTagPatternEnabled()) {
      newStatus |= SHUFFLE_TAGPATTERN;
    }
    else if(checkIfBlockSelectEnabled()) {
      newStatus |= SHUFFLE_BLOCKSELECT;
    }
    if (checkIfGenerateEnabled()) {
      newStatus |= GENERATE;
    }
    if (nodeStatus != newStatus) {

      for (int i = root.getChildCount() - 1; i >= 0; i--) {
        model.removeNodeFromParent((MutableTreeNode) root.getChildAt(i));
      }

      nodeStatus = newStatus;

      int index = 0;
      if ((nodeStatus & SHUFFLE_STATIONADMIN) > 0) {
        DefaultMutableTreeNode shuffleOpts = new DefaultMutableTreeNode(panels.get(SHUFFLE_STATIONADMIN));
        DefaultMutableTreeNode tagSeq = new DefaultMutableTreeNode(panels.get(SHUFFLE_STATIONADMIN_TAGSEQ));
        shuffleOpts.add(tagSeq);
        model.insertNodeInto(shuffleOpts, root, index++);
      }
      else if ((nodeStatus & SHUFFLE_TAGPATTERN) > 0) {
        DefaultMutableTreeNode shuffleOpts = new DefaultMutableTreeNode(panels.get(SHUFFLE_TAGPATTERN));
        model.insertNodeInto(shuffleOpts, root, index++);
      }
      else if ((nodeStatus & SHUFFLE_BLOCKSELECT) > 0) {
        DefaultMutableTreeNode shuffleOpts = new DefaultMutableTreeNode(panels.get(SHUFFLE_BLOCKSELECT));
        model.insertNodeInto(shuffleOpts, root, index++);
      }
      if ((nodeStatus & GENERATE) > 0) {
        DefaultMutableTreeNode generate = new DefaultMutableTreeNode(panels.get(GENERATE));
        DefaultMutableTreeNode generateAdvice = new DefaultMutableTreeNode(panels.get(GENERATE_ADVICE));
        generate.add(generateAdvice);
        model.insertNodeInto(generate, root, index++);
      }
      if ((nodeStatus & AUTOFILL) > 0) {
        DefaultMutableTreeNode autofill = new DefaultMutableTreeNode(panels.get(AUTOFILL));
        model.insertNodeInto(autofill, root, index++);
      }
      return true;
    } else {
      return false;
    }

  }

  private void init() {

    this.getContentPane().setLayout(new FormLayout("5dlu,130dlu,5dlu,pref:grow,5dlu", "1dlu,pref,0dlu,pref:grow,3dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.add(new PlaylistSettingsCopyAction(ctx, model));
    this.getContentPane().add(toolbar, cc.xy(2, 2));

    panels.put(BASE, new PanelSelection(textProvider.getString("playlistcfg.tab.base"), this.createBasePanel()));
    panels.put(SHUFFLE_STATIONADMIN, new PanelSelection(textProvider.getString("playlistcfg.tab.shuffleopts"), new StationAdminOptsPanel(ctx, model)));
    panels.put(SHUFFLE_STATIONADMIN_TAGSEQ, new PanelSelection(textProvider.getString("playlistcfg.tab.generate.advice"), new TagSequenceRuleEditor(ctx, model)));
    panels.put(SHUFFLE_TAGPATTERN, new PanelSelection(textProvider.getString("playlistcfg.tab.tagpattern"), new TagPatternPanel(ctx, model)));
    panels.put(SHUFFLE_BLOCKSELECT, new PanelSelection(textProvider.getString("playlistcfg.tab.blockselect"), new BlockSelectPanel(ctx, model)));
    panels.put(GENERATE, new PanelSelection(textProvider.getString("playlistcfg.tab.generate.base"), new PlaylistGeneratorBaseConfigurationPanel(ctx, model)));
    panels.put(GENERATE_ADVICE, new PanelSelection(textProvider.getString("playlistcfg.tab.generate.advice"), new PlaylistGeneratorAdviceConfigurationPanel(ctx, model)));
    panels.put(AUTOFILL, new PanelSelection(textProvider.getString("playlistcfg.tab.autofill"), new PlaylistAutoFillPanel(ctx, model)));

    container.add(panels.get(BASE).getPanel(), BorderLayout.CENTER);

    final DefaultMutableTreeNode root = new DefaultMutableTreeNode(panels.get(BASE));
    final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    rebuildTree(treeModel, root);
    final JTree tree = new JTree(treeModel);
    SwingTools.expandAllTreeNodes(tree, 0, tree.getRowCount());
    tree.setSelectionRow(0);

    tree.addTreeSelectionListener(new TreeSelectionListener() {

      @Override
      public void valueChanged(TreeSelectionEvent e) {
        JPanel next = null;
        TreePath path = tree.getSelectionPath();
        if (path != null) {
          PanelSelection selection = (PanelSelection) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
          next = selection.getPanel();
        }
        container.removeAll();
        if (next != null) {
          container.add(next, BorderLayout.CENTER);
        }
        validate();
        repaint();

      }
    });

    PropertyChangeListener treeRebuildListener = new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (rebuildTree(treeModel, root)) {
          SwingTools.expandAllTreeNodes(tree, 0, tree.getRowCount());
        }
      }
    };

    model.getTrackOrderType().addValueChangeListener(treeRebuildListener);
    model.getBufferedModel("shuffle").addPropertyChangeListener(treeRebuildListener);
    model.getBufferedModel("shuffleType").addPropertyChangeListener(treeRebuildListener);

    this.getContentPane().add(new JScrollPane(tree), cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));
    this.getContentPane().add(container, cc.xy(4, 4, CellConstraints.FILL, CellConstraints.FILL));

    // buttons
    {
      JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));

      JButton okBtn = new JButton(textProvider.getString("ok"));
      okBtn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent evt) {
          List<String> messages = model.validate();
          if (messages.size() > 0) {
            ErrorInfo errorInfo = new ErrorInfo(ctx.getTextProvider().getString("error.title"), messages.get(0), null, "general", null, ErrorLevel.SEVERE, null);
            JXErrorPane.showDialog(AppUtils.getRootFrame(), errorInfo);
            return;
          }

          model.triggerCommit();
          try {
            playlistService.savePlaylist(model.getBean());
            clientCfgService.write();
            dispose();

            ctx.getJumpHandler().jumpTo(new PlaylistEntryJumpTarget(model.getBean(), null));

          } catch (Exception e) {
            JXErrorPane.showFrame(PlaylistConfigurationDialog.this, textProvider.createErrorInfo(e, "playlistcfg.msg.savefailed"));
          }

        }

      });

      buttonPanel.add(okBtn);
      buttonPanel.add(new JButton(new DisposeAction(this, textProvider.getString("cancel"))));

      this.getContentPane().add(buttonPanel, cc.xywh(2, 6, 3, 1, CellConstraints.CENTER, CellConstraints.CENTER));
    }

    // Dimension prefSize = this.getPreferredSize();
    // this.setSize(Math.max(400, (int) prefSize.getWidth() + 30), (int)
    // prefSize.getHeight() + 80);
    this.setSize(650, 550);
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
