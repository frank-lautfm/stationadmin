package de.stationadmin.base.tools;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

public class StreamingServerResolver {

  public static String getStreamingServer(String station) {
    try {
      String cmd;
      
      String server = station + ".stream.laut.fm"; // best guess
      
      boolean followBefore = HttpURLConnection.getFollowRedirects();
      try {
        HttpURLConnection.setFollowRedirects(false);
        URL url = new URL("http://stream.laut.fm/" + station);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        if(con.getResponseCode() == HttpsURLConnection.HTTP_MOVED_TEMP) {
          Pattern pattern = Pattern.compile("\\/\\/(.*.stream.laut.fm)");
          Matcher matcher = pattern.matcher(con.getHeaderField("Location"));
          if(matcher.find()) {
            server = matcher.group(1);
          }
        }
      }
      catch(Exception e) {
      }
      finally {
        HttpURLConnection.setFollowRedirects(followBefore);
      }
      
      if (SystemUtils.IS_OS_WINDOWS) {
        // For Windows
        cmd = "ping -n 1 " + server;
      } else {
        // For Linux and OSX
        cmd = "ping -c 1 " + server;
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
