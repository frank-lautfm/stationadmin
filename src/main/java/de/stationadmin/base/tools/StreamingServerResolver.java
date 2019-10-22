package de.stationadmin.base.tools;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

public class StreamingServerResolver {

  public static String getStreamingServer(String station) {
    try {
      String cmd;
      if (SystemUtils.IS_OS_WINDOWS) {
        // For Windows
        cmd = "ping -n 1 " + station + ".stream.laut.fm";
      } else {
        // For Linux and OSX
        cmd = "ping -c 1 " + station + ".stream.laut.fm";
      }

      Process myProcess = Runtime.getRuntime().exec(cmd);
      myProcess.waitFor();
      String output = IOUtils.toString(myProcess.getInputStream(), "UTF-8");
      Pattern pattern = Pattern.compile("(\\w+).stream.laut.fm");
      Matcher matcher = pattern.matcher(output);
      if (matcher.find()) {
        return matcher.group(1);
      }
      return null;
    } catch (Exception e) {
      return null;
    }

  }

}
