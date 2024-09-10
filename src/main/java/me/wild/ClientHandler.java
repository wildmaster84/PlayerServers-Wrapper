package me.wild;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Main main;
    private final Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    public boolean running = false;
    public long lastHeartbeat;
    private final String clientIP;

    public ClientHandler(Main main, Socket socket) {
        this.main = main;
        this.socket = socket;
        this.clientIP = socket.getRemoteSocketAddress().toString();
        this.running = true;
        this.lastHeartbeat = System.currentTimeMillis();

        if (main.connections.containsKey(clientIP)) {
            main.connections.remove(clientIP);
        }
        main.connections.put(clientIP, this);
        main.ut.log("Control Client Connected. Client: " + clientIP);

        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            main.ut.log("Failed to initialize I/O streams for client: " + clientIP);
        }
    }

    @Override
    public void run() {
        try {
            String command;
            while ((command = in.readLine()) != null) {
                runCmd(command);
            }
        } catch (IOException e) {
            main.ut.log("Error reading from client: " + clientIP);
        } finally {
            close();
        }
    }

    public void runCmd(String command) {
        if (command.startsWith("+heartbeat")) {
            this.lastHeartbeat = System.currentTimeMillis();
        } else {
            this.main.ut.log("Received Input: " + command);
            try {
                this.main.cmd.runCmd(command, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean checkHeartbeat() {
        return (System.currentTimeMillis() - lastHeartbeat > 32000L);
    }

    public void close() {
        try {
            this.running = false;
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.lastHeartbeat = 1L;
        main.ut.log("Control Client Disconnected. Client: " + clientIP);
    }

    public Socket getSocket() {
        return this.socket;
    }

    public BufferedWriter getOut() {
        return this.out;
    }
}
