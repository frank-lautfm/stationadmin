/**
 * 
 */
package de.stationadmin.gui.logs;

/**
 * @author korf
 *
 */
public enum LogLevel {
   INFO("info"),
   WARN("warning"),
   ERROR("error");
   
   private String internalName;
   
   private LogLevel(String internalName) {
     this.internalName = internalName;
   }
   
   public static LogLevel fromInternal(String name) {
     for(LogLevel level : values()) {
       if(name.equals(level.getInternalName())) {
         return level;
       }
     }
     return null;
   }

  /**
   * @return the internalName
   */
  public String getInternalName() {
    return internalName;
  }
   
}
