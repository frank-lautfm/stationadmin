/**
 * 
 */
package de.stationadmin.base.tasks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author korf
 * 
 */
public class TaskExecutionResult implements Serializable {
  private static final long serialVersionUID = 9075609020591446561L;
  private Date timestamp = new Date();
  private boolean succeeded = true;
  private List<TaskExecutionMessage> messages = new ArrayList<TaskExecutionMessage>();

  public Date getTimestamp() {
    return timestamp;
  }

  public boolean isSucceeded() {
    return succeeded;
  }

  public void setSucceeded(boolean succeeded) {
    this.succeeded = succeeded;
  }

  public List<TaskExecutionMessage> getMessages() {
    return messages;
  }

  public void addMessage(TaskExecutionMessage message) {
    this.messages.add(message);
    if(message.isError()) {
      this.succeeded = false;
    }
  }
  
  public void addMessage(boolean error, String key, String... parameters) {
    this.messages.add(new TaskExecutionMessage(error, key, parameters));
  }

}
