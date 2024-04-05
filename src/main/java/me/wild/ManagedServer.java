package me.wild;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ManagedServer implements Runnable {
  private Main main;
  
  private ProcessBuilder procBuild;
  
  private Process proc;
  
  OutputStream out;
  
  InputStream in;
  
  InputStream errors;
  
  private List<String> command;
  
  private String serverName;
  
  private String path;
  
  private String memx;
  
  private String mems;
  
  private String jarFile;
  
  private ConsoleWatcher console;
  
  private ConsoleWatcher err;
  
  public ManagedServer(String serverName, String path, String memx, String mems, String jarFile) {
    this.proc = null;
    this.main = Main.getInstance();
    this.serverName = serverName;
    this.path = path;
    this.memx = memx;
    this.mems = mems;
    this.jarFile = jarFile;
    prepare();
    run();
  }
  
  public ManagedServer(List<String> command, String serverName, String path) {
    this.proc = null;
    this.command = command;
    this.serverName = serverName;
    this.path = path;
    this.main.ut.log("command: " + this.command);
    this.main.ut.log("path: " + this.path);
    this.procBuild = (new ProcessBuilder(command)).directory(new File(path));
  }
  
  private void prepare() {
    (this.command = new ArrayList<>()).add("java");
    if (this.memx.matches("(\\-Xmx)(.*)")) {
      this.command.add(this.memx);
    } else {
      this.command.add("-Xmx" + this.memx);
    } 
    if (this.mems.matches("(\\-Xms)(.*)")) {
      this.command.add(this.mems);
    } else {
      this.command.add("-Xms" + this.mems);
    } 
    if (this.main.isWindows)
      this.command.add("-Djline.terminal=jline.UnsupportedTerminal"); 
    if (!this.jarFile.matches("(?i)(paperspigot).+"))
      this.command.add("-Dcom.mojang.eula.agree=true"); 
    this.command.add("-jar");
    this.command.add(this.jarFile);
    if (this.main.isWindows)
      this.command.add("--nojline"); 
    if (!this.main.debug)
    	this.command.add("--nogui"); 
    this.main.ut.log("command: " + this.command);
    this.main.ut.log("path: " + this.path);
    this.procBuild = (new ProcessBuilder(this.command)).directory(new File(this.path));
  }
  
  private void startup() {
    try {
      this.proc = this.procBuild.start();
      this.in = this.proc.getInputStream();
      this.errors = this.proc.getErrorStream();
      this.out = this.proc.getOutputStream();
      this.main.serverProcesses.add(this.proc);
      this.main.ut.log("Starting server within PSWrapperV2");
      this.console = new ConsoleWatcher("in", this.in);
      this.err = new ConsoleWatcher("error", this.errors);
      (new Thread(new ProcessWatcher(this, this.proc, this.out))).start();
    } catch (IOException e) {
      e.printStackTrace();
    } 
  }
  
  public void killThreads() {
    this.console.killIt();
    this.err.killIt();
  }
  @Override
  public void run() {
    this.main.ut.log("starting up...");
    startup();
  }
  
  public void kill() {
    killThreads();
    this.proc.destroy();
    this.main.ut.log("Server '" + this.serverName + "' successfully killed.");
  }
  
  public void stop() {
      try {
    	  this.getOutputStream().write("stop".getBytes());
		  this.getOutputStream().close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }
  
  public Process getProcess() {
    return this.proc;
  }
  
  public InputStream getInputStream() {
    return this.in;
  }
  
  public OutputStream getOutputStream() {
    return this.out;
  }
  
  public InputStream getErrorStream() {
    return this.errors;
  }
  
  public String getPath() {
    return this.path;
  }
  
  public void setPath(String path) {
    this.path = path;
  }
  
  public List<String> getCommand() {
    return this.command;
  }
  
  public void setCommand(List<String> command) {
    this.command = command;
  }
  
  public int getMaxMem() {
    return 0;
  }
  
  public int getStartMem() {
    return 0;
  }
  
  public String getJarFile() {
    return this.jarFile;
  }
  
  public String getServerName() {
    return this.serverName;
  }
}
