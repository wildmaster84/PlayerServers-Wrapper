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

    public CommandProcess() {
        this.main = Main.getInstance();
    }

    public void runCmd(String input, ClientHandler clientHandler) throws IOException {
        if (input == null || input.isEmpty()) {
            sendResponse(clientHandler, "Input is null\n");
            return;
        }

        String[] args = input.split("\\s");
        if (args.length < 1) {
            sendResponse(clientHandler, "Command required\n");
            return;
        }
        if (args.length < 2) {
            sendResponse(clientHandler, "Server required\n");
            return;
        }

        String command = args[0];
        String server = args[1];

        switch (command) {
            case "+command":
                handleCommand(args, server, clientHandler);
                break;
            case "+restart":
                handleRestart(server, clientHandler);
                break;
            case "+stopall":
                handleStopAll(clientHandler);
                break;
            case "+exit":
                handleExit(clientHandler);
                break;
            case "+kill":
                handleKill(server, clientHandler);
                break;
            case "+stop":
                handleStop(server, clientHandler);
                break;
            case "+test":
                sendResponse(clientHandler, "Test Success!\n");
                break;
            case "+start":
                handleStart(args, server, clientHandler);
                break;
            default:
                sendResponse(clientHandler, String.format("Invalid command! %s\n", command));
                break;
        }
    }

    private void handleCommand(String[] args, String server, ClientHandler clientHandler) throws IOException {
        if (args.length < 3) {
            sendResponse(clientHandler, "You must specify a command to forward to " + server + "\n");
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
        } else {
            sendResponse(clientHandler, "Server is not running!\n");
        }
    }

    private void handleRestart(String server, ClientHandler clientHandler) {
        this.main.ut.log("Restarting server: " + server);
        if (this.running.contains(server)) {
            ManagedServer manServer = this.main.managedServers.get(server);
            List<String> cmd = manServer.getCommand();
            String path = manServer.getPath();
            this.main.ut.log("Restarting " + server + " in 10 seconds...");
            manServer.stop();
            this.running.remove(server);
            this.main.managedServers.remove(server);

            this.main.threadPool.submit(() -> {
                try {
                    Thread.sleep(10000L);
                    this.main.managedServers.put(server, new ManagedServer(cmd, server, path));
                    sendResponse(clientHandler, "Server restarted!\n");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    sendResponse(clientHandler, "Server restart interrupted!\n");
                }
            });
        } else {
            sendResponse(clientHandler, "Tried to restart " + server + ", but it was not online!\n");
        }
    }

    private void handleStopAll(ClientHandler clientHandler) {
        this.main.ut.log("Stopping all servers.");
        for (ManagedServer srv : this.main.managedServers.values()) {
            this.main.ut.log("Stopping \"" + srv.getServerName() + "\"");
            srv.stop();
            this.running.remove(srv.getServerName());
        }
        sendResponse(clientHandler, "All servers stopped!\n");
    }

    private void handleExit(ClientHandler clientHandler) {
        sendResponse(clientHandler, "Shutting down PSWrapper.\n");
        System.exit(0);
    }

    private void handleKill(String server, ClientHandler clientHandler) {
        this.main.ut.log("Killing server: " + server);
        if (this.running.contains(server)) {
            ManagedServer manServer = this.main.managedServers.get(server);
            manServer.getProcess().destroyForcibly();
            this.running.remove(server);
            this.main.managedServers.remove(server);
            sendResponse(clientHandler, "Server killed!\n");
        } else {
            sendResponse(clientHandler, "Tried to kill " + server + ", but it was not online!\n");
        }
    }

    private void handleStop(String server, ClientHandler clientHandler) {
        if (this.running.contains(server)) {
            this.main.ut.log("Stopping server: " + server);
            ManagedServer manServer = this.main.managedServers.get(server);
            manServer.stop();
            this.running.remove(server);
            this.main.managedServers.remove(server);
            this.main.ut.log("Stopped server: " + server);
            sendResponse(clientHandler, "Stopped server: " + server + "\n");
        } else {
            if (this.main.managedServers.get(server) != null) {
                this.main.ut.log("Stopping server: " + server);
                ManagedServer manServer = this.main.managedServers.get(server);
                manServer.stop();
                this.running.remove(server);
                this.main.managedServers.remove(server);
                this.main.ut.log("Stopped server: " + server);
                sendResponse(clientHandler, "Stopped server: " + server + "\n");
            } else {
                sendResponse(clientHandler, "That server is not online! " + server + "\n");
            }
        }
    }

    private void handleStart(String[] args, String server, ClientHandler clientHandler) {
        if (args.length < 6) {
            sendResponse(clientHandler, "Invalid arguments! Arguments required: server name, server path, max memory, starting memory, and jar file name.\n");
            return;
        }
        if (!this.running.contains(server)) {
            this.running.add(server);
            this.main.ut.log("Starting server: " + server);
            this.main.managedServers.put(server, new ManagedServer(server, args[2].replace("\\/", " "), args[3], args[4], args[5], args[6], args[7]));
            sendResponse(clientHandler, "Server started!\n");
        } else if (!this.main.managedServers.containsKey(server) && this.running.contains(server)) {
            this.running.remove(server);
            this.main.managedServers.remove(server);
            this.main.ut.log("Starting server: " + server);
            this.main.managedServers.put(server, new ManagedServer(server, args[2], args[3], args[4], args[5], args[6], args[7]));
            sendResponse(clientHandler, "Server started!\n");
        } else {
            sendResponse(clientHandler, "Server is already starting! Server: " + server + "\n");
        }
    }

    private void sendResponse(ClientHandler clientHandler, String message) {
        try {
            clientHandler.getOut().write(message);
            clientHandler.getOut().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
