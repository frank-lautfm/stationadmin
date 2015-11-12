/**
 * 
 */
package de.stationadmin.gui.upload.mix;

import java.text.NumberFormat;

import org.apache.commons.lang.StringUtils;

import de.stationadmin.base.mp3splitter.SplitPoint;

/**
 * Extension of {@link SplitPoint} that supports the position as string instead of
 * just number
 * 
 * @author Frank
 */
public class ExtSplitSpoint extends SplitPoint {
  private String positionAsString;

  public String getPositionAsString() {
    if (this.positionAsString == null) {

      long pos = this.getPosition();

      int ms = (int) (pos % 1000);
      int seconds = (int) (pos / 1000);
      int minutes = seconds / 60;
      seconds = seconds % 60;
      
      NumberFormat fmt = NumberFormat.getIntegerInstance();
      fmt.setMinimumIntegerDigits(2);
      
      StringBuilder buf = new StringBuilder();
      buf.append(fmt.format(minutes) + ":");
      buf.append(fmt.format(seconds) + ":");
      buf.append(fmt.format(ms / 100));

      this.positionAsString = buf.toString();
    }
    return this.positionAsString;
  }

  public void setPositionAsString(String str) throws NumberFormatException {
    String[] parts = StringUtils.split(str, ":");
    if(parts.length > 3) {
      throw new NumberFormatException();
    }
    int minutes = Integer.parseInt(parts[0]);
    int seconds = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
    int ms = 0;
    if(parts.length == 3) {
      ms = Integer.parseInt(parts[2]);
      if(ms < 10) {
        ms = ms * 100;
      }
      else if(ms < 100) {
        ms = ms * 10;
      }
    }

    this.setPosition((minutes * 60 + seconds) * 1000 + ms);
  }

  /* (non-Javadoc)
   * @see de.emjoy.stationadmin.base.mp3splitter.SplitPoint#setPosition(long)
   */
  @Override
  public void setPosition(long position) {
    this.positionAsString = null;
    super.setPosition(position);
  }

}
