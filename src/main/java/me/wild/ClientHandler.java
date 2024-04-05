package me.wild;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;

public class ClientHandler implements Runnable {
    private Socket socket;
	private String command;
    private BufferedReader in;
    public boolean running = false;
    public long lastHeartbeat;
    private String clientIP;
	private Main main;
	private OutputStream out;
    
    public ClientHandler(Main main, Socket socket) throws IOException {
        this.command = "";
        this.lastHeartbeat = System.currentTimeMillis();
        this.running = true;
        this.main = main;
        this.socket = socket;
        this.clientIP = socket.getInetAddress().getHostAddress();
        this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        this.out = this.socket.getOutputStream();
        if (main.connections.containsKey(clientIP)) main.connections.remove(clientIP);
    	main.connections.put(clientIP, this);
    	main.ut.log("Control Client Connected. Client: " + clientIP);
    }
    
    @Override
    public void run() {
        while (this.running) {
        	try {
				this.command = this.in.readLine();
			} catch (IOException e) {}
        	
            if (this.command != null) {
			  if (this.command.startsWith("+heartbeat")) {
			    this.lastHeartbeat = System.currentTimeMillis();
			  } else {
				    this.main.ut.log("Received Input: " + this.command);
				    try {
				      this.main.cmd.runCmd(this.command);
				    } catch (IOException e) {
				      e.printStackTrace();
				    } 
			    } 
            } else {
				try {
				  Thread.sleep(2000L);
				} catch (InterruptedException e2) {
				  e2.printStackTrace();
				} 
            } 
        } 
    }
    
    public boolean checkHeartbeat() {
		return (System.currentTimeMillis() - lastHeartbeat > 32000L);
    }
    public void close() {
        try {
          this.socket.shutdownInput();
          this.socket.shutdownOutput();
          this.socket.close();
        } catch (IOException e) {
          e.printStackTrace();
        } 
        this.lastHeartbeat = 1L;
        this.running = false;
    }
    public Socket getSocket() {
        return this.socket;
      }
}