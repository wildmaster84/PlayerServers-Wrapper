package me.wild;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class CommandProcess {
  private Main main;
  
  private List<String> running = new ArrayList<>();
  
  public CommandProcess() {
    this.main = Main.getInstance();
  }
  
  public void runCmd(String input) throws IOException {
    if (input == null || input.isEmpty()) {
      this.main.ut.log("input null");
      return;
    } 
    String[] args = input.split("\\s");
    if (args.length < 1) {
      this.main.ut.log("command required");
    } else if (args.length < 2) {
      this.main.ut.log("server required");
    } else {
      String command = args[0];
      final String server = args[1];
      String s = command;
      String str1;
      switch ((str1 = s).hashCode()) {
        case -1418172224:
          if (!str1.equals("+command"))
            break; 
          if (args.length < 3) {
            this.main.ut.log("You must specify a command to forward to " + server);
            return;
          } 
          return;
        case -1271060604:
          if (!str1.equals("+restart"))
            break; 
          this.main.ut.log("Restarting server: " + server);
          if (this.running.contains(server)) {
            ManagedServer manServer = this.main.managedServers.get(server);
            List<String> cmd = manServer.getCommand();
            String path = manServer.getPath();
            this.main.ut.log("Restarting " + server + " in 10 seconds...");
            manServer.stop();
              this.running.remove(server);
              this.main.managedServers.remove(server); 
            (new Thread(new Runnable() {
            	@Override
                  public void run() {
                    try {
                      Thread.sleep(10000L);
                    } catch (InterruptedException e) {
                      e.printStackTrace();
                    } 
                    main.managedServers.put(server, new ManagedServer(cmd, server, path));
                  }
                })).start();
          } else {
            this.main.ut.log("Tried to restart " + server + ", but was not online!");
          } 
          return;
        case -388649433:
          if (!str1.equals("+sendcmd"))
            break; 
          if (args.length < 3) {
            this.main.ut.log("You must specify a command to forward to " + server);
            return;
          } 
          return;
        case 1432:
          if (!str1.equals("+c"))
            break; 
          if (args.length < 3) {
            this.main.ut.log("You must specify a command to forward to " + server);
            return;
          } 
          return;
        case 1379631:
          if (!str1.equals("+cmd"))
            break; 
          if (args.length < 3) {
            this.main.ut.log("You must specify a command to forward to " + server);
            return;
          } 
          return;
        case 23718259:
          if (!str1.equals("+sendcommand+"))
            break; 
          if (args.length < 3) {
            this.main.ut.log("You must specify a command to forward to " + server);
            return;
          } 
          return;
        case 42066900:
          if (!str1.equals("+stopall"))
            break; 
          this.main.ut.log("Stopping all servers.");
          for (ManagedServer srv : this.main.managedServers.values()) {
            this.main.ut.log("Stopping \"" + srv.getServerName() + "\"");
            ManagedServer manServer = this.main.managedServers.get(server);
            manServer.stop();
            this.running.remove(srv.getServerName());
          } 
          return;
        case 42838985:
          if (!str1.equals("+exit"))
            break; 
          this.main.ut.log("Shutting down PSWrapper.");
          System.exit(0);
          return;
        case 43003401:
          if (!str1.equals("+kill"))
            break; 
          this.main.ut.log("Killing server: " + server);
          if (this.running.contains(server)) {
            ManagedServer manServer = this.main.managedServers.get(server);
            manServer.getProcess().destroyForcibly();
            this.running.remove(server);
            this.main.managedServers.remove(server);
          } else {
            this.main.ut.log("Tried to kill " + server + ", but was not online!");
          } 
          return;
        case 43252397:
          if (!str1.equals("+stop"))
            break; 
          if (this.running.contains(server)) {
            this.main.ut.log("Stopping server: " + server);
            ManagedServer manServer = this.main.managedServers.get(server);
            manServer.stop();
            this.running.remove(server);
            this.main.managedServers.remove(server); 
            this.main.ut.log("Stopped server: " + server);
          } else {
            if (this.main.managedServers.get(server) != null) {
              this.main.ut.log("Stopping server: " + server);
              ManagedServer manServer = this.main.managedServers.get(server);
              manServer.stop();
			  this.running.remove(server);
			  this.main.managedServers.remove(server); 
              this.main.ut.log("Stopped server: " + server);
            } 
            this.main.ut.log("That server is not online! " + server);
          } 
          return;
        case 43267901:
          if (!str1.equals("+test"))
            break; 
          this.main.ut.log("Test Success!");
          return;
        case 1340811031:
          if (!str1.equals("+start"))
            break; 
          if (args.length < 6) {
            this.main.ut.log("Invalid arguments! Arguments required: server name, server path, max memory, starting memory, and jar file name.");
            return;
          } 
          if (!this.running.contains(server)) {
            this.running.add(server);
            this.main.ut.log("Starting server: " + server);
            this.main.managedServers.put(server, new ManagedServer(server, args[2], args[3], args[4], args[5]));
            return;
          } 
          if (!this.main.managedServers.containsKey(server) && this.running.contains(server)) {
        	  this.running.remove(server);
        	  this.main.managedServers.remove(server);
        	  this.main.ut.log("Starting server: " + server);
              this.main.managedServers.put(server, new ManagedServer(server, args[2], args[3], args[4], args[5]));
        	  return;
          }
          main.ut.log("Server is already starting! Server: " + server);
          return;
      } 
      main.ut.log(String.format("Invalid command! %s %s", str1, s));
    } 
  }
}
