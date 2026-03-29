package br.com.ctech.remote.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class ServerConfigManager {

    private static final String FILE_NAME = "remote-desktop-server.properties";
    private static final String KEY_HANDSHAKE_PORT = "handshake.port";
    private static final String KEY_CONTROL_PORT = "control.port";

    private final File configFile;

    public ServerConfigManager() {
        this.configFile = new File(FILE_NAME);
    }

    public ServerConfig load() {
        ServerConfig config = new ServerConfig();

        if (!configFile.exists()) {
            return config;
        }

        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);

            int handshakePort = parsePort(
                    properties.getProperty(KEY_HANDSHAKE_PORT),
                    ServerConfig.DEFAULT_HANDSHAKE_PORT
            );

            int controlPort = parsePort(
                    properties.getProperty(KEY_CONTROL_PORT),
                    ServerConfig.DEFAULT_CONTROL_PORT
            );

            config.setHandshakePort(handshakePort);
            config.setControlPort(controlPort);

        } catch (Exception e) {
            //System.out.println("Falha ao carregar configuração. Usando valores padrão.");
            e.printStackTrace();
        }

        return config;
    }

    public void save(ServerConfig config) throws Exception {
        Properties properties = new Properties();
        properties.setProperty(KEY_HANDSHAKE_PORT, String.valueOf(config.getHandshakePort()));
        properties.setProperty(KEY_CONTROL_PORT, String.valueOf(config.getControlPort()));

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, "Remote Desktop Server configuration");
        }
    }

    private int parsePort(String value, int defaultValue) {
        try {
            int port = Integer.parseInt(value);
            if (port >= 1 && port <= 65535) {
                return port;
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    public File getConfigFile() {
        return configFile;
    }
}