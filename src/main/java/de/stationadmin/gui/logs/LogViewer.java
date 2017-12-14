/**
 * 
 */
package de.stationadmin.gui.logs;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;
import de.stationadmin.gui.logs.LogEntryTableModel.Column;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.DateTableCellRenderer;
import de.stationadmin.lfm.backend.LogEntry;

/**
 * @author korf
 *
 */
public class LogViewer extends StationAdminFrame {
  private static final long serialVersionUID = -3995940982166369199L;
  
  private ValueHolder days = new ValueHolder(1);
  private ValueHolder level = new ValueHolder(LogLevel.INFO);
  private ValueHolder type = new ValueHolder();
  
  private LogEntryTableModel model;

  /**
   * @param ctx
   * @throws HeadlessException
   */
  public LogViewer(ClientContext ctx) throws HeadlessException {
    super(ctx, "logviewer");
    this.initialize();
  }

  private void initialize() {

    model = new LogEntryTableModel(ctx.getTextProvider());
    this.setTitle(ctx.getTextProvider().getString("logviewer.title"));
    
    JXTable table = new JXTable(model);
    table.setSortable(true);
    
    int dateWidth = ComponentFactory.getTableColumnWidthDateTine();
    int levelWidth = ComponentFactory.getTableFontWidth(10);
    int typeWidth = ComponentFactory.getTableFontWidth(15);
    table.getColumnModel().getColumn(Column.CREATED_AT.ordinal()).setCellRenderer(new DateTableCellRenderer(new SimpleDateFormat(ctx.getTextProvider().getString("timeFormat"))));
    table.getColumnModel().getColumn(Column.CREATED_AT.ordinal()).setPreferredWidth(dateWidth);
    table.getColumnModel().getColumn(Column.CREATED_AT.ordinal()).setMaxWidth(dateWidth);
    table.getColumnModel().getColumn(Column.LEVEL.ordinal()).setPreferredWidth(levelWidth);
    table.getColumnModel().getColumn(Column.LEVEL.ordinal()).setMaxWidth(levelWidth);
    table.getColumnModel().getColumn(Column.TYPE.ordinal()).setPreferredWidth(typeWidth);
    table.getColumnModel().getColumn(Column.TYPE.ordinal()).setMaxWidth(typeWidth);
    
    this.getContentPane().setLayout(new FormLayout("3dlu,pref:grow,3dlu","3dlu,pref,2dlu,pref:grow,3dlu"));
    CellConstraints cc = new CellConstraints();
    this.getContentPane().add(createFilterPanel(), cc.xy(2, 2));
    this.getContentPane().add(new JScrollPane(table), cc.xy(2, 4));
  }
  
  @SuppressWarnings("rawtypes")
  private JPanel createFilterPanel() {
    
    JPanel panel = new JPanel(new FormLayout("pref,2dlu,pref,2dlu,pref,8dlu,pref,3dlu,pref,8dlu,pref,3dlu,pref,8dlu:grow,pref", "pref"));
    CellConstraints cc = new CellConstraints();
    
    int col = 1;
    
    // Time
    panel.add(new JLabel(ctx.getString("logviewer.column.created_at.before")), cc.xy(col, 1)); 
    col+=2;
    SelectionInList<Integer> daysSelection = new SelectionInList<Integer>(new Integer[] { 1, 2, 3, 4, 5, 6, 7 }, days);
    JComboBox daysCmb = BasicComponentFactory.createComboBox(daysSelection);
    panel.add(daysCmb, cc.xy(col, 1)); 
    col+=2;
    panel.add(new JLabel(ctx.getString("logviewer.column.created_at.unit")), cc.xy(col, 1)); 
    col+=2;
    
    // Level
    panel.add(new JLabel(ctx.getString("logviewer.column.level")), cc.xy(col, 1)); 
    col+=2;
    SelectionInList<LogLevel> levelSelection = new SelectionInList<LogLevel>(LogLevel.values(), level);
    JComboBox levelCmb = BasicComponentFactory.createComboBox(levelSelection, new DefaultListCellRenderer() {
      private static final long serialVersionUID = 3817876613492656987L;

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if(value instanceof LogLevel) {
          setText(ctx.getTextProvider().getString("logviewer.column.level." + ((LogLevel)value).name().toLowerCase()));
        }
        return comp;
      }
      
    });
    panel.add(levelCmb, cc.xy(col, 1)); 
    col+=2;

    panel.add(new JLabel(ctx.getString("logviewer.column.type")), cc.xy(col, 1)); 
    col+=2;
    SelectionInList<String> typeSelection = new SelectionInList<String>(new String[] { null, "station", "playlist", "schedule", "mp3" }, type);
    JComboBox typeCmb = BasicComponentFactory.createComboBox(typeSelection);
    panel.add(typeCmb, cc.xy(col, 1)); 
    col+=2;

    
    panel.add(new JButton(new SearchAction()), cc.xy(col, 1));

    return panel;
  }
  
  void search() {
    int days = (Integer)this.days.getValue();
    try {
      LogEntry[] entries = ctx.getAdminClient().getLogs(days);
      List<LogEntry> filtered = new ArrayList<LogEntry>();
      LogLevel minLevel = (LogLevel)this.level.getValue();
      String type = (String)this.type.getValue();
      for(LogEntry entry : entries) {
        boolean accept = true;
        
        LogLevel level = LogLevel.fromInternal(entry.getLevel());
        accept = accept && (level == null  || level.ordinal() >= minLevel.ordinal());
        accept = accept && (type == null || entry.getType().equals(type));
        if(accept) {
          filtered.add(entry);
        }
        
      }      
      model.setEntries(filtered.toArray(new LogEntry[filtered.size()]));
    } catch (IOException e) {
      JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "logviewer.error"));

    }
    
  }

  @Override
  protected Dimension getDefaultSize() {
    return new Dimension(700, 500);
  }
  
  class SearchAction extends AbstractAction {
    private static final long serialVersionUID = -8897492525426333713L;

    SearchAction() {
      this.putValue(Action.SMALL_ICON, AppUtils.getIcon("searching.png"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      search();
      
    }
    
  }

}
