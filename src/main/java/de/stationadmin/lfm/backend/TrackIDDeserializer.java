/**
 * 
 */
package de.stationadmin.lfm.backend;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

/**
 * @author korf
 *
 */
public class TrackIDDeserializer extends JsonDeserializer<Integer> {

  /* (non-Javadoc)
   * @see org.codehaus.jackson.map.JsonDeserializer#deserialize(org.codehaus.jackson.JsonParser, org.codehaus.jackson.map.DeserializationContext)
   */
  @Override
  public Integer deserialize(JsonParser jsonparser, DeserializationContext ctx) throws IOException, JsonProcessingException {
    
    String date = jsonparser.getText();
    
    try {
      return Integer.parseInt(date);
    }
    catch(NumberFormatException e) {
      // might have been an add
      return -1;
    }
  }

}
