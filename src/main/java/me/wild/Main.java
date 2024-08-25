package me.wild;

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main implements Runnable {
    public HashMap<String, ClientHandler> connections = new HashMap<>();
    public HashMap<String, ManagedServer> managedServers = new HashMap<>();
    private static ExecutorService threadPool = Executors.newCachedThreadPool();

    
    public ArrayList<Process> serverProcesses = new ArrayList<>();
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private static Main server;
    int abandonTime = 2;
    public Utils ut;
    public boolean debug = false;

    CommandProcess cmd;
    public boolean isWindows = false;

    // Thread pool to handle connections and tasks

    public Main(int port) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }
    
    @Override
    public void run() {
        try {
            while (true) {
                selector.select(); // Blocking until there's an event
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        acceptConnection();
                    } else if (key.isReadable()) {
                        readData(key);
                    }
                }
                try {
	                Thread.sleep(5000); // Wait 5000 milliseconds before trying again
	            } catch (InterruptedException e) {
	                Thread.currentThread().interrupt();
	                break;
	            }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                serverSocketChannel.close();
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void acceptConnection() throws IOException {
        SocketChannel clientChannel = serverSocketChannel.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
        String clientIP = clientChannel.getRemoteAddress().toString();
        ClientHandler handler = new ClientHandler(this, clientChannel);
        connections.put(clientIP, handler);
        ut.log("Control Client Connected. Client: " + clientIP);
    }

    private void readData(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ClientHandler handler = connections.get(clientChannel.getRemoteAddress().toString());
        handler.run(); // Process incoming data
    }

    private void watchAbandon() {
        threadPool.submit(() -> {
            int fails = 0;
            while (fails <= server.abandonTime * 4) {
                if (server.connections.size() > 0) {
                    Iterator<String> iter = server.connections.keySet().iterator();
                    while (iter.hasNext()) {
                        ClientHandler listener = server.connections.get(iter.next());
                        SocketChannel channel = listener.getSocketChannel();
                        if (!channel.isConnected() || !channel.isOpen() || listener.checkHeartbeat()) {
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

    public static void main(String[] args) throws IOException {
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
        // Shutdown the thread pool
        threadPool.shutdown();
    }

    public static ExecutorService getThreadPool() {
        return threadPool;
    }
}
