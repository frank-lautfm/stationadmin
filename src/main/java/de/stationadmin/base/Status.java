/**
 * 
 */
package de.stationadmin.base;

public class Status {
  private String key;
  private String[] parameters;

  public Status(String key, String... parameters) {
    this.key = key;
    this.parameters = parameters;
  }

  public String getKey() {
    return key;
  }

  public String[] getParameters() {
    return parameters;
  }

}