package me.wild;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ManagedServer implements Runnable {
    private Main main;
    private ProcessBuilder procBuild;
    private Process proc;
    private List<String> command;
    private String serverName;
    private String path;
    private ConsoleWatcher console;
    private InputStream errors;

    public ManagedServer(String serverName, String path, String port, String maxPlayers, String mems, String memx, String jarFile) {
        this.proc = null;
        this.main = Main.getInstance();
        this.serverName = serverName;
        this.path = path;
        prepare(serverName, path, port, maxPlayers, mems, memx, jarFile);
        submitToThreadPool();
    }

    public ManagedServer(List<String> command, String serverName, String path) {
        this.proc = null;
        this.command = command;
        this.serverName = serverName;
        this.path = path;
        this.main = Main.getInstance();
        this.procBuild = new ProcessBuilder(command).directory(new File(path));
        submitToThreadPool();
    }

    private void prepare(String serverName, String path, String port, String maxPlayers, String mems, String memx, String jarFile) {
    	command = new ArrayList<>();
        command.add("java");

        // Set memory options
        command.add(memx.matches("-Xmx.*") ? memx : "-Xmx" + memx);
        command.add(mems.matches("-Xms.*") ? mems : "-Xms" + mems);

        // Additional JVM arguments for Windows
        if (main.isWindows) {
            command.add("-Djline.terminal=jline.UnsupportedTerminal");
        }

        // EULA agreement for non-PaperSpigot servers
        if (!jarFile.matches("(?i)(paperspigot).*")) {
            command.add("-Dcom.mojang.eula.agree=true");
        }

        // Add the jar file and server options
        command.add("-jar");
        command.add(jarFile);

        // Disable jline on Windows and GUI if not in debug mode
        if (main.isWindows) {
            command.add("--nojline");
        }
        if (!main.debug) {
            command.add("--nogui");
        }

        command.add("--port");
        command.add(port);
        command.add("-o");
        command.add("false");
        command.add("--max-players");
        command.add(maxPlayers);

        // Log command and path
        main.ut.log("command: " + command);
        main.ut.log("path: " + path);
        procBuild = new ProcessBuilder(command).directory(new File(path));
    }

    private void startup() {
        try {
            this.proc = this.procBuild.start();
            this.main.ut.log("Starting server within PSWrapperV2");
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.proc.getInputStream()));
            this.console = new ConsoleWatcher("in", reader);
            this.errors = this.proc.getErrorStream();
            (new Thread(new ProcessWatcher(this, this.proc))).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void submitToThreadPool() {
        ExecutorService threadPool = Main.getThreadPool();
        threadPool.submit(this);
    }

    public void killThreads() {
        this.console.killIt();
    }

    @Override
    public void run() {
        this.main.ut.log("Starting up...");
        startup();
    }

    public void kill() {
        killThreads();
        this.proc.destroy();
        this.main.ut.log("Server '" + this.serverName + "' successfully killed.");
    }

    public void stop() {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(getProcess().getOutputStream()));
            writer.write("stop");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Process getProcess() {
        return this.proc;
    }

    public InputStream getErrorStream() {
        return this.errors;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getCommand() {
        return this.command;
    }

    public void setCommand(List<String> command) {
        this.command = command;
    }

    public String getServerName() {
        return this.serverName;
    }
}
