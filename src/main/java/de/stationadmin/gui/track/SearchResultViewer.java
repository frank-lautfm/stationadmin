/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.Color;
import java.awt.Component;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.table.TableColumnExt;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.TrackQuery;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.track.SearchResultTableModel.Column;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.IntTableCellRenderer;

/**
 * 
 * @author Frank Korf
 * 
 */
public class SearchResultViewer extends JPanel {
  private static final long serialVersionUID = 8196389288826457612L;

  private ClientContext ctx;
  private PresentationModel<TrackQuery> queryModel;
  private ValueModel searchResultHolder;
  private ValueModel selectionHolder;
  private PlaySnippetAction playSnippetAction;
  private SearchResultTableModel tableModel;

  public SearchResultViewer(ClientContext ctx, PresentationModel<TrackQuery> queryModel, ValueModel searchResultHolder, ValueModel selectionHolder, boolean multiSelection) {
    this.ctx = ctx;
    this.queryModel = queryModel;
    this.searchResultHolder = searchResultHolder;
    this.selectionHolder = selectionHolder;
    this.init(multiSelection);
  }

  public void setSearchAction(Action action) {
    this.tableModel.setSearchAction(action);
  }

  private void init(boolean multiSelection) {
    this.setLayout(new FormLayout("100dlu:grow", "120dlu:grow"));

    this.tableModel = new SearchResultTableModel(this.ctx, queryModel, searchResultHolder);
    final JXTable table = new JXTable(tableModel);
    table.setColumnControlVisible(true);

    table.addHighlighter(new AbstractHighlighter() {

      @Override
      protected Component doHighlight(Component comp, ComponentAdapter adapter) {
        int row = table.convertRowIndexToModel(adapter.row);
        if (row == 0) {
          if (!adapter.isSelected()) {
            comp.setBackground(new Color(250, 250, 250));
          }
          comp.setFont(ComponentFactory.italicLabelFont);

        }
        return comp;
      }
    });
    table.setRowSorter(new TableRowSorter<SearchResultTableModel>(tableModel) {

      @Override
      public void sort() {
        TrackQuery query = queryModel.getBean();

        String oldOrder = query.getOrderBy();
        boolean oldAsc = query.isOrderAscending();

        SortKey key = this.getSortKeys().size() > 0 ? this.getSortKeys().get(0) : null;

        if (key != null) {
          String rawName = Column.values()[key.getColumn()].getRawName();
          query.setOrderBy(rawName);
          query.setOrderAscending(!key.getSortOrder().equals(SortOrder.DESCENDING));
        } else {
          query.setOrderBy("artist");
          query.setOrderAscending(true);
        }

        if (oldAsc != query.isOrderAscending() || !StringUtils.equals(oldOrder, query.getOrderBy())) {
          if (!query.isEmpty()) {
            SwingUtilities.invokeLater(new Runnable() {

              @Override
              public void run() {
                try {
                  TrackQuery query = queryModel.getBean();
                  query.setPage(1);
                  searchResultHolder.setValue(ctx.getAdminClient().getTrackService().find(query));
                } catch (Exception e) {
                  JXErrorPane.showDialog(null, ctx.getTextProvider().createErrorInfo(e, "action.search.error"));
                }
              }

            });
          }
        }

      }

    });
    table.setSortable(true);

    table.getColumn(Column.YEAR.ordinal()).setMaxWidth(50);
    table.getColumn(Column.YEAR.ordinal()).setCellRenderer(new IntTableCellRenderer(0));
    table.getColumn(Column.LENGTH.ordinal()).setMaxWidth(70);
    table.getColumn(Column.UPLOADDATE.ordinal()).setMaxWidth(80);
    
    ((TableColumnExt) table.getColumnModel().getColumn(Column.GENRE.ordinal())).setVisible(false);
    
    table.setSortable(true);

    final JPopupMenu popup = new JPopupMenu();
    final TagMenu tagMenu = new TagMenu(this.ctx.getTextProvider(), ctx.getAdminClient().getTagManager(), true);
    final TagMenu untagMenu = new TagMenu(this.ctx.getTextProvider(), ctx.getAdminClient().getTagManager(), false);
    final DistributeTracksAction distributeAction = new DistributeTracksAction(this.ctx);
    popup.add(tagMenu);
    popup.add(untagMenu);
    popup.addSeparator();
    popup.add(distributeAction);
    popup.addSeparator();
    this.playSnippetAction = new PlaySnippetAction(this.ctx, this.selectionHolder);
    popup.add(this.playSnippetAction);

    table.addMouseListener(new MouseAdapter() {

      private void checkPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
          popup.show(table, e.getX(), e.getY());
        }
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        this.checkPopup(e);
        if (e.getClickCount() == 2) {
          playSnippetAction.playSnippetInternal();
        }
      }

      /**
       * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
       */
      @Override
      public void mousePressed(MouseEvent e) {
        this.checkPopup(e);
      }

      /**
       * @see java.awt.event.MouseAdapter#mouseReleased(java.awt.event.MouseEvent)
       */
      @Override
      public void mouseReleased(MouseEvent e) {
        this.checkPopup(e);
      }

    });

    table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          int[] rows = table.getSelectedRows();
          int[] titleIds = new int[rows.length];
          int tIdx = 0;
          List<BasicTrack> entries = new ArrayList<BasicTrack>();
          for (int i = 0; i < rows.length; i++) {
            int row = table.convertRowIndexToModel(rows[i]);
            if (row > 0) {
              BasicTrack title = tableModel.getTitle(row);
              entries.add(title);
              titleIds[tIdx++] = title.getId();
            }
          }
          if (tIdx < titleIds.length) {
            int[] newIds = new int[tIdx];
            System.arraycopy(titleIds, 0, newIds, 0, tIdx);
            titleIds = newIds;
          }
          tagMenu.setTitleIds(titleIds);
          untagMenu.setTitleIds(titleIds);
          distributeAction.setTitles(entries);
          selectionHolder.setValue(entries);
        }
      }

    });

    table.setSelectionMode(multiSelection ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
    table.setDragEnabled(true);
    table.setTransferHandler(new TransferHandler() {
      private static final long serialVersionUID = 380546188495324607L;

      /**
       * @see javax.swing.TransferHandler#createTransferable(javax.swing.JComponent)
       */
      @Override
      protected Transferable createTransferable(JComponent c) {
        if (table.getSelectedRow() > -1) {
          StringBuilder buf = new StringBuilder();
          for (int row : table.getSelectedRows()) {
            row = table.convertRowIndexToModel(row);
            DetailedTrack title = tableModel.getTitle(row);
            if (title != null) {
              String str = title.toTabSeparatedValues();
              buf.append(str);
              buf.append("\n");
            }
          }
          return buf.length() > 0 ? new StringSelection(buf.toString()) : super.createTransferable(c);
        }
        return super.createTransferable(c);
      }

      /**
       * @see javax.swing.TransferHandler#getSourceActions(javax.swing.JComponent)
       */
      @Override
      public int getSourceActions(JComponent c) {
        if (table.getSelectedRow() > -1 && (table.getSelectedRows().length > 1 || table.convertRowIndexToModel(table.getSelectedRow()) > 0)) {
          return COPY;
        } else {
          return NONE;
        }
      }

    });

    this.add(new JScrollPane(table), new CellConstraints(1, 1, CellConstraints.FILL, CellConstraints.FILL));

  }

  /**
   * @return the selectionHolder
   */
  public ValueModel getSelectionHolder() {
    return selectionHolder;
  }

}
