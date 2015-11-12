/**
 * 
 */
package de.stationadmin.lfmapi;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Frank Korf
 * 
 */
public class JSONUtil {
  public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss ZZZZZ";

  /**
   * Gets an int value from a JSON object or returns the default value if
   * the field was not present
   * @param obj
   * @param field
   * @param defaultValue
   * @return
   */
  public static int getInt(JSONObject obj, String field, int defaultValue) {
    try {
      return obj.getInt(field);
    } catch (JSONException e) {
      return defaultValue;
    }
  }

  public static boolean getBoolean(JSONObject obj, String field) {
    try {
      return obj.getBoolean(field);
    } catch (JSONException e) {
      return false;
    }
  }

  public static String getString(JSONObject obj, String field) {
    try {
      return obj.getString(field);
    } catch (JSONException e) {
      return null;
    }
  }

  public static Date getDate(JSONObject obj, String field, String fmt) {
    try {
      String date = obj.getString(field);
      if (date != null) {
        SimpleDateFormat df = new SimpleDateFormat(fmt);
        return df.parse(date);
      }
    } catch (Exception e) {
    }
    return null;

  }

  public static JSONObject getObject(JSONObject obj, String field) {
    try {
      return obj.getJSONObject(field);
    } catch (JSONException e) {
      return null;
    }
  }

}
