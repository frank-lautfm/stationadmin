/**
 * 
 */
package de.stationadmin.base.mp3splitter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * Tries to extract titles from a string in format
 * <p>
 * 1. Artist 1 - Title 1
 * 2. Artist 2 - Title 2
 * 3. Artist 2 - Title 3
 * 
 * @author Frank
 */
public class DJMixTitleListParser {

  public static List<SplitPoint> parse(String str) {
    Pattern patternSimple = Pattern.compile("(\\d+[\\.\\)]){0,1}\\s*(.*?)\\s+-\\s+(.*?)\\s*", Pattern.DOTALL);
    Pattern patternTime = Pattern.compile("([\\d\\:]{2,})\\s*(.*?)\\s+-\\s+(.*?)\\s*", Pattern.DOTALL);

    List<SplitPoint> list = new ArrayList<SplitPoint>();
    String[] lines = StringUtils.split(str, "\r\n");
    for (String line : lines) {
      line = line.trim();
      if (line.length() > 0) {
        Matcher matcherSimple = patternSimple.matcher(line);
        Matcher matcherTime = patternTime.matcher(line);
        SplitPoint title = new SplitPoint();
        if (matcherTime.matches()) {
          try {
            title.setPosition(parseTime(matcherTime.group(1)));
          } catch (Exception e) {
          }
          title.setArtist(matcherTime.group(2));
          title.setTitle(matcherTime.group(3));
        } else if (matcherSimple.matches()) {
          title.setArtist(matcherSimple.group(2));
          title.setTitle(matcherSimple.group(3));
        } else {
          title.setArtist(line);
        }
        list.add(title);
      }
    }

    return list;
  }

  public static long parseTime(String str) {
    String[] parts = StringUtils.split(str, ":");
    if (parts.length > 3) {
      throw new NumberFormatException();
    }
    int minutes = Integer.parseInt(parts[0]);
    int seconds = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
    int ms = 0;
    if (parts.length == 3) {
      ms = Integer.parseInt(parts[2]);
      if (ms < 10) {
        ms = ms * 100;
      } else if (ms < 100) {
        ms = ms * 10;
      }
    }

    return (minutes * 60 + seconds) * 1000;

  }

}
