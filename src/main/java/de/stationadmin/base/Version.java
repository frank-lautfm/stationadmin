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
  int MINOR = 1;
  int STEP = 0;
  String STATUS = "Test";
  
  String VERSION = MAJOR + "." + MINOR + "." + STEP + " " + STATUS;

}
