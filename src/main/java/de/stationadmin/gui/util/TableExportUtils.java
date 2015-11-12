/**
 * 
 */
package de.stationadmin.gui.util;

import java.awt.Component;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXTable;

import de.stationadmin.gui.TextProvider;

/**
 * @author korf
 * 
 */
public class TableExportUtils {

  public static void toExcelFile(TextProvider textProvider, JXTable table, File file, String sheetName, int excludedCols) throws IOException {
    
    String timeFormat = textProvider.getString("timeFormat");

    Workbook wb = new HSSFWorkbook();

    Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName(sheetName));
    CreationHelper helper = wb.getCreationHelper();

    TableModel model = table.getModel();
    Row row = sheet.createRow(0);
    int cellCnt = 0;
    for (int c = 0; c < model.getColumnCount(); c++) {
      int cc = table.convertColumnIndexToView(c);
      if (cc > -1 && (excludedCols & (1 << c)) == 0) {
        Cell cell = row.createCell(cellCnt++);
        cell.setCellValue(helper.createRichTextString(model.getColumnName(c)));
      }
    }

    for (int i = 0; i < model.getRowCount(); i++) {
      row = sheet.createRow(i + 1);
      cellCnt = 0;
      for (int c = 0; c < model.getColumnCount(); c++) {
        int cc = table.convertColumnIndexToView(c);
        if (cc > -1 && (excludedCols & (1 << c)) == 0) {
          Cell cell = row.createCell(cellCnt++);
          Object value = model.getValueAt(i, c);

          if (value != null) {
            Class<?> type = model.getColumnClass(c);
            if (Date.class.isAssignableFrom(type)) {
              cell.setCellValue((Date) value);
              
              CellStyle cellStyle = wb.createCellStyle();
              cellStyle.setDataFormat(
                  helper.createDataFormat().getFormat(timeFormat));             
              cell.setCellStyle(cellStyle);
            } else if (Integer.class.isAssignableFrom(type)) {
              cell.setCellValue((Integer) value);
            } else {
              cell.setCellValue(helper.createRichTextString(value.toString()));
            }
          }

        }
      }
    }

    for (int c = 0; c < cellCnt; c++) {
      sheet.autoSizeColumn(c);
    }

    FileOutputStream fileOut = new FileOutputStream(file);
    wb.write(fileOut);
    fileOut.close();
  }

  public static String toTabSeparatedValues(JXTable table) {
    StringBuilder buf = new StringBuilder();

    TableModel model = table.getModel();

    // header
    for (int c = 0; c < model.getColumnCount(); c++) {
      int cc = table.convertColumnIndexToView(c);
      if (cc > -1) {
        buf.append(model.getColumnName(c));
        buf.append('\t');
      }
    }
    buf.append('\n');

    // content
    for (int i = 0; i < model.getRowCount(); i++) {
      for (int c = 0; c < model.getColumnCount(); c++) {
        int cc = table.convertColumnIndexToView(c);
        if (cc > -1) {
          Object value = model.getValueAt(i, c);
          // try to use cells renderer
          Component comp = table.getColumn(cc).getCellRenderer() != null ? table.getColumn(cc).getCellRenderer()
              .getTableCellRendererComponent(table, value, false, false, i, c) : null;
          String text = comp instanceof DefaultTableCellRenderer ? ((DefaultTableCellRenderer) comp).getText() : value.toString();
          buf.append(text.replace('\t', ' '));
          buf.append('\t');
        }
      }
      buf.append('\n');
    }

    return buf.toString();

  }

  public static Action getCopyToClipboardAction(JXTable table, TextProvider textProvider) {
    return new CopyToClipboardAction(textProvider, table);
  }

  public static Action getExportToExcelAction(JXTable table, TextProvider textProvider, String sheetName) {
    return new ExportToExcelAction(textProvider, table, sheetName, 0);
  }

  public static Action getExportToExcelAction(JXTable table, TextProvider textProvider, String sheetName, int excludedColumns) {
    return new ExportToExcelAction(textProvider, table, sheetName, excludedColumns);
  }

  public static TransferHandler getTransferHandler(JXTable table) {
    return new TableTransferHandler(table);
  }

  private static class CopyToClipboardAction extends AbstractAction {
    private static final long serialVersionUID = -5314338375674342446L;
    private JXTable source;

    CopyToClipboardAction(TextProvider textProvider, JXTable source) {
      this.source = source;
      this.putValue(Action.NAME, textProvider.getString("action.table.clipboard.copy"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      TransferHandler.getCopyAction().actionPerformed(new ActionEvent(source, e.getID(), e.getActionCommand()));

    }

  }

  private static class ExportToExcelAction extends AbstractAction {
    private static final long serialVersionUID = -5314338375674342446L;
    private TextProvider textProvider;
    private JXTable source;
    private String sheetName;
    private int excludedCols;

    ExportToExcelAction(TextProvider textProvider, JXTable source, String sheetName, int excludedCols) {
      this.source = source;
      this.sheetName = sheetName;
      this.textProvider = textProvider;
      this.excludedCols = excludedCols;
      this.putValue(Action.NAME, textProvider.getString("action.table.save"));
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setFileFilter(new FileNameExtensionFilter("Excel", "xls"));
      if (fileChooser.showSaveDialog(source) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        if(!file.getName().toLowerCase().endsWith(".xls")) {
          file = new File(file.getAbsolutePath() + ".xls");
        }
        try {
          toExcelFile(textProvider, source, file, sheetName, excludedCols);
        } catch (IOException e) {
          JXErrorPane.showInternalFrame(source, textProvider.createErrorInfo(e, "action.table.save.msg.failed"));
        }

      }

    }

  }

  private static class TableTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 3787320104159518933L;
    private JXTable table;

    public TableTransferHandler(JXTable table) {
      super();
      this.table = table;
    }

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
      String string = toTabSeparatedValues(table);
      if (string != null) {
        StringSelection stringSelection = new StringSelection(string);
        clip.setContents(stringSelection, stringSelection);

      }
    }

    /**
     * @see javax.swing.TransferHandler#createTransferable(javax.swing.JComponent)
     */
    @Override
    protected Transferable createTransferable(JComponent c) {
      String string = toTabSeparatedValues(table);
      if (string != null) {
        return new StringSelection(string);
      } else {
        return null;
      }
    }

  }
}
