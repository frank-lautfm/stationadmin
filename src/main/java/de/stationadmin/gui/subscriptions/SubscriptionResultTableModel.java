/**
 * 
 */
package de.stationadmin.gui.subscriptions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.util.TimeFormat;
import de.stationadmin.gui.ClientContext;

/**
 *
 * @author Frank Korf
 *
 */
public class SubscriptionResultTableModel extends AbstractTableModel {

  public enum Column {
    ARTIST("artist"),
    TITLE("title"),
    ALBUM("album"),
    LENGTH("playtime_seconds"),
    YEAR("releaseyear"),
    GENRE("genre"),
    UPLOADDATE("upload_datum");
    
    private String rawName;
    
    Column(String rawName) {
      this.rawName = rawName;
    }

    /**
     * @return the rawName
     */
    protected String getRawName() {
      return rawName;
    }
    
    
  }
  
  private static final long serialVersionUID = 8544454516149594529L;
  private ClientContext ctx;

  private List<DetailedTrack> results = new ArrayList<DetailedTrack>();

  public SubscriptionResultTableModel(ClientContext ctx) {
    super();
    this.ctx = ctx;
    ctx.getAdminClient().getSubscriptionService().addPropertyChangeListener("results", new PropertyChangeListener() {
      
      @Override
      public void propertyChange(PropertyChangeEvent arg0) {
        setResults(SubscriptionResultTableModel.this.ctx.getAdminClient().getSubscriptionService().getResults());
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
    Column col = Column.values() [column];
    return ctx.getString("searchresult.column." + col.name().toLowerCase());
  }
  
  /**
   * @see javax.swing.table.TableModel#getRowCount()
   */
  @Override
  public int getRowCount() {
    return this.results.size();
  }

  /**
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    Column col = Column.values()[columnIndex];
    DetailedTrack title = this.results.get(rowIndex);
    if(title == null) {
      return null;
    }
    
    switch(col) {
    case ARTIST:
      return title.getArtist();
    case TITLE:
      return title.getTitle();
    case ALBUM:
      return title.getAlbum();
    case LENGTH:
      return TimeFormat.format(title.getLength(), false);
    case UPLOADDATE:
      return title.getUploadDate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(title.getUploadDate()) : null;
    case YEAR:
      return title.getYear() != 0 ? title.getYear() : "";
    case GENRE:
      return title.getGenre();
    }
    
    return null;
  }

  public List<DetailedTrack> getResults() {
    return results;
  }

  public void setResults(List<DetailedTrack> results) {
    this.results = new ArrayList<DetailedTrack>(results);
    this.fireTableDataChanged();
  }


}
