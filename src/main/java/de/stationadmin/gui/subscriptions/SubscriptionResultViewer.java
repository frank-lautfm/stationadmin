/**
 * 
 */
package de.stationadmin.gui.subscriptions;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.subscriptions.SubscriptionResultTableModel.Column;
import de.stationadmin.gui.track.DistributeTracksAction;
import de.stationadmin.gui.track.PlaySnippetAction;
import de.stationadmin.gui.track.TagMenu;
import de.stationadmin.gui.util.IntTableCellRenderer;
import de.stationadmin.gui.util.LengthTableCellRenderer;

/**
 * 
 * @author Frank Korf
 * 
 */
public class SubscriptionResultViewer extends JPanel {
  private static final long serialVersionUID = 8196389288826457612L;

  private ClientContext ctx;
  private ValueModel selectionHolder;
  private PlaySnippetAction playSnippetAction;

  public SubscriptionResultViewer(ClientContext ctx, ValueModel selectionHolder, boolean multiSelection) {
    this.ctx = ctx;
    this.selectionHolder = selectionHolder;
    this.init(multiSelection);
  }

  private void init(boolean multiSelection) {
    this.setLayout(new FormLayout("100dlu:grow", "80dlu:grow,pref"));

    final SubscriptionResultTableModel tableModel = new SubscriptionResultTableModel(this.ctx);
    tableModel.setResults(ctx.getAdminClient().getSubscriptionService().getResults());
    final JXTable table = new JXTable(tableModel);
    table.getColumn(Column.YEAR.ordinal()).setMaxWidth(50);
    table.getColumn(Column.YEAR.ordinal()).setCellRenderer(new IntTableCellRenderer(0));
    table.getColumn(Column.LENGTH.ordinal()).setMaxWidth(70);
    table.getColumn(Column.LENGTH.ordinal()).setCellRenderer(new LengthTableCellRenderer(false));;
    table.getColumn(Column.UPLOADDATE.ordinal()).setMaxWidth(80);
    table.setSortable(true);
    table.setSortOrder(Column.UPLOADDATE.ordinal(), SortOrder.DESCENDING);

    SubscriptionResultDeleteAction deleteAction = new SubscriptionResultDeleteAction(ctx, selectionHolder);
    table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
    table.getActionMap().put("delete", deleteAction);

    final JPopupMenu popup = new JPopupMenu();
    final TagMenu tagMenu = new TagMenu(this.ctx.getTextProvider(), ctx.getAdminClient().getTagManager(), true);
    final TagMenu untagMenu = new TagMenu(this.ctx.getTextProvider(), ctx.getAdminClient().getTagManager(), false);
    final DistributeTracksAction distributeAction = new DistributeTracksAction(this.ctx);
    popup.add(tagMenu);
    popup.add(untagMenu);
    popup.addSeparator();
    popup.add(distributeAction);
    popup.add(deleteAction);
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
          playSnippetAction.playSnippet();
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
          List<BasicTrack> entries = new ArrayList<BasicTrack>();
          for (int i = 0; i < rows.length; i++) {
            int row = table.convertRowIndexToModel(rows[i]);
            BasicTrack title = tableModel.getResults().get(row);
            entries.add(title);
            titleIds[i] = title.getId();
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
            String str = tableModel.getResults().get(row).toTabSeparatedValues();
            buf.append(str);
            buf.append("\n");
          }
          return new StringSelection(buf.toString());
        }
        return super.createTransferable(c);
      }

      /**
       * @see javax.swing.TransferHandler#getSourceActions(javax.swing.JComponent)
       */
      @Override
      public int getSourceActions(JComponent c) {
        return COPY;
      }

    });

    JXStatusBar statusBar = new JXStatusBar();
    statusBar.setOpaque(false);

    final JLabel hitsLabel = new JLabel("");
    ctx.getAdminClient().getSubscriptionService().addPropertyChangeListener("results", new PropertyChangeListener() {
      
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        hitsLabel.setText(ctx.getAdminClient().getSubscriptionService().getResults().size() + " Titel");
      }
    });
    statusBar.add(hitsLabel, new JXStatusBar.Constraint());

    this.add(new JScrollPane(table), new CellConstraints(1, 1, CellConstraints.FILL, CellConstraints.FILL));
    this.add(statusBar, new CellConstraints(1, 2, CellConstraints.FILL, CellConstraints.FILL));

  }

  /**
   * @return the selectionHolder
   */
  public ValueModel getSelectionHolder() {
    return selectionHolder;
  }

}
