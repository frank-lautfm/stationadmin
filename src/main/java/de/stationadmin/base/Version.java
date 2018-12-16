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
  int MINOR = 10;
  int STEP = 0;
  String STATUS = "DEV";

  String VERSION = MAJOR + "." + MINOR + "." + STEP + " " + STATUS;
  
  int NUMBER = 40900;

}
