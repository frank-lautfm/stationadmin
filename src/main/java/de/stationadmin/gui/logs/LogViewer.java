/**
 * 
 */
package de.stationadmin.gui.logs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.io.IOException;
import java.text.SimpleDateFormat;

import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXTable;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;
import de.stationadmin.gui.logs.LogEntryTableModel.Column;
import de.stationadmin.gui.util.DateTableCellRenderer;

/**
 * @author korf
 *
 */
public class LogViewer extends StationAdminFrame {
  private static final long serialVersionUID = -3995940982166369199L;

  /**
   * @param ctx
   * @throws HeadlessException
   */
  public LogViewer(ClientContext ctx) throws HeadlessException {
    super(ctx, "logviewer");
    this.initialize();
  }

  private void initialize() {

    LogEntryTableModel model = new LogEntryTableModel(ctx.getTextProvider());
    this.setTitle(ctx.getTextProvider().getString("logviewer.title"));
    
    JXTable table = new JXTable(model);
    table.setSortable(true);
    
    table.getColumnModel().getColumn(Column.CREATED_AT.ordinal()).setCellRenderer(new DateTableCellRenderer(new SimpleDateFormat(ctx.getTextProvider().getString("timeFormat"))));
    table.getColumnModel().getColumn(Column.CREATED_AT.ordinal()).setPreferredWidth(110);
    table.getColumnModel().getColumn(Column.CREATED_AT.ordinal()).setMaxWidth(110);
    table.getColumnModel().getColumn(Column.LEVEL.ordinal()).setPreferredWidth(70);
    table.getColumnModel().getColumn(Column.LEVEL.ordinal()).setMaxWidth(70);
    table.getColumnModel().getColumn(Column.TYPE.ordinal()).setPreferredWidth(80);
    table.getColumnModel().getColumn(Column.TYPE.ordinal()).setMaxWidth(80);
    
    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);

    try {
      model.setEntries(ctx.getAdminClient().getLogs(1));
    } catch (IOException e) {
      JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "logviewer.error"));

    }

  }

  /* (non-Javadoc)
   * @see de.stationadmin.gui.StationAdminFrame#getDefaultSize()
   */
  @Override
  protected Dimension getDefaultSize() {
    return new Dimension(700, 500);
  }

}
