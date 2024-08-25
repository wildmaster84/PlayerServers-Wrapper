package me.wild;

import java.io.BufferedReader;
import java.io.IOException;

class ConsoleWatcher {
    Main main;
    private BufferedReader reader;
    private String name;
    private boolean stopped;

    public ConsoleWatcher(String name, BufferedReader reader) {
        this.stopped = false;
        this.main = Main.getInstance();
        this.name = name;
        this.reader = reader;
        run();
    }

    public void killIt() {
        this.stopped = true;
    }

    private void run() {
        new Thread(() -> {
            try {
                String line;
                while (!stopped && (line = reader.readLine()) != null) {
                    if (!main.debug) continue;

                    if (name.equalsIgnoreCase("in")) {
                        main.ut.log(String.valueOf(name) + ":Server Log: " + line);
                    } else {
                        main.ut.log(String.valueOf(name) + "/unknown: " + line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
