/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.TrackService;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.DisposeAction;

/**
 * 
 * @author Frank Korf
 * 
 */
public class TracksDeleteDlg extends JDialog {
  private static final long serialVersionUID = -5853278386931287783L;
  private TextProvider textProvider;
  private TrackService titleService;
  private DeletionCandidate[] titles;
  private DeleteAction deleteAction;
  private DeletionCandidateTableModel tableModel;

  public TracksDeleteDlg(TextProvider textProvider, TrackService titleService, int[] titleIds) {
    super();
    this.textProvider = textProvider;
    this.titleService = titleService;
    this.deleteAction  = new DeleteAction();
    this.titles = new DeletionCandidate[titleIds.length];
    for (int i = 0; i < titleIds.length; i++) {
      RegisteredTrack title = titleService.getTrackRegistry().getTrack(titleIds[i]);
      this.titles[i] = new DeletionCandidate(title);
    }
    this.init();
  }

  private void init() {
    this.setTitle(textProvider.getString("titledelete.title"));
    
    this.tableModel = new DeletionCandidateTableModel();
    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,200px:grow,5dlu,pref,5dlu"));
    final JXTable table = new JXTable(tableModel);
    table.getColumnModel().getColumn(Column.MARKDED.ordinal()).setPreferredWidth(40);
    table.getColumnModel().getColumn(Column.MARKDED.ordinal()).setMaxWidth(40);
    
    JScrollPane tableScroll = new JScrollPane(table);
    this.getContentPane().add(tableScroll, new CellConstraints(2, 2));

    table.addHighlighter(new AbstractHighlighter() {

      @Override
      protected Component doHighlight(Component comp, ComponentAdapter adapter) {
        int row = table.convertRowIndexToModel(adapter.row);
        if (!titles[row].isDeletable()) {
          comp.setForeground(Color.LIGHT_GRAY);
        }
        return comp;
      }

    });

    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 10));
    buttonPanel.add(new JButton(deleteAction));
    buttonPanel.add(new JButton(new DisposeAction(this, "Abbrechen")));

    this.getContentPane().add(buttonPanel, new CellConstraints(2, 4, CellConstraints.CENTER, CellConstraints.CENTER));

    this.setSize(400, 300);
    AppUtils.centerWithinRoot(this);

    Thread t = new Thread() {

      /**
       * @see java.lang.Thread#run()
       */
      @Override
      public void run() {
        try {
          for (DeletionCandidate title : titles) {
            boolean used = titleService.isTrackUsed(title.getTitle().getId());
            title.setDeletable(!used);
            title.setMarkedForDeletion(!used);
            fireDataChanged();
          }
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              deleteAction.setEnabled(true);
            }
          });
        } catch (Exception e) {
          e.printStackTrace();

        }
      }

    };
    t.start();
  }

  void fireDataChanged() {
    SwingUtilities.invokeLater(new Runnable() {

      public void run() {
        tableModel.fireTableDataChanged();
      }

    });
  }

  private static class DeletionCandidate {
    private RegisteredTrack title;
    private volatile boolean deletable = false;
    private boolean markedForDeletion = false;

    public DeletionCandidate(RegisteredTrack title) {
      super();
      this.title = title;
    }

    /**
     * @return the deletable
     */
    protected boolean isDeletable() {
      return this.title.isOwnTitle() && deletable;
    }

    /**
     * @param deletable
     *          the deletable to set
     */
    protected void setDeletable(boolean deletable) {
      this.deletable = deletable;
    }

    /**
     * @return the markedForDeletion
     */
    protected boolean isMarkedForDeletion() {
      return markedForDeletion;
    }

    /**
     * @param markedForDeletion
     *          the markedForDeletion to set
     */
    protected void setMarkedForDeletion(boolean markedForDeletion) {
      this.markedForDeletion = markedForDeletion;
    }

    /**
     * @return the title
     */
    protected RegisteredTrack getTitle() {
      return title;
    }

  }

  private class DeletionCandidateTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 5198013750117614285L;

    /**
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    @Override
    public int getColumnCount() {
      return Column.values().length;
    }

    /**
     * @see javax.swing.table.TableModel#getRowCount()
     */
    @Override
    public int getRowCount() {
      return titles.length;
    }

    /**
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      Column col = Column.values()[columnIndex];
      switch (col) {
      case MARKDED:
        return titles[rowIndex].isDeletable() ? titles[rowIndex].isMarkedForDeletion() : null;
      case ARTIST:
        return titles[rowIndex].getTitle().getArtist();
      case TITLE:
        return titles[rowIndex].getTitle().getTitle();
      }
      return null;
    }

    /**
     * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
      Column col = Column.values()[columnIndex];
      if (col == Column.MARKDED) {
        return Boolean.class;
      } else {
        return String.class;
      }
    }

    /**
     * @see javax.swing.table.AbstractTableModel#getColumnName(int)
     */
    @Override
    public String getColumnName(int column) {
      Column col = Column.values()[column];
      if(col == Column.MARKDED) {
        return "";
      }
      return textProvider.getString("titledelete.column." + col.name().toLowerCase());
    }

    /**
     * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return titles[rowIndex].isDeletable() && columnIndex == Column.MARKDED.ordinal();
    }

    /**
     * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object,
     *      int, int)
     */
    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
      Column col = Column.values()[columnIndex];
      if (col == Column.MARKDED) {
        Boolean v = (Boolean) value;
        titles[rowIndex].setMarkedForDeletion(v.booleanValue());
      }
    }

  }

  enum Column {
    MARKDED, ARTIST, TITLE,

  }

  private class DeleteAction extends AbstractAction {
    private static final long serialVersionUID = -6843424786608158669L;

    DeleteAction() {
      this.putValue(Action.NAME, textProvider.getString("titledelete.action.delete"));
      this.setEnabled(false);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      int cnt = 0;
      for (DeletionCandidate title : titles) {
        if (title.isMarkedForDeletion()) {
          cnt++;
        }
      }

      int[] titleIds = new int[cnt];
      int idx = 0;
      for (DeletionCandidate title : titles) {
        if (title.isMarkedForDeletion()) {
          titleIds[idx++] = title.getTitle().getId();
        }
      }
      try {
        titleService.deleteTracks(titleIds);
        dispose();
      } catch (Exception e) {
        JXErrorPane.showDialog(AppUtils.getRootFrame(), textProvider.createErrorInfo(e, "titledelete.msg.failed"));
      }

    }

  }

}
