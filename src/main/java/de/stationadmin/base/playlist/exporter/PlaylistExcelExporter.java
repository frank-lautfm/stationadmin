package de.stationadmin.base.playlist.exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.track.Title;
import de.stationadmin.base.util.TimeFormat;

public class PlaylistExcelExporter extends PlaylistExporter {
  
  
  /**
   * Saves a playlist to a file
   * @param playlist
   * @param file
   * @throws IOException
   */
  public void toFile(Playlist playlist, File file) throws IOException {
    Workbook wb = new HSSFWorkbook();
    
    Sheet plSheet = wb.createSheet(WorkbookUtil.createSafeSheetName(playlist.getName()));
    CreationHelper helper = wb.getCreationHelper();
    
    int rowNum = 0;
    for(Entry entry : playlist.getEntries()) {
      Row row = plSheet.createRow(rowNum++);
      
      Cell timeCell = row.createCell(0);
      timeCell.setCellValue(helper.createRichTextString(TimeFormat.format(entry.getStart(), true)));
      CellStyle cellStyle = wb.createCellStyle();
      cellStyle.setAlignment(CellStyle.ALIGN_RIGHT);
      timeCell.setCellStyle(cellStyle);
      
      Cell artistCell = row.createCell(1);
      artistCell.setCellValue(helper.createRichTextString(entry.getTrack().getArtist()));

      Cell titleCell = row.createCell(2);
      titleCell.setCellValue(helper.createRichTextString(entry.getTrack().getTitle()));
      
      Cell lengthCell = row.createCell(3);
      lengthCell.setCellValue(helper.createRichTextString(TimeFormat.format(entry.getTrack().getLength(), false)));
      cellStyle = wb.createCellStyle();
      cellStyle.setAlignment(CellStyle.ALIGN_RIGHT);
      lengthCell.setCellStyle(cellStyle);

    }

    plSheet.autoSizeColumn(1);
    plSheet.autoSizeColumn(2);

    FileOutputStream fileOut = new FileOutputStream(file);
    wb.write(fileOut);
    fileOut.close();
  }


  @Override
  protected String toString(Entry entry, Title title) {
    return null;
  }

  @Override
  protected String getHeadLine() {
    return null;
  }

}
