/**
 * 
 */
package de.stationadmin.lfm.backend;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

/**
 * @author korf
 *
 */
public class CustomJsonDateDeserializer extends JsonDeserializer<Date> {
  @Override
  public Date deserialize(JsonParser jsonparser, DeserializationContext deserializationcontext) throws IOException, JsonProcessingException {

    String[] formats = { "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss ZZZZZ" };
    String date = jsonparser.getText();

    for (String format : formats) {
      try {
        return new SimpleDateFormat(format).parse(date);
      } catch (ParseException e) {
      }
    }
    return null;

  }

}