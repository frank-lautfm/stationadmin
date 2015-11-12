/**
 * 
 */
package de.stationadmin.base.track.format;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.Title;

/**
 * Internal format with id, artist, title, length and type separated by tabs.
 * This format is used for internal data exchange via drag & drop and clipboard.
 * 
 * @author Frank Korf
 */
public class ExtendedTrackFormat implements TrackExportFormat {

  private boolean writeDetailedData = false;

  public ExtendedTrackFormat() {
    this(false);
  }

  /**
   * @param writeDetailedData
   */
  public ExtendedTrackFormat(boolean writeDetailedData) {
    super();
    this.writeDetailedData = writeDetailedData;
  }

  /**
   * @see de.stationadmin.base.track.format.TrackExportFormat#fromString(java.lang.String)
   */
  @Override
  public DetailedTrack fromString(String str) {
    String[] parts = str.split("\t");
    if (parts.length >= 4) {
      try {
        int id = Integer.parseInt(parts[0].trim());
        DetailedTrack title = new DetailedTrack();
        title.setId(id);
        title.setArtist(parts[1].trim());
        title.setTitle(parts[2].trim());
        title.setLength(Integer.parseInt(parts[3]));
        if (parts.length >= 5) {
          title.setType(Integer.parseInt(parts[4]));
          if (parts.length >= 6) {
            DetailedTrack reg = (DetailedTrack) title;
            reg.setAlbum(StringUtils.trimToNull(parts[5]));
            reg.setGenre(StringUtils.trimToNull(parts[6]));
            reg.setYear(Integer.parseInt(parts[7]));
            long uploadDate = Long.parseLong(parts[8]);
            reg.setUploadDate(uploadDate > 0 ? new Date(uploadDate) : null);
            reg.setPrivateTrack(parts[9].equalsIgnoreCase("true"));
            if (parts.length >= 11) {
              reg.setOwnTitle(parts[10].equalsIgnoreCase("true"));
            }
          }
        }
        return title;
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  /**
   * @see de.stationadmin.base.track.format.TrackExportFormat#supports(java.lang.String)
   */
  @Override
  public boolean supports(String str) {
    return this.fromString(str) != null;
  }

  /**
   * @see de.stationadmin.base.track.format.TrackExportFormat#toString(de.stationadmin.base.track.Title)
   */
  @Override
  public String toString(Title title) {
    String str = title.toTabSeparatedValues();
    if (this.writeDetailedData && title instanceof DetailedTrack) {
      DetailedTrack reg = (DetailedTrack) title;
      StringBuffer buf = new StringBuffer(str);

      buf.append(StringUtils.trimToEmpty(reg.getAlbum()));
      buf.append('\t');
      buf.append(StringUtils.trimToEmpty(reg.getGenre()));
      buf.append('\t');
      buf.append(Integer.toString(reg.getYear()));
      buf.append('\t');
      buf.append(reg.getUploadDate() != null ? Long.toString(reg.getUploadDate().getTime()) : "0");
      buf.append('\t');
      buf.append(Boolean.toString(reg.isPrivateTrack()));
      buf.append('\t');
      buf.append(Boolean.toString(reg.isOwnTitle()));
      buf.append('\t');

      str = buf.toString();
    }
    return str;
  }

}
