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
  int MINOR = 11;
  int STEP = 0;
  String STATUS = "Beta 5";

  String VERSION = MAJOR + "." + MINOR + "." + STEP + " " + STATUS;
  
  int NUMBER = 41001;

}
