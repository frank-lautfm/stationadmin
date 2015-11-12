package de.stationadmin.base.track.exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;

import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.util.TimeFormat;

public class TitleListExcelExporter implements TitleListExporter {

  public void toFile(List<RegisteredTrack> titles, File file, boolean full) throws IOException {
    Workbook wb = new HSSFWorkbook();
    
    Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName("Titel"));
    CreationHelper helper = wb.getCreationHelper();
    
    int albumCol = -1;
    for(int i = 0; i < titles.size(); i++) {
      RegisteredTrack title = titles.get(i);
      
      Row row = sheet.createRow(i);
      
      Cell artistCell = row.createCell(0);
      artistCell.setCellValue(helper.createRichTextString(title.getArtist()));

      Cell titleCell = row.createCell(1);
      titleCell.setCellValue(helper.createRichTextString(title.getTitle()));
      
      int cell = 2;
      
      if(full) {
        albumCol = cell;
        Cell albumCell = row.createCell(cell++);
        albumCell.setCellValue(helper.createRichTextString(StringUtils.trimToEmpty(title.getAlbum())));
      }

      Cell lengthCell = row.createCell(cell++);
      lengthCell.setCellValue(helper.createRichTextString(TimeFormat.format(title.getLength(), false)));
      CellStyle cellStyle = wb.createCellStyle();
      cellStyle.setAlignment(CellStyle.ALIGN_RIGHT);
      lengthCell.setCellStyle(cellStyle);

      if(full) {
        Cell genreCell = row.createCell(cell++);
        genreCell.setCellValue(helper.createRichTextString(StringUtils.trimToEmpty(title.getGenre())));
        
        Cell yearCell = row.createCell(cell++);
        if(title.getYear() > 0) {
          yearCell.setCellValue(title.getYear());
        }
      }

    }
    
    sheet.autoSizeColumn(0);
    sheet.autoSizeColumn(1);
    if(full) {
      sheet.autoSizeColumn(albumCol);
      
    }
    
    FileOutputStream fileOut = new FileOutputStream(file);
    wb.write(fileOut);
    fileOut.close();
  }


}
