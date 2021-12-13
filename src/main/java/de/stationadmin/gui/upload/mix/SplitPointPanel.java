/**
 * 
 */
package de.stationadmin.gui.upload.mix;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVWriter;
import de.stationadmin.base.mp3splitter.DJMixTitleListParser;
import de.stationadmin.base.mp3splitter.SplitPoint;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.upload.mix.SplitPointTableModel.Column;
import de.stationadmin.gui.util.AlignedTableCellRenderer;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.RXTable;

/**
 * @author Frank
 * 
 */
public class SplitPointPanel extends JPanel {
  private static final long serialVersionUID = -5826190464357614146L;
  private ValueModel lastDirHolder = new ValueHolder();

  private ClientContext ctx;
  private TextProvider textProvider;
  private SplitPointTableModel tableModel;
  private AlignedTableCellRenderer rightAlignedTableCellRender = new AlignedTableCellRenderer(JLabel.RIGHT);

  /**
   * @param textProvider
   */
  public SplitPointPanel(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.textProvider = ctx.getTextProvider();
    this.tableModel = new SplitPointTableModel(textProvider);
    this.init();
  }

  private void init() {
    this.setLayout(new BorderLayout());

    final RXTable table = new RXTable(this.tableModel) {
      private static final long serialVersionUID = -7362236957111245738L;

      public TableCellRenderer getCellRenderer(int row, int column) {
        if (column == Column.POSITION.ordinal()) {
          return rightAlignedTableCellRender;
        }
        return super.getCellRenderer(row, column);
      }

    };
    table.setSelectAllForEdit(true);
    final SplitPointTransferHandler transferHandler = new SplitPointTransferHandler(table);
    table.setTransferHandler(transferHandler);

    table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
    table.getActionMap().put("delete", new AbstractAction() {
      private static final long serialVersionUID = -2841090125019659296L;

      @Override
      public void actionPerformed(ActionEvent evt) {
        int row = table.getSelectedRow();
        row = table.convertRowIndexToModel(row);
        tableModel.deleteRow(row);
      }
    });

    table.setCellSelectionEnabled(true);
    table.getColumnModel().getColumn(Column.POSITION.ordinal()).setPreferredWidth(100);
    table.getColumnModel().getColumn(Column.POSITION.ordinal()).setMaxWidth(100);

    JScrollPane tScroll = new JScrollPane(table);
    tScroll.setPreferredSize(new Dimension(400, 300));
    this.add(tScroll, BorderLayout.CENTER);

    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.add(new PasteAction(table));
    toolbar.add(new ReadTracklistAction());
    toolbar.add(new WriteTracklistAction());

    this.add(toolbar, BorderLayout.NORTH);

    JPanel bottom = new JPanel(new FormLayout("pref,5dlu:grow,pref", "pref"));
    CellConstraints cc = new CellConstraints();
    bottom.add(new JLabel(textProvider.getString("upload.mix.splitpoint.position.hint")), cc.xy(1, 1));
    JToolBar tb = new JToolBar();
    tb.setFloatable(false);
    tb.add(new JButton(new ResortAction()));
    bottom.add(tb, cc.xy(3, 1));
    this.add(bottom, BorderLayout.SOUTH);

  }

  /**
   * @return the tableModel
   */
  public SplitPointTableModel getTableModel() {
    return tableModel;
  }

  private class SplitPointTransferHandler extends TransferHandler {
    /**
     * @param table
     */
    public SplitPointTransferHandler(JXTable table) {
      super();
      this.table = table;
    }

    private static final long serialVersionUID = 4256761293539147616L;
    private JXTable table;

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.swing.TransferHandler#exportToClipboard(javax.swing.JComponent,
     * java.awt.datatransfer.Clipboard, int)
     */
    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
      int row = table.getSelectedRow();
      int col = table.getSelectedColumn();
      String value = (String) table.getModel().getValueAt(row, col);
      if (value != null) {
        StringSelection stringSelection = new StringSelection(value);
        clip.setContents(stringSelection, stringSelection);
      }

    }

    /**
     * 
     * @see javax.swing.TransferHandler#importData(javax.swing.TransferHandler.TransferSupport)
     */
    @Override
    public boolean importData(TransferSupport support) {
      int row = table.getSelectedRow();
      int col = table.getSelectedColumn();
      for (DataFlavor flavor : support.getDataFlavors()) {
        if (flavor.equals(DataFlavor.stringFlavor)) {
          try {
            String string = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
            if (string.indexOf('\n') > 0) {
              List<SplitPoint> titles = DJMixTitleListParser.parse(string);
              ((SplitPointTableModel) this.table.getModel()).insert(row >= 0 ? row : 0, titles);
            } else {
              table.getModel().setValueAt(string, row >= 0 ? row : 0, col >= 0 ? col : 0);
            }
          } catch (Exception e) {
            LogManager.getLogger(SplitPointPanel.class).error("error while parsing dj mix list", e);
          }

        }
      }
      return false;
    }

    /**
     * @see javax.swing.TransferHandler#canImport(javax.swing.TransferHandler.TransferSupport)
     */
    @Override
    public boolean canImport(TransferSupport support) {
      return support.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

  }

  public void readCSV(File file) throws IOException {
    String content = FileUtils.readFileToString(file, "UTF-8");
    String[] lines = StringUtils.split(content, "\r\n");

    // try to detect separator char
    char[] separatorCandidates = { ';', ',', '\t' };
    char seperator = ';';

    boolean accepted = false;
    for (int i = 0; i < separatorCandidates.length && !accepted; i++) {
      int matches = 0;
      for (String line : lines) {
        String[] fields = StringUtils.split(line, "" + separatorCandidates[i]);
        if (fields.length >= 3) {
          matches++;
        }
      }
      if (matches >= lines.length - 1) {
        accepted = true;
        seperator = separatorCandidates[i];
      }
    }

    CSVParser parser = new CSVParser(seperator);
    for (int i = 0; i < lines.length; i++) {
      if (StringUtils.isNotEmpty(lines[i])) {
        String[] fields = parser.parseLine(lines[i]);

        String time = fields[0];
        String artist = fields.length > 1 ? fields[1] : null;
        String title = fields.length > 2 ? fields[2] : null;
        String album = fields.length > 3 ? fields[3] : null;

        this.tableModel.setValueAt(time, i, Column.POSITION.ordinal());
        this.tableModel.setValueAt(artist, i, Column.ARTIST.ordinal());
        this.tableModel.setValueAt(title, i, Column.TITLE.ordinal());
        this.tableModel.setValueAt(album, i, Column.ALBUM.ordinal());
      }
    }

  }

  public void writeCSV(File file) throws IOException {
    if(!file.getName().toLowerCase().endsWith(".csv")) {
      file = new File(file.getAbsolutePath() + ".csv");
    }
    FileWriterWithEncoding fwriter = new FileWriterWithEncoding(file, "UTF-8");
    CSVWriter writer = new CSVWriter(fwriter, ';');

    for (int i = 0; i < this.tableModel.getRowCount(); i++) {
      if (!StringUtils.isEmpty((String) tableModel.getValueAt(i, Column.ARTIST.ordinal()))) {
        writer.writeNext(new String[] { (String) tableModel.getValueAt(i, Column.POSITION.ordinal()),
            (String) tableModel.getValueAt(i, Column.ARTIST.ordinal()), (String) tableModel.getValueAt(i, Column.TITLE.ordinal()),
            (String) tableModel.getValueAt(i, Column.ALBUM.ordinal()) });
      }
    }

    writer.close();
    fwriter.close();

  }

  private class ResortAction extends AbstractAction {
    private static final long serialVersionUID = -6101034003650859288L;

    ResortAction() {
      this.putValue(Action.NAME, textProvider.getString("upload.mix.splitpoint.resort"));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      tableModel.resort();
    }

  }

  private class PasteAction extends AbstractAction {
    private static final long serialVersionUID = 2186867212185709977L;
    private JComponent source;

    PasteAction(JComponent source) {
      this.source = source;
      this.putValue(Action.SMALL_ICON, AppUtils.getIcon("paste.png"));
      this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("upload.mix.action.tracklist.paste.description"));

    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      TransferHandler.getPasteAction().actionPerformed(new ActionEvent(source, evt.getID(), evt.getActionCommand()));

    }

  }

  private class ReadTracklistAction extends AbstractAction {
    private static final long serialVersionUID = 2186867212185709977L;

    ReadTracklistAction() {
      this.putValue(Action.SMALL_ICON, AppUtils.getIcon("open.png"));
      this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("upload.mix.action.tracklist.open.description"));

    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      JFileChooser fileChooser = new JFileChooser();
      if (lastDirHolder.getValue() != null) {
        fileChooser.setCurrentDirectory((File)lastDirHolder.getValue());
      }
      fileChooser.setFileFilter(new FileNameExtensionFilter("CSV", "csv"));
      if (fileChooser.showOpenDialog(SplitPointPanel.this) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        lastDirHolder.setValue(file.getParentFile());
        try {
          readCSV(file);
        } catch (IOException e) {
          JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "Fehler beim Lesen der Datei"));
        }

      }

    }

  }

  private class WriteTracklistAction extends AbstractAction {
    private static final long serialVersionUID = 2186867212185709977L;

    WriteTracklistAction() {
      this.putValue(Action.SMALL_ICON, AppUtils.getIcon("save.png"));
      this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("upload.mix.action.tracklist.save.description"));

    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      JFileChooser fileChooser = new JFileChooser();
      if (lastDirHolder.getValue() != null) {
        fileChooser.setCurrentDirectory((File)lastDirHolder.getValue());
      }
      fileChooser.setFileFilter(new FileNameExtensionFilter("CSV", "csv"));
      if (fileChooser.showSaveDialog(SplitPointPanel.this) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        lastDirHolder.setValue(file.getParentFile());
        try {
          writeCSV(file);
        } catch (IOException e) {
          JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "Fehler beim Schreiben der Datei"));
        }

      }

    }

  }

  public ValueModel getLastDirHolder() {
    return lastDirHolder;
  }

  public void setLastDirHolder(ValueModel lastDirHolder) {
    this.lastDirHolder = lastDirHolder;
  }

}
