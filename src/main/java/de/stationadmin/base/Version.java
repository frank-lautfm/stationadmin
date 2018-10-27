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
  int MINOR = 8;
  int STEP = 0;
  String STATUS = "Preview";

  String VERSION = MAJOR + "." + MINOR + "." + STEP + " " + STATUS;

}
