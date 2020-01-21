package de.stationadmin.gui.loganalyzer.monthly;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.stationadmin.base.loganalyzer.MonthlySummary;
import de.stationadmin.gui.TextProvider;

public class MonthlySummaryTableModel extends AbstractTableModel {
  private static final long serialVersionUID = -146588916063625296L;
  public static final int TYPE_TLH = 1;
  public static final int TYPE_UNIQS = 2;

  private TextProvider textProvider;
  private int type;

  private List<MonthlySummary[]> years = new ArrayList<>();
  private List<String> colNames = new ArrayList<>();
  private List<Integer> summary = new ArrayList<>();

  public MonthlySummaryTableModel(TextProvider textProvider, List<MonthlySummary> data, int type) {
    super();
    this.textProvider = textProvider;
    this.type = type;
    this.initYears(data);

  }

  private void initYears(List<MonthlySummary> data) {
    this.years.clear();
    this.colNames.clear();
    this.colNames.add("");
    data.sort((a, b) -> -Integer.compare(a.getYear(), b.getYear()));
    int currentYear = 0;
    MonthlySummary[] current = new MonthlySummary[12];
    for (MonthlySummary entry : data) {
      if (entry.getYear() != currentYear) {
        current = new MonthlySummary[12];
        currentYear = entry.getYear();
        years.add(current);
        this.colNames.add(Integer.toString(currentYear));
      }
      current[entry.getMonth() - 1] = entry;
    }

    for (MonthlySummary[] year : years) {
      int numEntries = 0;
      int sum = 0;
      for (int i = 0; i < 12; i++) {
        if (year[i] != null) {
          numEntries++;
          sum += type == TYPE_TLH ? year[i].getTlh() : year[i].getAvgUniqs();
        }
      }
      summary.add(type == TYPE_TLH ? sum : sum / numEntries);
    }
  }

  @Override
  public int getRowCount() {
    return 13;
  }

  @Override
  public int getColumnCount() {
    return this.years.size() + 1;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    if (columnIndex == 0) {
      return rowIndex < 12 ? textProvider.getString("month." + (rowIndex + 1) + ".short") : 
        (type == TYPE_TLH ? textProvider.getString("sum") : "Ø");
    }
    if (rowIndex == 12) {
      return summary.get(columnIndex - 1);
    }
    MonthlySummary[] year = years.get(columnIndex - 1);
    if (rowIndex >= 0 && year[rowIndex] != null) {
      return type == TYPE_TLH ? year[rowIndex].getTlh() : year[rowIndex].getAvgUniqs();
    }
    return null;
  }

  @Override
  public String getColumnName(int column) {
    return colNames.get(column);
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    return columnIndex == 0 ? String.class : Integer.class;
  }

}
