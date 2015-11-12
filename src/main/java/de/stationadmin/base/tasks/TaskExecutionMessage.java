/**
 * 
 */
package de.stationadmin.base.tasks;

/**
 * @author korf
 *
 */
public class TaskExecutionMessage {
  private boolean error;
  private String key;
  private String[] parameters;
  
  public TaskExecutionMessage() {
  }
  
  public TaskExecutionMessage(boolean error, String key, String... parameters) {
    super();
    this.error = error;
    this.key = key;
    this.parameters = parameters;
  }

  public boolean isError() {
    return error;
  }

  public String getKey() {
    return key;
  }

  public String[] getParameters() {
    return parameters;
  }

}
