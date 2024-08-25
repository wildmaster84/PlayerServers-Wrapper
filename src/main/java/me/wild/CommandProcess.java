package me.wild;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class CommandProcess {
    private Main main;
    private List<String> running = new ArrayList<>();
    private ExecutorService commandExecutor;

    public CommandProcess() {
        this.main = Main.getInstance();
        this.commandExecutor = Executors.newFixedThreadPool(5); // Adjust thread pool size as needed
    }

    public void runCmd(String input) throws IOException {
        if (input == null || input.isEmpty()) {
            this.main.ut.log("input null");
            return;
        }

        String[] args = input.split("\\s");
        if (args.length < 1) {
            this.main.ut.log("command required");
            return;
        }
        if (args.length < 2) {
            this.main.ut.log("server required");
            return;
        }

        String command = args[0];
        String server = args[1];

        switch (command) {
            case "+command":
                handleCommand(args, server);
                break;
            case "+restart":
                handleRestart(server);
                break;
            case "+stopall":
                handleStopAll();
                break;
            case "+exit":
                handleExit();
                break;
            case "+kill":
                handleKill(server);
                break;
            case "+stop":
                handleStop(server);
                break;
            case "+test":
                this.main.ut.log("Test Success!");
                break;
            case "+start":
                handleStart(args, server);
                break;
            default:
                this.main.ut.log(String.format("Invalid command! %s", command));
                break;
        }
    }

    private void handleCommand(String[] args, String server) throws IOException {
        if (args.length < 3) {
            this.main.ut.log("You must specify a command to forward to " + server);
            return;
        }
        if (this.running.contains(server)) {
            StringBuilder str = new StringBuilder();
            for (int i = 2; i < args.length; i++) { // Start at index 2 to skip command and server name
                str.append(args[i]).append(" ");
            }
            ManagedServer manServer = this.main.managedServers.get(server);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(manServer.getProcess().getOutputStream()));
            writer.write(str.toString());
            writer.flush();
        }
    }

    private void handleRestart(String server) {
        this.main.ut.log("Restarting server: " + server);
        if (this.running.contains(server)) {
            ManagedServer manServer = this.main.managedServers.get(server);
            List<String> cmd = manServer.getCommand();
            String path = manServer.getPath();
            this.main.ut.log("Restarting " + server + " in 10 seconds...");
            manServer.stop();
            this.running.remove(server);
            this.main.managedServers.remove(server);

            commandExecutor.submit(() -> {
                try {
                    Thread.sleep(10000L);
                    this.main.managedServers.put(server, new ManagedServer(cmd, server, path));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        } else {
            this.main.ut.log("Tried to restart " + server + ", but it was not online!");
        }
    }

    private void handleStopAll() {
        this.main.ut.log("Stopping all servers.");
        for (ManagedServer srv : this.main.managedServers.values()) {
            this.main.ut.log("Stopping \"" + srv.getServerName() + "\"");
            srv.stop();
            this.running.remove(srv.getServerName());
        }
    }

    private void handleExit() {
        this.main.ut.log("Shutting down PSWrapper.");
        System.exit(0);
    }

    private void handleKill(String server) {
        this.main.ut.log("Killing server: " + server);
        if (this.running.contains(server)) {
            ManagedServer manServer = this.main.managedServers.get(server);
            manServer.getProcess().destroyForcibly();
            this.running.remove(server);
            this.main.managedServers.remove(server);
        } else {
            this.main.ut.log("Tried to kill " + server + ", but it was not online!");
        }
    }

    private void handleStop(String server) {
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
    }

    private void handleStart(String[] args, String server) {
        if (args.length < 6) {
            this.main.ut.log("Invalid arguments! Arguments required: server name, server path, max memory, starting memory, and jar file name.");
            return;
        }
        if (!this.running.contains(server)) {
            this.running.add(server);
            this.main.ut.log("Starting server: " + server);
            this.main.managedServers.put(server, new ManagedServer(server, args[2], args[3], args[4], args[5], args[6], args[7]));
        } else if (!this.main.managedServers.containsKey(server) && this.running.contains(server)) {
            this.running.remove(server);
            this.main.managedServers.remove(server);
            this.main.ut.log("Starting server: " + server);
            this.main.managedServers.put(server, new ManagedServer(server, args[2], args[3], args[4], args[5], args[6], args[7]));
        } else {
            this.main.ut.log("Server is already starting! Server: " + server);
        }
    }
}

