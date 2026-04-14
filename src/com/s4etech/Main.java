package com.s4etech;

import java.io.File;

import com.s4etech.desktop.path.ApplicationPaths;
import com.s4etech.desktop.server.RemoteDesktopServer;

public class Main {

    public static void main(String[] args) {
        File baseDir = ApplicationPaths.getApplicationBaseDirectory();
        File logsDir = new File(baseDir, "logs");

        if (!logsDir.exists()) {
            boolean created = logsDir.mkdirs();
            System.out.println("Logs dir criada: " + created + " | caminho=" + logsDir.getAbsolutePath());
        } else {
            System.out.println("Logs dir já existe | caminho=" + logsDir.getAbsolutePath());
        }

        System.setProperty("app.base.dir", baseDir.getAbsolutePath());
        System.out.println("app.base.dir=" + System.getProperty("app.base.dir"));

        RemoteDesktopServer.main(args);
    }
}