package me.wild;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public HashMap<String, ClientHandler> connections = new HashMap<>();
    public HashMap<String, ManagedServer> managedServers = new HashMap<>();
    private static ExecutorService threadPool = Executors.newCachedThreadPool();
    public List<Process> serverProcesses = new ArrayList<>();

    private static Main server;
    int abandonTime = 2;
    public Utils ut;
    public boolean debug = false;
    CommandProcess cmd;
    public boolean isWindows = false;

    public Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        int port = 0;
        if (args.length < 1) {
            System.out.println("[PSWrapperV2] [" + Level.SEVERE.getLocalizedName() + "] " + "Listen port must be specified!");
            System.exit(1);
        }
        try {
            port = Integer.parseInt(args[0]);
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

        server = new Main();
        server.ut = new Utils(Logger.getLogger("PSWrapperV2"));
        server.ut.log("Operating System: " + os + " v" + osVer);
        if (os.matches("(?i)(.*)(windows)(.*)"))
            server.isWindows = true;
        if (args[args.length - 1].equalsIgnoreCase("debug")) {
            server.debug = true;
        }
        safeShutdown();
        server.cmd = new CommandProcess();

        server.startServer(port);
        server.watchAbandon();
    }

    private void startServer(int port) throws InterruptedException {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new LineBasedFrameDecoder(8192));
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new ServerHandler());
                        }
                    });

            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private void watchAbandon() {
        threadPool.submit(() -> {
            int fails = 0;
            while (fails <= server.abandonTime * 4) {
                if (server.connections.size() > 0) {
                    Iterator<String> iter = server.connections.keySet().iterator();
                    while (iter.hasNext()) {
                        ClientHandler listener = server.connections.get(iter.next());
                        if (listener.checkHeartbeat()) {
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
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
            server.ut.log("Wrapper had no connection or servers for " + server.abandonTime + " minutes, shutting down.");
            System.exit(0);
        });
    }

    public static Main getInstance() {
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
                BufferedWriter reader = new BufferedWriter(new OutputStreamWriter(server.getProcess().getOutputStream()));
                reader.write("stop");
                reader.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        while (server.managedServers.size() >= 1) {
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        threadPool.shutdown();
    }

    public static ExecutorService getThreadPool() {
        return threadPool;
    }

    private class ServerHandler extends SimpleChannelInboundHandler<String> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            String clientIP = ctx.channel().remoteAddress().toString();
            ClientHandler handler = connections.get(clientIP);
            if (handler == null) {
                handler = new ClientHandler(Main.this, ctx);  // Pass ChannelHandlerContext instead of Channel
                connections.put(clientIP, handler);
            }
            handler.runCmd(msg, ctx);  // Pass context to handle the command
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            String clientIP = ctx.channel().remoteAddress().toString();
            ut.log("Control Client Connected. Client: " + clientIP);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            String clientIP = ctx.channel().remoteAddress().toString();
            connections.remove(clientIP);
            ut.log("Control Client Disconnected. Client: " + clientIP);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }

}
