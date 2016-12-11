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
  String STATUS = "Beta 2016-12-11_01";
  
  String VERSION = MAJOR + "." + MINOR + "." + STEP + " " + STATUS;

}
