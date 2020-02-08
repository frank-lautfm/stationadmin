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

  int MAJOR = 5;
  int MINOR = 0;
  int STEP = 0;
  String STATUS = "Beta 3";

  String VERSION = MAJOR + "." + MINOR + "." + STEP + " " + STATUS;
  
  int NUMBER = 5000;

}
