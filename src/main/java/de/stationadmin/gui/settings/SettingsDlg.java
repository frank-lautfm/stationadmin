package de.stationadmin.gui.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXLabel;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.Settings;
import de.stationadmin.base.backup.BackupFrequency;
import de.stationadmin.base.playlist.shuffle.WordDistributionStrategy;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.SwingTools;
import de.stationadmin.lfm.backend.LautfmAdminService;


@SuppressWarnings("rawtypes")
public class SettingsDlg extends JDialog {
  private static final long serialVersionUID = -1412608764831045129L;
  private ClientContext ctx;
  private PresentationModel<Settings> model;
  private JPanel container = new JPanel(new BorderLayout());

  public SettingsDlg(ClientContext ctx) {
    this.ctx = ctx;
    this.model = new PresentationModel<Settings>(ctx.getAdminClient().getSettings());
    this.init();
  }

  private void init() {

    this.getContentPane().setLayout(new FormLayout("5dlu,130dlu,5dlu,pref:grow,5dlu", "5dlu,pref:grow,8dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    DefaultMutableTreeNode root = new DefaultMutableTreeNode(new PanelSelection("Einstellungen", null));
    this.initTreeModel(root);
    final JTree tree = new JTree(new DefaultTreeModel(root));
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

    this.getContentPane().add(new JScrollPane(tree), cc.xy(2, 2, CellConstraints.FILL, CellConstraints.FILL));
    this.getContentPane().add(this.container, cc.xy(4, 2, CellConstraints.FILL, CellConstraints.FILL));


    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 10));
    JButton ok = new JButton("Ok");
    ok.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent evt) {
        model.triggerCommit();
        try {
          ctx.getAdminClient().saveSettings();
          dispose();
        } catch (IOException e) {
          JXErrorPane.showDialog(e);
        }
      }

    });
    JButton cancel = new JButton(new DisposeAction(this, ctx.getTextProvider().getString("cancel")));
    buttonPanel.add(ok);
    buttonPanel.add(cancel);

    this.getContentPane().add(buttonPanel, cc.xywh(2, 4, 3, 1, CellConstraints.CENTER, CellConstraints.FILL));

    // Dimension pref = this.getContentPane().getPreferredSize();
    this.setSize(730, 450);
    this.setTitle(ctx.getTextProvider().getString("settings.title"));
    SwingTools.centerOnScreen(this);
    
    tree.setSelectionRow(1);

  }

  private void initTreeModel(DefaultMutableTreeNode root) {
    CellConstraints cc = new CellConstraints();
    // logging
    {
      JPanel logPanel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "5dlu,pref,5dlu,pref,5dlu"));
      logPanel.add(this.createListenerStatsPanel(), cc.xy(2, 2));
      logPanel.add(this.createTitleLogPanel(), cc.xy(2, 4));
      root.add(new DefaultMutableTreeNode(new PanelSelection(ctx.getTextProvider().getString("settings.tab.logging"), logPanel)));
    }

    // Playlists
    {
      JPanel logPanel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "5dlu,pref,5dlu"));
      logPanel.add(this.createShufflePanel(), cc.xy(2, 2));
      
      DefaultMutableTreeNode playlists = new DefaultMutableTreeNode(new PanelSelection(ctx.getTextProvider().getString("settings.tab.playlists"), logPanel));
      playlists.add(new DefaultMutableTreeNode(new PanelSelection(ctx.getTextProvider().getString("settings.tab.playlists.normalize"), this.createArtistNormalizePanel())));
      playlists.add(new DefaultMutableTreeNode(new PanelSelection(ctx.getTextProvider().getString("settings.tab.playlists.weights"), this.createGenerateWeightsPanel())));
      playlists.add(new DefaultMutableTreeNode(new PanelSelection(ctx.getTextProvider().getString("settings.tab.playlists.preselect"), this.createGenerateArtistPreselectPanel())));
      
      root.add(playlists);
    }
    
    // Backup
    {
      JPanel backupPanel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "5dlu,pref,5dlu"));
      backupPanel.add(this.createBackupPanel(), cc.xy(2, 2));
      root.add(new DefaultMutableTreeNode(new PanelSelection(ctx.getTextProvider().getString("settings.tab.backup"), backupPanel)));
    }

    // MP3
    {
      JPanel logPanel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "5dlu,pref,5dlu,pref,5dlu"));
      logPanel.add(this.createMP3PlayerPanel(), cc.xy(2, 2));
      logPanel.add(this.createMP3RootPanel(), cc.xy(2, 4));
      root.add(new DefaultMutableTreeNode(new PanelSelection(ctx.getTextProvider().getString("settings.tab.mp3"), logPanel)));
    }

    // misc
    {
      JPanel logPanel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "5dlu,pref,5dlu,pref,5dlu"));
      logPanel.add(this.createAutologinPanel(), cc.xy(2, 2));
      logPanel.add(this.createLoggingPanel(), cc.xy(2, 4));
      // logPanel.add(this.createLogDownloadPanel(), cc.xy(2, 4));
      root.add(new DefaultMutableTreeNode(new PanelSelection(ctx.getTextProvider().getString("settings.tab.misc"), logPanel)));
    }

  }
  
  private JPanel createLoggingPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "3dlu,pref,3dlu"));
    panel.setBorder(BorderFactory.createTitledBorder("Logging"));
    
    final JCheckBox cb = new JCheckBox("Netzwerkverkehr für diese Sitzung loggen");
    cb.setSelected(Logger.getLogger(LautfmAdminService.class).getLevel().equals(Level.INFO));
    cb.addActionListener(new ActionListener() {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        if(cb.isSelected()) {
          Logger.getLogger(LautfmAdminService.class).setLevel(Level.INFO);
        }
        else {
          Logger.getLogger(LautfmAdminService.class).setLevel(Level.ERROR);
        }
      }
    });
    
    panel.add(cb, new CellConstraints(2,2));
    return panel;
    
  }

  private JPanel createAutologinPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "3dlu,pref,3dlu"));
    panel.setBorder(BorderFactory.createTitledBorder(ctx.getTextProvider().getString("settings.section.autologin")));

    JCheckBox updateCheckCb = BasicComponentFactory.createCheckBox(model.getBufferedModel("autologin"),
        this.ctx.getTextProvider().getString("settings.property.autologin"));
    panel.add(updateCheckCb, new CellConstraints(2, 2));

    return panel;
  }

  private JPanel createTitleLogPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,max(pref;60dlu),5dlu,pref,3dlu,pref,3dlu", "3dlu,pref,3dlu,pref,3dlu"));
    panel.setBorder(BorderFactory.createTitledBorder(ctx.getTextProvider().getString("settings.section.title")));
    CellConstraints cc = new CellConstraints();
    int row = 2;

    JTextField titleLogTf = BasicComponentFactory.createTextField(model.getBufferedModel("titleLogFile"));
    titleLogTf.setColumns(30);

    panel.add(new JLabel(this.ctx.getTextProvider().getString("settings.property.titlelogdir")), cc.xy(2, row));
    panel.add(titleLogTf, cc.xy(4, row));
    String titleLogDefault = ctx.getAdminClient().getStation() + "-titles-%day%.log";
    panel.add(new JButton(new FileSelectionAction(model.getBufferedModel("titleLogFile"), titleLogDefault)), cc.xy(6, row));
    row += 2;

    JCheckBox logListenersCb = BasicComponentFactory.createCheckBox(model.getBufferedModel("logTitleWithListeners"),
        ctx.getTextProvider().getString("settings.property.logTitleWithListeners"));
    panel.add(logListenersCb, cc.xy(4, row));

    return panel;
  }

  private JPanel createBackupPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,max(pref;60dlu),5dlu,pref,3dlu,pref,3dlu", "3dlu,pref,3dlu,pref,3dlu"));
    panel.setBorder(BorderFactory.createTitledBorder(ctx.getTextProvider().getString("settings.section.backup")));
    CellConstraints cc = new CellConstraints();
    int row = 2;

    {
      JTextField backupDirTf = BasicComponentFactory.createTextField(model.getBufferedModel("backupDirectory"));
      backupDirTf.setColumns(30);

      panel.add(new JLabel(this.ctx.getTextProvider().getString("settings.property.backupDirectory")), cc.xy(2, row));
      panel.add(backupDirTf, cc.xy(4, row));
      panel.add(new JButton(new DirSelectionAction(model.getBufferedModel("backupDirectory"))), cc.xy(6, row));

      row += 2;
    }

    {
      Integer[] options = new Integer[BackupFrequency.values().length];
      for (int i = 0; i < options.length; i++) {
        options[i] = i;
      }
      SelectionInList<Integer> selection = new SelectionInList<Integer>(options, this.model.getBufferedModel("backupFrequency"));
      JComboBox cmb = BasicComponentFactory.createComboBox(selection, new DefaultListCellRenderer() {
        private static final long serialVersionUID = -318769036621335552L;

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          BackupFrequency option = BackupFrequency.values()[(Integer) value];
          setText(ctx.getTextProvider().getString("settings.property.backupFrequency." + option.name().toLowerCase()));
          return this;
        }

      });
      panel.add(new JLabel(this.ctx.getTextProvider().getString("settings.property.backupFrequency")), cc.xy(2, row));
      panel.add(cmb, cc.xywh(4, row, 3, 1));

    }

    return panel;

  }

  /**
   * Creates a panel for editing the path to an external mp3 player
   * 
   * @return
   */
  private JPanel createMP3PlayerPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,max(pref;60dlu),5dlu,pref,3dlu,pref,3dlu", "3dlu,pref,5dlu,pref,3dlu"));
    panel.setBorder(BorderFactory.createTitledBorder(ctx.getTextProvider().getString("settings.section.mp3player")));
    CellConstraints cc = new CellConstraints();

    JXLabel desc = new JXLabel(ctx.getTextProvider().getString("settings.property.mp3player.description"));
    desc.setLineWrap(true);
    panel.add(desc, cc.xywh(2, 2, 5, 1));

    JTextField playerTf = BasicComponentFactory.createTextField(model.getBufferedModel("mp3Player"));
    playerTf.setColumns(30);

    panel.add(new JLabel(this.ctx.getTextProvider().getString("settings.property.mp3player")), cc.xy(2, 4));
    panel.add(playerTf, cc.xy(4, 4));

    panel.add(new JButton(new FileSelectionAction(model.getBufferedModel("mp3Player"), null)), cc.xy(6, 4));

    return panel;
  }

  private JPanel createMP3RootPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,max(pref;60dlu),5dlu,pref,3dlu,pref,3dlu", "3dlu,pref,5dlu,pref,3dlu,pref,3dlu"));
    panel.setBorder(BorderFactory.createTitledBorder(ctx.getTextProvider().getString("settings.section.mp3root")));
    CellConstraints cc = new CellConstraints();

    JXLabel desc = new JXLabel(ctx.getTextProvider().getString("settings.property.mp3Root.description"));
    desc.setLineWrap(true);
    panel.add(desc, cc.xywh(2, 2, 5, 1));

    JTextField playerTf = BasicComponentFactory.createTextField(model.getBufferedModel("mp3Root"));
    playerTf.setColumns(30);

    panel.add(new JLabel(this.ctx.getTextProvider().getString("settings.property.mp3Root")), cc.xy(2, 4));
    panel.add(playerTf, cc.xy(4, 4));

    panel.add(new JButton(new DirSelectionAction(model.getBufferedModel("mp3Root"))), cc.xy(6, 4));

    return panel;
  }

  private JPanel createShufflePanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,60dlu,5dlu,pref,2dlu,pref,pref,pref:grow,3dlu",
        "3dlu,pref,3dlu,pref,3dlu,pref,7dlu,pref,3dlu,pref,5dlu,pref,3dlu,pref,5dlu,pref,3dlu"));
    panel.setBorder(BorderFactory.createTitledBorder(ctx.getTextProvider().getString("settings.section.shuffle")));
    CellConstraints cc = new CellConstraints();

    final ValueHolder jingleIntervalEnable = new ValueHolder(((Integer) this.model.getValue("shuffleJingleInterval")).intValue() > 0);
    jingleIntervalEnable.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (((Boolean) evt.getNewValue()).booleanValue() == false) {
          model.getBufferedModel("shuffleJingleInterval").setValue(0);
        }
      }

    });

    this.model.getBufferedModel("shuffleJingleInterval").addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        int value = (Integer) evt.getNewValue();
        jingleIntervalEnable.setValue(value > 0);
      }

    });

    JCheckBox protect1stCb = BasicComponentFactory.createCheckBox(this.model.getBufferedModel("shuffleProtectFirstJingle"), null);
    JCheckBox jingleIntervalEnableCb = BasicComponentFactory.createCheckBox(jingleIntervalEnable, null);
    JTextField jingleIntervalTf = BasicComponentFactory.createIntegerField(this.model.getBufferedModel("shuffleJingleInterval"), 0);
    jingleIntervalTf.setColumns(3);

    panel.add(protect1stCb, cc.xy(4, 2));
    panel.add(new JLabel(ctx.getTextProvider().getString("settings.shuffle.protectFirstJingle")), cc.xywh(6, 2, 3, 1));

    panel.add(jingleIntervalEnableCb, cc.xy(4, 4));
    panel.add(new JLabel(ctx.getTextProvider().getString("settings.shuffle.interval.every") + " "), cc.xy(6, 4));
    panel.add(jingleIntervalTf, cc.xy(7, 4));
    panel.add(new JLabel(" " + ctx.getTextProvider().getString("settings.shuffle.interval.minute")), cc.xy(8, 4));

    SelectionInList<WordDistributionStrategy> wordDistSelection = new SelectionInList<WordDistributionStrategy>(WordDistributionStrategy.values(),
        this.model.getBufferedModel("shuffleWordDistributionStrategy"));
    JComboBox wordDistCmb = BasicComponentFactory.createComboBox(wordDistSelection, new DefaultListCellRenderer() {
      private static final long serialVersionUID = 7985870900294296891L;

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        String val = ((WordDistributionStrategy) value).name().toLowerCase();
        setText(ctx.getTextProvider().getString("settings.property.shuffleWordDistribution." + val));
        return this;
      }
    });
    panel.add(new JLabel(this.ctx.getTextProvider().getString("settings.property.shuffleWordDistribution")), cc.xy(2, 6));
    panel.add(wordDistCmb, cc.xywh(4, 6, 5, 1));

    //
    // ValueHolder preselectLimit = new ValueHolder(0);
    //
    // JPanel preselectProperyPanel = new JPanel(new
    // FormLayout("pref,2dlu,pref,2dlu,pref,8dlu:grow:pref","pref"));
    // JTextField preselectLimitTf =
    // BasicComponentFactory.createIntegerField(preselectLimit, 0);
    // preselectLimitTf.setColumns(2);
    // preselectProperyPanel.add(new
    // JLabel(this.ctx.getTextProvider().getString("settings.property.generatePreselect.prefix")),
    // cc.xy(1,1));
    // preselectProperyPanel.add(preselectLimitTf, cc.xy(3,1));
    // preselectProperyPanel.add(new
    // JLabel(this.ctx.getTextProvider().getString("settings.property.generatePreselect.suffix")),
    // cc.xy(5,1));
    // panel.add(preselectProperyPanel, cc.xywh(2, 16, 7, 1));

    return panel;

  }
  
  private JPanel createGenerateArtistPreselectPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "3dlu,pref:grow,3dlu"));
    panel.setBorder(BorderFactory.createTitledBorder(ctx.getTextProvider().getString("settings.section.gen.preselect")));
    CellConstraints cc = new CellConstraints();
    
    panel.add(new ArtistLimitPanel(ctx, model), cc.xy(2,2, CellConstraints.FILL, CellConstraints.FILL));
    
    return panel;
  }
  
  private JPanel createArtistNormalizePanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "3dlu,pref:grow,3dlu"));
    panel.setBorder(BorderFactory.createTitledBorder(ctx.getTextProvider().getString("settings.section.gen.normalize")));
    panel.add(new ArtistNormalizePanel(ctx, model), new CellConstraints(2, 2, CellConstraints.FILL, CellConstraints.FILL));
    
    return panel;
  }

  private JPanel createGenerateWeightsPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "3dlu,pref,3dlu,pref,8dlu,pref,5dlu,pref:grow,3dlu"));
    panel.setBorder(BorderFactory.createTitledBorder(ctx.getTextProvider().getString("settings.section.gen.weight")));
    CellConstraints cc = new CellConstraints();

    final ValueModel minRandomValueModel = model.getBufferedModel("generateMinRandomValue");
    final JSlider minRandomValueSlider = new JSlider(0, 500);
    minRandomValueSlider.setMinorTickSpacing(25);
    minRandomValueSlider.setMajorTickSpacing(100);
    minRandomValueSlider.setSnapToTicks(true);
    minRandomValueSlider.setPaintTicks(true);
    minRandomValueSlider.setValue((Integer) minRandomValueModel.getValue());
    minRandomValueSlider.addChangeListener(new ChangeListener() {

      @Override
      public void stateChanged(ChangeEvent e) {
        int value = minRandomValueSlider.getValue();
        minRandomValueModel.setValue(value);
      }
    });

    panel.add(new JLabel(this.ctx.getTextProvider().getString("settings.property.generateMinRandomValue")), cc.xy(2, 2));
    panel.add(minRandomValueSlider, cc.xy(2, 4));

    panel.add(new JLabel(this.ctx.getTextProvider().getString("settings.property.generateWeightTags")), cc.xy(2, 6));
    panel.add(new TagWeightPanel(ctx, this.model), cc.xy(2, 8, CellConstraints.FILL, CellConstraints.FILL));

    return panel;
  }

  private JPanel createListenerStatsPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,max(pref;60dlu),5dlu,pref,3dlu,pref,3dlu", "3dlu,pref,3dlu,pref,5dlu,pref,5dlu,pref,3dlu"));
    panel.setBorder(BorderFactory.createTitledBorder(ctx.getTextProvider().getString("settings.section.stats")));
    CellConstraints cc = new CellConstraints();
    int row = 2;

    NumberFormat fmt = NumberFormat.getIntegerInstance();
    fmt.setGroupingUsed(false);

    ValueModel onTitleRefresh = new ValueHolder(model.getBean().getStatisticsRefreshInterval() == -1);
    JRadioButton intervalBtn = BasicComponentFactory.createRadioButton(onTitleRefresh, Boolean.FALSE, null);
    JRadioButton titleRefrshBtn = BasicComponentFactory.createRadioButton(onTitleRefresh, Boolean.TRUE, null);
    onTitleRefresh.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue().equals(Boolean.TRUE)) {
          model.getBufferedModel("statisticsRefreshInterval").setValue(-1);
        }
      }

    });

    JTextField statsRefreshTf = BasicComponentFactory.createIntegerField(model.getBufferedModel("statisticsRefreshInterval"), fmt, -1);
    statsRefreshTf.setColumns(3);

    JTextField statsLogTf = BasicComponentFactory.createTextField(model.getBufferedModel("statisticsLogFile"));
    statsLogTf.setColumns(30);

    panel.add(new JLabel(this.ctx.getTextProvider().getString("settings.property.statsrefresh")), cc.xy(2, row));
    JPanel refresh = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    refresh.add(intervalBtn);
    refresh.add(new JLabel(ctx.getTextProvider().getString("settings.property.statsrefresh.every")));
    refresh.add(statsRefreshTf);
    refresh.add(new JLabel(ctx.getTextProvider().getString("settings.property.statsrefresh.unit")));

    JPanel refresh2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    refresh2.add(titleRefrshBtn);
    refresh2.add(new JLabel(ctx.getTextProvider().getString("settings.property.statsrefresh.onTitle")));

    panel.add(refresh, cc.xy(4, row));
    row += 2;
    panel.add(refresh2, cc.xy(4, row));
    row += 2;

    panel.add(new JLabel(this.ctx.getTextProvider().getString("settings.property.statslogdir")), cc.xy(2, row));
    panel.add(statsLogTf, cc.xy(4, row));
    String statsLogDefault = ctx.getAdminClient().getStation() + "-listeners-%day%.log";
    panel.add(new JButton(new FileSelectionAction(model.getBufferedModel("statisticsLogFile"), statsLogDefault)), cc.xy(6, row));
    row += 2;

    JCheckBox logRankCb = BasicComponentFactory.createCheckBox(model.getBufferedModel("logRank"),
        ctx.getTextProvider().getString("settings.property.logRank"));
    panel.add(logRankCb, cc.xy(4, row));

    return panel;
  }

  private static class FileSelectionAction extends AbstractAction {
    private static final long serialVersionUID = -2063331600015348910L;
    private ValueModel model;
    private String defaultName;

    FileSelectionAction(ValueModel model, String defaultName) {
      this.putValue(Action.NAME, "...");
      this.model = model;
      this.defaultName = defaultName;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      JFileChooser fileChooser = new JFileChooser();
      String value = (String) model.getValue();
      if (value != null && value.length() > 0) {
        File file = new File(value);
        fileChooser.setCurrentDirectory(file.getParentFile());
        fileChooser.setSelectedFile(file);
      } else if (this.defaultName != null) {
        File file = new File(fileChooser.getCurrentDirectory().getAbsolutePath() + File.separatorChar + defaultName);
        fileChooser.setCurrentDirectory(file.getParentFile());
        fileChooser.setSelectedFile(file);

      }
      if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        if (fileChooser.getSelectedFile().isDirectory()) {
          model.setValue(fileChooser.getSelectedFile().getAbsolutePath() + File.separatorChar + this.defaultName);
        } else {
          model.setValue(fileChooser.getSelectedFile().getAbsolutePath());
        }
      }

    }

  }

  private static class DirSelectionAction extends AbstractAction {
    private static final long serialVersionUID = -2063331600015348910L;
    private ValueModel model;

    DirSelectionAction(ValueModel model) {
      this.putValue(Action.NAME, "...");
      this.model = model;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      String value = (String) model.getValue();
      if (value != null && value.length() > 0) {
        File file = new File(value);
        fileChooser.setCurrentDirectory(file.getParentFile());
        fileChooser.setSelectedFile(file);
      }
      if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        if (fileChooser.getSelectedFile().isDirectory()) {
          model.setValue(fileChooser.getSelectedFile().getAbsolutePath());
        } else {
          Toolkit.getDefaultToolkit().beep();
        }
      }

    }

  }

  private static class PanelSelection {
    private String label;
    private JPanel panel;

    /**
     * @param label
     * @param panel
     */
    PanelSelection(String label, JPanel panel) {
      super();
      this.label = label;
      this.panel = panel;
    }

    public String toString() {
      return this.label;
    }

    /**
     * @return the label
     */
    public String getLabel() {
      return label;
    }

    /**
     * @return the panel
     */
    public JPanel getPanel() {
      return panel;
    }
  }
}
