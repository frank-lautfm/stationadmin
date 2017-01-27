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
  int MINOR = 2;
  int STEP = 1;
  String STATUS = "";
  
  String VERSION = MAJOR + "." + MINOR + "." + STEP + " " + STATUS;

}
