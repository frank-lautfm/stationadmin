package de.stationadmin.gui.settings;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Vector;

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
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

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

import de.stationadmin.base.Autosynchronisation;
import de.stationadmin.base.Settings;
import de.stationadmin.base.backup.BackupFrequency;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.HintLabel;
import de.stationadmin.gui.util.Option;
import de.stationadmin.gui.util.SwingTools;
import de.stationadmin.lfm.backend.LautfmAdminService;

@SuppressWarnings("rawtypes")
public class SettingsDlg extends JDialog {
  private static final long serialVersionUID = -1412608764831045129L;
  private ClientContext ctx;
  private PresentationModel<Settings> model;

  public SettingsDlg(ClientContext ctx) {
    this.ctx = ctx;
    this.model = new PresentationModel<Settings>(ctx.getAdminClient().getSettings());
    this.init();
  }

  private void init() {

    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref:grow,8dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();
    
    JTabbedPane tabPane = new JTabbedPane();
    this.initTabs(tabPane);

    this.getContentPane().add(tabPane, cc.xy(2, 2, CellConstraints.FILL, CellConstraints.FILL));

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

    this.getContentPane().add(buttonPanel, cc.xy(2, 4, CellConstraints.CENTER, CellConstraints.FILL));

    // Dimension pref = this.getContentPane().getPreferredSize();
    this.setSize(650, 500);
    this.setTitle(ctx.getTextProvider().getString("settings.title"));
    SwingTools.centerOnScreen(this);

  }

  private void initTabs(JTabbedPane tabPane) {
    CellConstraints cc = new CellConstraints();
    // general
    {
      JPanel logPanel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "5dlu,pref,5dlu,pref,5dlu,pref,5dlu,pref,5dlu,pref,5dlu"));
      logPanel.add(this.createLookAndFeelPanel(), cc.xy(2, 2));
      logPanel.add(this.createAutosynchronizePanel(), cc.xy(2, 4));
      logPanel.add(this.createClientConfigurationPanel(), cc.xy(2, 6));
      logPanel.add(this.createAutologinPanel(), cc.xy(2, 8));
      logPanel.add(this.createListenerStatsPanel(), cc.xy(2, 10));
      tabPane.addTab(ctx.getTextProvider().getString("settings.tab.general"), logPanel);
    }

    // Backup
    {
      JPanel backupPanel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "5dlu,pref,5dlu"));
      backupPanel.add(this.createBackupPanel(), cc.xy(2, 2));
      tabPane.addTab(ctx.getTextProvider().getString("settings.tab.backup"), backupPanel);
    }

    // MP3
    {
      JPanel logPanel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "5dlu,pref,5dlu,pref,5dlu"));
      logPanel.add(this.createMP3PlayerPanel(), cc.xy(2, 2));
      logPanel.add(this.createMP3RootPanel(), cc.xy(2, 4));
      tabPane.addTab(ctx.getTextProvider().getString("settings.tab.mp3"), logPanel);
    }
    
    // debug
    {
      JPanel logPanel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "5dlu,pref,5dlu"));
      logPanel.add(this.createLoggingPanel(), cc.xy(2, 2));
      tabPane.addTab(ctx.getTextProvider().getString("settings.tab.misc"), logPanel);
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
        if (cb.isSelected()) {
          Logger.getLogger(LautfmAdminService.class).setLevel(Level.INFO);
        } else {
          Logger.getLogger(LautfmAdminService.class).setLevel(Level.ERROR);
        }
      }
    });

    panel.add(cb, new CellConstraints(2, 2));
    return panel;

  }

  private JPanel createAutologinPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "3dlu,pref,3dlu"));
    panel.setBorder(BorderFactory.createTitledBorder(ctx.getTextProvider().getString("settings.section.autologin")));

    JCheckBox updateCheckCb = BasicComponentFactory.createCheckBox(model.getBufferedModel("autologin"), this.ctx.getTextProvider().getString("settings.property.autologin"));
    panel.add(updateCheckCb, new CellConstraints(2, 2));

    return panel;
  }

  private JPanel createLookAndFeelPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "3dlu,pref,5dlu,pref,3dlu"));
    panel.setBorder(BorderFactory.createTitledBorder(ctx.getTextProvider().getString("settings.section.laf")));

    Vector<Option> options = new Vector<>();
    options.add(new Option("system", "System"));

    options.add(new Option("com.jtattoo.plaf.acryl.AcrylLookAndFeel", "Acryl"));
    options.add(new Option("com.jtattoo.plaf.aero.AeroLookAndFeel", "Aero"));
    options.add(new Option("com.jtattoo.plaf.aluminium.AluminiumLookAndFeel", "Aluminium"));
    options.add(new Option("com.jtattoo.plaf.hifi.HiFiLookAndFeel", "Hifi"));
    options.add(new Option("com.jtattoo.plaf.luna.LunaLookAndFeel", "Luna"));
    options.add(new Option("com.jtattoo.plaf.mcwin.McWinLookAndFeel", "McWin"));
    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
      if ("Nimbus".equals(info.getName())) {
        options.add(new Option(info.getClassName(), "Nimbus"));
        break;
      }
    }
    options.add(new Option("com.jtattoo.plaf.noire.NoireLookAndFeel", "Noire"));

    JComboBox<Option> cmb = new JComboBox<>(options);
    String current = model.getBean().getLookAndFeel();
    if (current != null) {
      for (Option opt : options) {
        if (opt.getKey().equals(current)) {
          cmb.setSelectedItem(opt);
          break;
        }
      }
    }

    cmb.addItemListener(new ItemListener() {

      @Override
      public void itemStateChanged(ItemEvent e) {
        Option opt = (Option) cmb.getSelectedItem();
        model.getBean().setLookAndFeel((String) opt.getKey());
      }
    });

    panel.add(cmb, new CellConstraints(2, 2));

    JLabel hint = new HintLabel(ctx.getString("settings.laf.hint"));

    panel.add(hint, new CellConstraints(2, 4));

    return panel;
  }
  
  private JPanel createClientConfigurationPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,pref,5dlu,pref,3dlu", "3dlu,pref,3dlu"));
    panel.setBorder(BorderFactory.createTitledBorder(ctx.getTextProvider().getString("settings.section.clientsettings")));
    
    JCheckBox clientSettingsCb = BasicComponentFactory.createCheckBox(model.getBufferedModel("saveClientSettings"), this.ctx.getTextProvider().getString("settings.property.saveClientSettings"));
    panel.add(clientSettingsCb, new CellConstraints(2, 2));

    
    return panel;
  }


  private JPanel createAutosynchronizePanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,pref,5dlu,pref,3dlu", "3dlu,pref,3dlu"));
    panel.setBorder(BorderFactory.createTitledBorder(ctx.getTextProvider().getString("settings.section.autosynchronisation")));

    panel.add(new JLabel(ctx.getString("settings.property.autosynchronisation")), new CellConstraints(2, 2));

    SelectionInList<Autosynchronisation> selection = new SelectionInList<Autosynchronisation>(Autosynchronisation.values(), model.getBufferedModel("autoSynchronisation"));
    JComboBox cmb = BasicComponentFactory.createComboBox(selection, new DefaultListCellRenderer() {
      private static final long serialVersionUID = -7509561259749019548L;

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Autosynchronisation) {
          String name = ((Autosynchronisation) value).name().toLowerCase();
          setText(ctx.getTextProvider().getString("settings.property.autosynchronisation." + name));
        }
        return comp;
      }

    });
    panel.add(cmb, new CellConstraints(4, 2));

    return panel;
  }

  private JPanel createBackupPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,max(pref;60dlu),5dlu,pref,3dlu,pref,3dlu", "3dlu,pref,3dlu,pref,3dlu"));
    panel.setBorder(BorderFactory.createTitledBorder(ctx.getTextProvider().getString("settings.section.backup")));
    CellConstraints cc = new CellConstraints();
    int row = 2;

    {
      JTextField backupDirTf = ctx.getComponentFactory().createTextField(model.getBufferedModel("backupDirectory"));
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

    JTextField playerTf = ctx.getComponentFactory().createTextField(model.getBufferedModel("mp3Player"));
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

    JTextField playerTf = ctx.getComponentFactory().createTextField(model.getBufferedModel("mp3Root"));
    playerTf.setColumns(30);

    panel.add(new JLabel(this.ctx.getTextProvider().getString("settings.property.mp3Root")), cc.xy(2, 4));
    panel.add(playerTf, cc.xy(4, 4));

    panel.add(new JButton(new DirSelectionAction(model.getBufferedModel("mp3Root"))), cc.xy(6, 4));

    return panel;
  }


  private JPanel createListenerStatsPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,max(pref;60dlu),5dlu,pref,3dlu,pref,3dlu", "3dlu,pref,3dlu,pref,3dlu"));
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

}
