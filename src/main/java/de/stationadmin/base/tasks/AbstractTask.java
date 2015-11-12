/**
 * 
 */
package de.stationadmin.base.tasks;

/**
 * @author korf
 * 
 */
public abstract class AbstractTask implements Task {
  private String name;

  /**
   * @see de.stationadmin.base.tasks.Task#getName()
   */
  @Override
  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
