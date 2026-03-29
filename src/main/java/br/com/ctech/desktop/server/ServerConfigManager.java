package br.com.ctech.desktop.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ServerConfigManager.class);

    private static final String FILE_NAME = "remote-desktop-server.properties";
    private static final String KEY_HANDSHAKE_PORT = "handshake.port";
    private static final String KEY_CONTROL_PORT = "control.port";

    private final File configFile;

    public ServerConfigManager() {
        this.configFile = new File(ApplicationPaths.getApplicationBaseDirectory(), FILE_NAME);
        logger.info("Gerenciador de configuração inicializado | arquivo={}", configFile.getAbsolutePath());
    }

    public ServerConfig load() {
        ServerConfig config = new ServerConfig();

        if (!configFile.exists()) {
            logger.warn(
                    "Arquivo de configuração não encontrado. Usando valores padrão | arquivo={}",
                    configFile.getAbsolutePath()
            );
            return config;
        }

        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream(configFile)) {
            logger.info("Carregando configuração | arquivo={}", configFile.getAbsolutePath());

            properties.load(fis);

            int handshakePort = parsePort(
                    KEY_HANDSHAKE_PORT,
                    properties.getProperty(KEY_HANDSHAKE_PORT),
                    ServerConfig.DEFAULT_HANDSHAKE_PORT
            );

            int controlPort = parsePort(
                    KEY_CONTROL_PORT,
                    properties.getProperty(KEY_CONTROL_PORT),
                    ServerConfig.DEFAULT_CONTROL_PORT
            );

            config.setHandshakePort(handshakePort);
            config.setControlPort(controlPort);

            logger.info(
                    "Configuração carregada com sucesso | handshake={} | controle={}",
                    handshakePort,
                    controlPort
            );

        } catch (Exception e) {
            logger.error(
                    "Falha ao carregar configuração. Usando valores padrão | arquivo={}",
                    configFile.getAbsolutePath(),
                    e
            );
        }

        return config;
    }

    public void save(ServerConfig config) throws Exception {
        Properties properties = new Properties();
        properties.setProperty(KEY_HANDSHAKE_PORT, String.valueOf(config.getHandshakePort()));
        properties.setProperty(KEY_CONTROL_PORT, String.valueOf(config.getControlPort()));

        logger.info(
                "Salvando configuração | arquivo={} | handshake={} | controle={}",
                configFile.getAbsolutePath(),
                config.getHandshakePort(),
                config.getControlPort()
        );

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, "Remote Desktop Server configuration");
            logger.info("Configuração salva com sucesso | arquivo={}", configFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Erro ao salvar configuração | arquivo={}", configFile.getAbsolutePath(), e);
            throw e;
        }
    }

    private int parsePort(String key, String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            logger.warn(
                    "Chave de configuração ausente ou vazia. Usando valor padrão | chave={} | valorPadrao={}",
                    key,
                    defaultValue
            );
            return defaultValue;
        }

        try {
            int port = Integer.parseInt(value.trim());

            if (port >= 1 && port <= 65535) {
                return port;
            }

            logger.warn(
                    "Porta fora do intervalo válido. Usando valor padrão | chave={} | valorInformado={} | valorPadrao={}",
                    key,
                    value,
                    defaultValue
            );
        } catch (Exception e) {
            logger.warn(
                    "Valor inválido para porta. Usando valor padrão | chave={} | valorInformado={} | valorPadrao={}",
                    key,
                    value,
                    defaultValue
            );
        }

        return defaultValue;
    }

    public File getConfigFile() {
        return configFile;
    }
}