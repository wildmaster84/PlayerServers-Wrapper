package me.wild;

import java.io.*;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

class ProcessWatcher implements Runnable {
    private Main main;
    private Process proc;
    private ManagedServer server;
    private boolean shutdown;
    private int frozen;

    public ProcessWatcher(ManagedServer server, Process proc) {
        this.shutdown = false;
        this.frozen = 0;
        this.main = Main.getInstance();
        this.server = server;
        this.proc = proc;
    }

    private void processCheck() {
        try {
            if (proc.isAlive()) {
                // Send a no-op command to check if the process is responsive
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
                writer.write(13); // Sending a carriage return as a no-op
                writer.flush();
            }
        } catch (IOException e) {
            main.ut.log(Level.WARNING, "Server \"" + server.getServerName() + "\" may be stopped or frozen.");
            frozen++;
        }
    }

    @Override
    public void run() {
        while (true) {
            processCheck();
            if (!proc.isAlive()) {
                if (frozen > 5) {
                    main.ut.log(Level.WARNING, "Server \"" + server.getServerName() + "\" stopped or frozen! Shutting down.");
                    shutdown = true;
                }
                if (!main.managedServers.containsValue(server)) {
                    shutdown = true;
                }
                frozen++;
            } else if (frozen > 0) {
                frozen--;
            }

            try {
                proc.exitValue();
                shutdown = true;
            } catch (IllegalThreadStateException ignored) {
                // Process is still running
            }

            if (shutdown) {
                break;
            }

            try {
                Thread.sleep(10000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        main.ut.log(server.getServerName() + " shut down.");
        if (main.managedServers.containsValue(server)) {
            main.managedServers.remove(server.getServerName());
            main.ut.log("managedServers: " + main.managedServers.toString());
        }
        server.killThreads();
    }
}
