/**
 * 
 */
package de.stationadmin.base.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.Sun14ReflectionProvider;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * @author fkorf
 *
 */
public class XStreamFactory {
  
  /**
   * Creates a new instance of XStream with the default settings Station Admin needs
   * @return
   */
  public static XStream newXStream() {
    return new XStream(new Sun14ReflectionProvider(), new DomDriver("UTF-8"));
  }

}
