/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Random;

import javax.swing.Action;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang.ObjectUtils;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.SearchResultSet;
import de.stationadmin.base.track.TrackQuery;
import de.stationadmin.base.util.TimeFormat;
import de.stationadmin.gui.ClientContext;

/**
 * 
 * @author Frank Korf
 * 
 */
public class SearchResultTableModel extends AbstractTableModel {

  public enum Column {
    ARTIST("artist"), TITLE("title"), ALBUM("album"), LENGTH("duration"), YEAR("year", "release_year"), UPLOADDATE("created_at");

    private String modelName;
    private String rawName;

    Column(String modelName, String rawName) {
      this.modelName = modelName;
      this.rawName = rawName;
    }

    Column(String rawName) {
      this.modelName = rawName;
      this.rawName = rawName;
    }

    /**
     * @return the rawName
     */
    protected String getRawName() {
      return rawName;
    }

    protected String getModelName() {
      return modelName;
    }

  }

  private static final long serialVersionUID = 8544454516149594529L;
  private ClientContext ctx;

  private PresentationModel<TrackQuery> queryModel;
  private SearchResultSet resultSet;
  
  private Action searchAction;

  public SearchResultTableModel(ClientContext ctx, PresentationModel<TrackQuery> queryModel, ValueModel resultSetHolder) {
    super();
    this.ctx = ctx;
    this.queryModel = queryModel;
    resultSetHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        SearchResultSet resultSet = (SearchResultSet) evt.getNewValue();
        setResultSet(resultSet);
      }

    });
  }

  /**
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return Column.values().length;
  }

  /**
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    Column col = Column.values()[column];
    return ctx.getString("searchresult.column." + col.name().toLowerCase());
  }

  /**
   * @see javax.swing.table.TableModel#getRowCount()
   */
  @Override
  public int getRowCount() {
    return this.resultSet != null ? this.resultSet.getTitles().size() + 1 : 1;
  }

  /**
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    Column col = Column.values()[columnIndex];

    if (rowIndex == 0) {
      switch (col) {
      case ARTIST:
      case TITLE:
      case ALBUM:
      case YEAR:
        Object year = queryModel.getModel(col.getModelName()).getValue();
        return year == null || year.equals(Integer.valueOf(0)) ? null : year;
      case LENGTH:
      case UPLOADDATE:
        return null;
      }

    } else {
      DetailedTrack title = this.resultSet.getTitles().get(rowIndex - 1);

      switch (col) {
      case ARTIST:
        return title.getArtist();
      case TITLE:
        return title.getTitle();
      case ALBUM:
        return title.getAlbum();
      case LENGTH:
        return TimeFormat.format(title.getLength(), false);
      case UPLOADDATE:
        return new SimpleDateFormat("yyyy-MM-dd").format(title.getUploadDate());
      case YEAR:
        return title.getYear() != 0 ? title.getYear() : "";
      }
    }

    return null;
  }

  /**
   * @return the resultSet
   */
  public SearchResultSet getResultSet() {
    return resultSet;
  }

  public DetailedTrack getTitle(int row) {
    if (row > 0) {
      return this.resultSet.getTitles().get(row - 1);
    }
    return null;
  }

  /**
   * @param resultSet
   *          the resultSet to set
   */
  protected void setResultSet(SearchResultSet resultSet) {
    this.resultSet = resultSet;
    this.fireTableDataChanged();
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    Column col = Column.values()[columnIndex];
    switch (col) {
    case LENGTH:
    case YEAR:
      return Integer.class;
    default:
      return String.class;

    }
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    Column col = Column.values()[columnIndex];
    switch (col) {
    case ALBUM:
    case ARTIST:
    case TITLE:
    case YEAR:
      return true;
    default:
      return false;
    }
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    if (rowIndex == 0) {
      Column col = Column.values()[columnIndex];
      if(aValue == null && col == Column.YEAR) {
        aValue = 0;
      }
      Object old = queryModel.getModel(col.getModelName()).getValue();
      queryModel.getModel(col.getModelName()).setValue(aValue);
      if(searchAction != null && !ObjectUtils.equals(old, aValue)) {
        searchAction.actionPerformed(new ActionEvent(this, new Random().nextInt(), "search"));
      }
    }
  }

  public Action getSearchAction() {
    return searchAction;
  }

  public void setSearchAction(Action searchAction) {
    this.searchAction = searchAction;
  }

}
