package me.wild;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

public class ClientHandler {

    private final Main main;
    private final Channel channel;
    public boolean running = false;
    public long lastHeartbeat;
    private final String clientIP;

    public ClientHandler(Main main, ChannelHandlerContext ctx) {
        this.main = main;
        this.channel = ctx.channel();
        this.clientIP = ctx.channel().remoteAddress().toString();
        this.running = true;
        this.lastHeartbeat = System.currentTimeMillis();

        if (main.connections.containsKey(clientIP)) {
            main.connections.remove(clientIP);
        }
        main.connections.put(clientIP, this);
        main.ut.log("Control Client Connected. Client: " + clientIP);
    }

    public void runCmd(String command, ChannelHandlerContext ctx) {
        if (command.startsWith("+heartbeat")) {
            this.lastHeartbeat = System.currentTimeMillis();
        } else {
            this.main.ut.log("Received Input: " + command);
            try {
                this.main.cmd.runCmd(command, ctx);  // Pass the command and context to CommandProcess
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
            channel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.lastHeartbeat = 1L;
    }

    public Channel getChannel() {
        return this.channel;
    }
}
