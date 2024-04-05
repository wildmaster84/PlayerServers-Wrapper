package me.wild;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;

class ProcessWatcher implements Runnable {
  private Main main;
  
  private Process proc;
  
  private OutputStream out;
  
  private ManagedServer server;
  
  private boolean shutdown;
  
  private int frozen;
  
  public ProcessWatcher(ManagedServer server, Process proc, OutputStream out) {
    this.shutdown = false;
    this.frozen = 0;
    this.main = Main.getInstance();
    this.server = server;
    this.proc = proc;
    this.out = out;
  }
  
  private void processCheck() {
    boolean enable = true;
    if (this.out != null && enable)
      try {
        this.out.write(13);
        this.out.flush();
      } catch (IOException e) {
        this.main.ut.log(Level.WARNING, "Server \"" + this.server.getServerName() + "\" may be stopped or frozen.");
        this.frozen++;
      }  
  }
  
  public void run() {
    while (true) {
      processCheck();
      if (!this.proc.isAlive()) {
        if (this.frozen > 1) {
          this.main.ut.log(Level.WARNING, "Server \"" + this.server.getServerName() + "\" stopped or frozen! Shutting down.");
          this.shutdown = true;
        } 
        if (!this.main.managedServers.containsValue(this.server))
          this.shutdown = true; 
        this.frozen++;
      } else if (this.frozen > 0) {
        this.frozen--;
      } 
      try {
        this.proc.exitValue();
        this.out.close();
        this.shutdown = true;
      } catch (IllegalThreadStateException illegalThreadStateException) {
      
      } catch (IOException iOException) {}
      if (this.shutdown)
        break; 
      try {
        Thread.sleep(10000L);
      } catch (InterruptedException e) {
        e.printStackTrace();
      } 
    } 
    this.main.ut.log(String.valueOf(this.server.getServerName()) + " shut down.");
    if (this.main.managedServers.containsValue(this.server)) {
      this.main.managedServers.remove(this.server.getServerName());
      this.main.ut.log("managedServers: " + this.main.managedServers.toString());
    } 
    this.server.killThreads();
    try {
      this.server.in.close();
      this.server.out.close();
      this.server.errors.close();
    } catch (IOException iOException) {}
  }
}
