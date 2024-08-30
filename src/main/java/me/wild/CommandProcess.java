package me.wild;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelFutureListener;

class CommandProcess {
    private Main main;
    private List<String> running = new ArrayList<>();
    private ExecutorService commandExecutor;

    public CommandProcess() {
        this.main = Main.getInstance();
        this.commandExecutor = Executors.newFixedThreadPool(5); // Adjust thread pool size as needed
    }

    public void runCmd(String input, ChannelHandlerContext ctx) throws IOException {
        if (input == null || input.isEmpty()) {
            ctx.writeAndFlush("Input is null\n");
            return;
        }

        String[] args = input.split("\\s");
        if (args.length < 1) {
            ctx.writeAndFlush("Command required\n");
            return;
        }
        if (args.length < 2) {
            ctx.writeAndFlush("Server required\n");
            return;
        }

        String command = args[0];
        String server = args[1];

        switch (command) {
            case "+command":
                handleCommand(args, server, ctx);
                break;
            case "+restart":
                handleRestart(server, ctx);
                break;
            case "+stopall":
                handleStopAll(ctx);
                break;
            case "+exit":
                handleExit(ctx);
                break;
            case "+kill":
                handleKill(server, ctx);
                break;
            case "+stop":
                handleStop(server, ctx);
                break;
            case "+test":
                ctx.writeAndFlush("Test Success!\n");
                break;
            case "+start":
                handleStart(args, server, ctx);
                break;
            default:
                ctx.writeAndFlush(String.format("Invalid command! %s\n", command));
                break;
        }
    }

    private void handleCommand(String[] args, String server, ChannelHandlerContext ctx) throws IOException {
        if (args.length < 3) {
            ctx.writeAndFlush("You must specify a command to forward to " + server + "\n");
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
            ctx.writeAndFlush("Server is not running!\n");
        }
    }

    private void handleRestart(String server, ChannelHandlerContext ctx) {
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
                    ctx.writeAndFlush("Server restarted!\n");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    ctx.writeAndFlush("Server restart interrupted!\n");
                }
            });
        } else {
            ctx.writeAndFlush("Tried to restart " + server + ", but it was not online!\n");
        }
    }

    private void handleStopAll(ChannelHandlerContext ctx) {
        this.main.ut.log("Stopping all servers.");
        for (ManagedServer srv : this.main.managedServers.values()) {
            this.main.ut.log("Stopping \"" + srv.getServerName() + "\"");
            srv.stop();
            this.running.remove(srv.getServerName());
        }
        ctx.writeAndFlush("All servers stopped!\n");
    }

    private void handleExit(ChannelHandlerContext ctx) {
        ctx.writeAndFlush("Shutting down PSWrapper.\n").addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                // After successful write and flush, exit the application
                System.exit(0);
            } else {
                // Handle the error if writing failed
                future.cause().printStackTrace();
            }
        });
    }

    private void handleKill(String server, ChannelHandlerContext ctx) {
        this.main.ut.log("Killing server: " + server);
        if (this.running.contains(server)) {
            ManagedServer manServer = this.main.managedServers.get(server);
            manServer.getProcess().destroyForcibly();
            this.running.remove(server);
            this.main.managedServers.remove(server);
            ctx.writeAndFlush("Server killed!\n");
        } else {
            ctx.writeAndFlush("Tried to kill " + server + ", but it was not online!\n");
        }
    }

    private void handleStop(String server, ChannelHandlerContext ctx) {
        if (this.running.contains(server)) {
            this.main.ut.log("Stopping server: " + server);
            ManagedServer manServer = this.main.managedServers.get(server);
            manServer.stop();
            this.running.remove(server);
            this.main.managedServers.remove(server);
            this.main.ut.log("Stopped server: " + server);
            ctx.writeAndFlush("Stopped server: " + server + "\n");
        } else {
            if (this.main.managedServers.get(server) != null) {
                this.main.ut.log("Stopping server: " + server);
                ManagedServer manServer = this.main.managedServers.get(server);
                manServer.stop();
                this.running.remove(server);
                this.main.managedServers.remove(server);
                this.main.ut.log("Stopped server: " + server);
                ctx.writeAndFlush("Stopped server: " + server + "\n");
            } else {
                ctx.writeAndFlush("That server is not online! " + server + "\n");
            }
        }
    }

    private void handleStart(String[] args, String server, ChannelHandlerContext ctx) {
        if (args.length < 6) {
            ctx.writeAndFlush("Invalid arguments! Arguments required: server name, server path, max memory, starting memory, and jar file name.\n");
            return;
        }
        if (!this.running.contains(server)) {
            this.running.add(server);
            this.main.ut.log("Starting server: " + server);
            this.main.managedServers.put(server, new ManagedServer(server, args[2].replace("\\/", " "), args[3], args[4], args[5], args[6], args[7]));
            ctx.writeAndFlush("Server started!\n");
        } else if (!this.main.managedServers.containsKey(server) && this.running.contains(server)) {
            this.running.remove(server);
            this.main.managedServers.remove(server);
            this.main.ut.log("Starting server: " + server);
            this.main.managedServers.put(server, new ManagedServer(server, args[2], args[3], args[4], args[5], args[6], args[7]));
            ctx.writeAndFlush("Server started!\n");
        } else {
            ctx.writeAndFlush("Server is already starting! Server: " + server + "\n");
        }
    }
}
