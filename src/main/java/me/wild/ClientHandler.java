package me.wild;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class ClientHandler implements Runnable {
    private SocketChannel socketChannel;
    private Selector selector;
    public boolean running = false;
    public long lastHeartbeat;
    private String clientIP;
    private Main main;
    private ByteBuffer readBuffer;

    public ClientHandler(Main main, SocketChannel socketChannel) throws IOException {
        this.lastHeartbeat = System.currentTimeMillis();
        this.running = true;
        this.main = main;
        this.socketChannel = socketChannel;
        this.clientIP = socketChannel.getRemoteAddress().toString();
        this.readBuffer = ByteBuffer.allocate(1024);

        socketChannel.configureBlocking(false);
        this.selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_READ);

        if (main.connections.containsKey(clientIP)) main.connections.remove(clientIP);
        main.connections.put(clientIP, this);
        main.ut.log("Control Client Connected. Client: " + clientIP);
    }

    @Override
    public void run() {
        while (this.running) {
            try {
                if (selector.select() > 0) {
                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        if (key.isReadable()) {
                            readData((SocketChannel) key.channel());
                        }
                        keys.remove();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(5000); // Wait 500 milliseconds before trying again
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void readData(SocketChannel channel) throws IOException {
        readBuffer.clear();
        int bytesRead = channel.read(readBuffer);
        if (bytesRead > 0) {
            readBuffer.flip();
            String command = new String(readBuffer.array(), 0, bytesRead);
            if (command.startsWith("+heartbeat")) {
                this.lastHeartbeat = System.currentTimeMillis();
            } else {
                this.main.ut.log("Received Input: " + command);
                try {
                    this.main.cmd.runCmd(command);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean checkHeartbeat() {
        return (System.currentTimeMillis() - lastHeartbeat > 32000L);
    }

    public void close() {
        try {
            this.running = false;
            socketChannel.close();
            selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.lastHeartbeat = 1L;
    }

    public SocketChannel getSocketChannel() {
        return this.socketChannel;
    }
}
