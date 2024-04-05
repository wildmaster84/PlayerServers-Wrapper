package me.wild;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Scanner;

class ConsoleWatcher {
  Main main;
  
  private InputStream in;
  
  private String name;
  
  private boolean stopped;
  
  public ConsoleWatcher(String name, InputStream in) {
    this.stopped = false;
    this.main = Main.getInstance();
    this.name = name;
    this.in = in;
    run();
  }
  
  public void killIt() {
    this.stopped = true;
  }
  
  private void run() {
    (new Thread(new Runnable() {
    	@Override
          public void run() {
            @SuppressWarnings("resource")
			Scanner scanner = new Scanner(new BufferedInputStream(ConsoleWatcher.this.in));
            while (!stopped) {
              String next;
              if (scanner.hasNext()) {
            	  next = scanner.nextLine();
                if (!main.debug)
                  continue; 
                if (name.equalsIgnoreCase("in")) {
                  main.ut.log(String.valueOf(name) + ":Server Log: " + next);
                  continue;
                } 
                if (name.equalsIgnoreCase("error")) {
                  main.ut.log("ERROR: " + next);
                  continue;
                } 
                main.ut.log(String.valueOf(name) + "/unknown: " + next);
                continue;
              } 
              try {
                Thread.sleep(1000L);
              } catch (InterruptedException interruptedException) {}
            } 
          }
        })).start();
  }
}
