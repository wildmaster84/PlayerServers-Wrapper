package me.wild;



import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main implements Runnable {
    public HashMap<String, ClientHandler> connections = new HashMap<>();
    public HashMap<String, ManagedServer> managedServers = new HashMap<>();
    
    public ArrayList<Process> serverProcesses = new ArrayList<>();
	private ServerSocket serverSocket;
	private static Main server;
	int abandonTime = 2;
    public Utils ut;
	public boolean debug = false;

	CommandProcess cmd;

	public boolean isWindows = false;
	
    public Main(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }
    
    @Override
    public void run() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();  
                Thread clientThread = new Thread(new ClientHandler(server, clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void watchAbandon() {
        (new Thread(new Runnable() {
        	  @Override
              public void run() {
                int fails = 0;
                while (fails <= server.abandonTime * 4) {
                	if (server.connections.size() > 0) {
                        Iterator<String> iter = server.connections.keySet().iterator();
                        while (iter.hasNext()) {
                          ClientHandler listener = server.connections.get(iter.next());
                          Socket sock = listener.getSocket();
                          if (!sock.isConnected() || sock.isClosed() || listener.checkHeartbeat()) {
                            listener.close();
                            iter.remove();
                            Main.this.ut.log("Control client disconnected, removed.");
                          } 
                        } 
                    } 
                	
                	if (server.managedServers.size() < 1 && server.connections.size() < 1) {
                		server.ut.log(Level.WARNING, "No connections and no servers running, shutting down in " + (Main.this.abandonTime - fails / 4) + " more minute(s) without a connection.");
                		fails++;
                	} else {
                    fails = 0;
	                  } 
	                  try {
	                	  Thread.sleep(15000L);
	                  } catch (InterruptedException interruptedException) {}
                } 
                server.ut.log("Wrapper had no connection or servers for " + server.abandonTime + " minutes, shutting down.");
                System.exit(0);
              }
            })).start();
    }
    
    public static void main(String[] args) throws IOException {
    	int port = 0;
    	if (args.length < 1) {
        	System.out.println("[PSWrapperV2] [" + Level.SEVERE.getLocalizedName() + "] " + "Listen port must be specified!"); 
        	System.exit(1);
        } 
    	try {
            port = Integer.valueOf(args[0]).intValue();
          } catch (NumberFormatException e) {
        	  System.out.println("[PSWrapperV2] [" + Level.SEVERE.getLocalizedName() + "] " + "Specified port (" + args[0] + ") must be a number!"); 
              System.exit(1);
          }
    	if (port == 0) {
    		System.out.println("[PSWrapperV2] [" + Level.SEVERE.getLocalizedName() + "] " + "Port cannot be 0!"); 
            System.exit(1);
        }
    	String os = System.getProperty("os.name");
        String osVer = System.getProperty("os.version");
    	
        server = new Main(port);
        server.ut = new Utils(Logger.getLogger("PSWrapperV2"));
        server.ut.log("Operating System: " + os + " v" + osVer);
        if (os.matches("(?i)(.*)(windows)(.*)"))
            server.isWindows = true; 
        if (args[args.length - 1].equalsIgnoreCase("debug")) {
        	server.debug = true; 
        }
        safeShutdown();
        server.cmd = new CommandProcess();
        
        Thread serverThread = new Thread(server);
        serverThread.start();
        server.watchAbandon();
    }

	public static Main getInstance() {
		// TODO Auto-generated method stub
		return server;
	}
	
	private static void safeShutdown() {
	    Runtime.getRuntime().addShutdownHook(new Thread() {
	          public void run() {
	           doShutdown();
	          }
	    });
	}
	  
	private static void doShutdown() {
		server.ut.log(Level.SEVERE, "Wrapper shutting down, stopping running servers.");
	    for (ManagedServer server : server.managedServers.values()) {
	      try {
	        server.getOutputStream().write("stop".getBytes());
	        server.getOutputStream().close();
	      } catch (IOException e) {
	        e.printStackTrace();
	      } 
	    } 
	    while (server.managedServers.size() >= 1) {
	      try {
	        Thread.sleep(3000L);
	      } catch (InterruptedException interruptedException) {}
	    } 
	  }
}

