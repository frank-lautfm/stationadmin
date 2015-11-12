/**
 * 
 */
package de.stationadmin.base.tasks;

import de.stationadmin.base.StationAdminClient;

/**
 * @author korf
 *
 */
public interface Task {
  
  String getName();
    
  TaskExecutionResult execute(StationAdminClient client);
}
