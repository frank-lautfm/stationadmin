/**
 * 
 */
package de.stationadmin.base.mp3splitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author korf
 * 
 */
public class MixcloudTitleListReader {

  public static List<SplitPoint> read(String mixUrl) throws IOException, JSONException {
    // http://www.mixcloud.com/fkorf/emjoy-horizons-4/
    String apiUrl = StringUtils.replace(mixUrl, "www", "api");
    JSONObject obj = get(apiUrl);

    List<SplitPoint> splits = new ArrayList<SplitPoint>();
    JSONArray arr = obj.getJSONArray("sections");
    for(int i = 0; i < arr.length(); i++) {
      JSONObject section = arr.getJSONObject(i);
      int start = section.getInt("start_time");
      JSONObject track = section.getJSONObject("track");
      String title = track.getString("name");
      JSONObject artistObj = track.getJSONObject("artist");
      String artist = artistObj.getString("name"); 
      SplitPoint p = new SplitPoint(start * 1000, artist, title, null);
      splits.add(p);
    }


    return splits;
  }

  private static JSONObject get(String apiUrl) throws IOException, JSONException {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpGet get = new HttpGet(apiUrl);
    HttpResponse response = client.execute(get);

    return new JSONObject(IOUtils.toString(response.getEntity().getContent()));

  }
}
