package de.stationadmin.base.util;

public class FileUtils {

  public static String normalizeFilename(String name) {
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (c == '*' || c == ':' || c == '\\' || c == '/' || c == '?') {
        buf.append("x" + Integer.toHexString(c));

      } else {
        buf.append(c);
      }
    }
    return buf.toString();
  }
}
