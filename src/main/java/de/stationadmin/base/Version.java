/**
 * 
 */
package de.stationadmin.base;

/**
 * Version number
 *
 * @author Frank Korf
 */
public interface Version {
  
  int MAJOR = 4;
  int MINOR = 0;
  int STEP = 0;
  String STATUS = "Preview 1";
  
  String VERSION = MAJOR + "." + MINOR + "." + STEP + " " + STATUS;

}
