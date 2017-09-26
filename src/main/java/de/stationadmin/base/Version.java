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
  int MINOR = 3;
  int STEP = 1;
  String STATUS = "Alpha";

  String VERSION = MAJOR + "." + MINOR + "." + STEP + " " + STATUS;

}
