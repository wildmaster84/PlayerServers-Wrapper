package me.wild;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Utils {
  Main main;
  
  private Logger logger;
  
  public Utils(Logger logger) {
    this.logger = logger;
    this.main = Main.getInstance();
    formatLogs();
  }
  
  public void formatLogs() {
    try {
      FileHandler handler = new FileHandler("PSWrapper.log");
      this.logger.addHandler(handler);
      SimpleFormatter form = new SimpleFormatter();
      handler.setFormatter(form);
    } catch (IOException e) {
      e.printStackTrace();
    } 
  }
  
  public void log(String message) {
    this.logger.info("[PSWrapperV2] " + message);
    if (this.main.debug)
      System.out.println("[PSWrapperV2] " + message); 
  }
  
  public void log(Level level, String message) {
    this.logger.log(level, "[PSWrapperV2] " + message);
    if (this.main.debug)
      System.out.println("[PSWrapperV2] [" + level.getLocalizedName() + "] " + message); 
  }
  
  public void print(String message) {
    this.logger.info("[PSWrapperV2] " + message);
    if (this.main.debug)
      System.out.println("[PSWrapperV2] " + message); 
  }
}
