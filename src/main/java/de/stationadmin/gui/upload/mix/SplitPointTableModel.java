/**
 * 
 */
package de.stationadmin.gui.upload.mix;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang.StringUtils;

import de.stationadmin.base.mp3splitter.SplitPoint;
import de.stationadmin.gui.TextProvider;

/**
 * @author Frank
 *
 */
public class SplitPointTableModel extends AbstractTableModel {
  private static final long serialVersionUID = -2108865568662719294L;
  private TextProvider textProvider;
  private ExtSplitSpoint[] splitPoints = new ExtSplitSpoint[100];
  private boolean autoResort = false;

  public SplitPointTableModel() {
    this(new TextProvider());
  }

  /**
   * @param textProvider
   */
  public SplitPointTableModel(TextProvider textProvider) {
    super();
    this.textProvider = textProvider;
    this.splitPoints[0] = new ExtSplitSpoint();
  }

  public void deleteRow(int row) {
    if (row >= 0 && row < this.splitPoints.length) {
      for (int i = row; i < this.splitPoints.length - 1; i++) {
        this.splitPoints[i] = this.splitPoints[i + 1];
      }
      this.splitPoints[this.splitPoints.length - 1] = null;
      this.fireTableDataChanged();
    }
  }

  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return Column.values().length;
  }

  /* (non-Javadoc)
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    Column c = Column.values()[column];
    return this.textProvider.getString("upload.mix.splitpoint." + c.name().toLowerCase());
  }

  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getRowCount()
   */
  @Override
  public int getRowCount() {
    return this.splitPoints.length;
  }

  public List<SplitPoint> getSplitPoints() {
    List<SplitPoint> list = new ArrayList<SplitPoint>();
    for (int i = 0; i < splitPoints.length; i++) {
      if (splitPoints[i] != null && !StringUtils.isEmpty(splitPoints[i].getTitle())
          && !StringUtils.isEmpty(splitPoints[i].getArtist())) {
        list.add(splitPoints[i]);
      }
    }
    return list;

  }

  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  @Override
  public Object getValueAt(int row, int col) {
    Column c = Column.values()[col];
    ExtSplitSpoint sp = splitPoints[row];
    if (sp != null) {
      switch (c) {
        case ALBUM :
          return sp.getAlbum();
        case ARTIST :
          return sp.getArtist();
        case POSITION :
          return sp.getPositionAsString();
        case TITLE :
          return sp.getTitle();
      }
    }
    return null;
  }

  /**
   * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
   */
  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return true;
  }

  public void resort() {
    Arrays.sort(this.splitPoints, new Comparator<SplitPoint>() {

      @Override
      public int compare(SplitPoint o1, SplitPoint o2) {
        long pos1 = o1 != null ? o1.getPosition() : Integer.MAX_VALUE;
        long pos2 = o2 != null ? o2.getPosition() : Integer.MAX_VALUE;
        return Long.valueOf(pos1).compareTo(pos2);
      }
    });
    this.fireTableDataChanged();

  }

  /**
   * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
   */
  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    Column c = Column.values()[columnIndex];
    if (splitPoints[rowIndex] == null) {
      splitPoints[rowIndex] = new ExtSplitSpoint();
    }
    boolean resort = false;
    switch (c) {
      case ALBUM :
        splitPoints[rowIndex].setAlbum((String) aValue);
        break;
      case ARTIST :
        splitPoints[rowIndex].setArtist((String) aValue);
        break;
      case TITLE :
        splitPoints[rowIndex].setTitle((String) aValue);
        break;
      case POSITION :
        try {
          splitPoints[rowIndex].setPositionAsString((String) aValue);
          resort = true;
        } catch (Exception e) {
          Toolkit.getDefaultToolkit().beep();
        }
        break;
    }
    if (this.autoResort && resort) {
      this.resort();
    }
  }

  public void insert(int row, List<SplitPoint> titles) {
    if (this.splitPoints.length < row + titles.size()) {
      ExtSplitSpoint[] newSp = new ExtSplitSpoint[row + titles.size() + 10];
      System.arraycopy(this.splitPoints, 0, newSp, 0, this.splitPoints.length);
      this.splitPoints = newSp;
    }
    for (int i = 0; i < titles.size(); i++) {
      if (this.splitPoints[row + i] == null) {
        this.splitPoints[row + i] = new ExtSplitSpoint();
      }
      this.splitPoints[row + i].setPosition(titles.get(i).getPosition());
      this.splitPoints[row + i].setArtist(titles.get(i).getArtist());
      this.splitPoints[row + i].setTitle(titles.get(i).getTitle());
    }
    this.fireTableDataChanged();
  }

  enum Column {
    POSITION, ARTIST, TITLE, ALBUM
  }

  /**
   * @return the autoResort
   */
  public boolean isAutoResort() {
    return autoResort;
  }

  /**
   * @param autoResort the autoResort to set
   */
  public void setAutoResort(boolean autoResort) {
    this.autoResort = autoResort;
  }
}
