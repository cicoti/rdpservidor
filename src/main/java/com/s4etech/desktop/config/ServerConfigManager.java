package com.s4etech.desktop.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.s4etech.desktop.path.ApplicationPaths;

public class ServerConfigManager {

	private static final Logger logger = LoggerFactory.getLogger(ServerConfigManager.class);

	private static final String FILE_NAME = "remote-desktop-server.properties";
	private static final String KEY_HANDSHAKE_PORT = "handshake.port";
	private static final String KEY_CONTROL_PORT = "control.port";
	private static final String KEY_CONNECTION_PROFILE = "connection.profile";
	private static final String KEY_CONNECTION_PROFILE_PREFIX = "connection.profile.";

	private final File configFile;

	public ServerConfigManager() {
		this.configFile = new File(ApplicationPaths.getApplicationBaseDirectory(), FILE_NAME);
		logger.info("Gerenciador de configuração inicializado | arquivo={}", configFile.getAbsolutePath());
	}

	public ServerConfig load() {
		ServerConfig config = new ServerConfig();

		if (!configFile.exists()) {
			logger.warn("Arquivo de configuração não encontrado. Usando valores padrão | arquivo={}",
					configFile.getAbsolutePath());
			return config;
		}

		Properties properties = new Properties();

		try (FileInputStream fis = new FileInputStream(configFile)) {
			logger.info("Carregando configuração | arquivo={}", configFile.getAbsolutePath());

			properties.load(fis);

			int handshakePort = parsePort(KEY_HANDSHAKE_PORT, properties.getProperty(KEY_HANDSHAKE_PORT),
					ServerConfig.DEFAULT_HANDSHAKE_PORT);

			int controlPort = parsePort(KEY_CONTROL_PORT, properties.getProperty(KEY_CONTROL_PORT),
					ServerConfig.DEFAULT_CONTROL_PORT);

			List<ConnectionProfile> availableProfiles = parseAvailableProfiles(properties);
			ConnectionProfile defaultProfile = findDefaultProfile(availableProfiles);
			ConnectionProfile connectionProfile = parseConnectionProfile(properties.getProperty(KEY_CONNECTION_PROFILE),
					availableProfiles, defaultProfile);

			config.setHandshakePort(handshakePort);
			config.setControlPort(controlPort);
			config.setAvailableProfiles(availableProfiles);
			config.setConnectionProfile(connectionProfile);

			logger.info("Configuração carregada com sucesso | handshake={} | controle={} | perfil={} | perfis={}",
					handshakePort, controlPort, connectionProfile.getId(), availableProfiles.size());

		} catch (Exception e) {
			logger.error("Falha ao carregar configuração. Usando valores padrão | arquivo={}",
					configFile.getAbsolutePath(), e);
		}

		return config;
	}

	public void save(ServerConfig config) throws Exception {
		Properties properties = new Properties();
		properties.setProperty(KEY_HANDSHAKE_PORT, String.valueOf(config.getHandshakePort()));
		properties.setProperty(KEY_CONTROL_PORT, String.valueOf(config.getControlPort()));
		properties.setProperty(KEY_CONNECTION_PROFILE, config.getConnectionProfile().getId());

		List<ConnectionProfile> availableProfiles = new ArrayList<>(config.getAvailableProfiles());
		availableProfiles.sort(Comparator.comparing(ConnectionProfile::getId, String.CASE_INSENSITIVE_ORDER));

		for (ConnectionProfile profile : availableProfiles) {
			properties.setProperty(KEY_CONNECTION_PROFILE_PREFIX + profile.getId(), profile.toPropertyValue());
		}

		logger.info("Salvando configuração | arquivo={} | handshake={} | controle={} | perfil={} | perfis={}",
				configFile.getAbsolutePath(), config.getHandshakePort(), config.getControlPort(),
				config.getConnectionProfile().getId(), availableProfiles.size());

		try (FileOutputStream fos = new FileOutputStream(configFile)) {
			properties.store(fos,
					"formato:\nid,displayName,width,height,fps,bitrateKbps,keyIntMax,encoderPreset,encoderTune,leakyQueue\n\nRemote Desktop Server configuration");
			logger.info("Configuração salva com sucesso | arquivo={}", configFile.getAbsolutePath());
		} catch (Exception e) {
			logger.error("Erro ao salvar configuração | arquivo={}", configFile.getAbsolutePath(), e);
			throw e;
		}
	}

	private int parsePort(String key, String value, int defaultValue) {
		if (value == null || value.trim().isEmpty()) {
			logger.warn("Chave de configuração ausente ou vazia. Usando valor padrão | chave={} | valorPadrao={}", key,
					defaultValue);
			return defaultValue;
		}

		try {
			int port = Integer.parseInt(value.trim());

			if (port >= 1 && port <= 65535) {
				return port;
			}

			logger.warn(
					"Porta fora do intervalo válido. Usando valor padrão | chave={} | valorInformado={} | valorPadrao={}",
					key, value, defaultValue);
		} catch (Exception e) {
			logger.warn(
					"Valor inválido para porta. Usando valor padrão | chave={} | valorInformado={} | valorPadrao={}",
					key, value, defaultValue);
		}

		return defaultValue;
	}

	private List<ConnectionProfile> parseAvailableProfiles(Properties properties) {
		List<ConnectionProfile> profiles = new ArrayList<>();

		for (String propertyName : properties.stringPropertyNames()) {
			if (!propertyName.startsWith(KEY_CONNECTION_PROFILE_PREFIX)) {
				continue;
			}

			String propertyValue = properties.getProperty(propertyName);

			try {
				ConnectionProfile profile = ConnectionProfile.fromPropertyValue(propertyValue);
				if (!profiles.contains(profile)) {
					profiles.add(profile);
				}
			} catch (Exception e) {
				logger.warn("Perfil de conexão inválido ignorado | chave={} | valor={}", propertyName, propertyValue);
			}
		}

		if (profiles.isEmpty()) {
			logger.warn("Nenhum perfil de conexão encontrado no arquivo. Usando perfil padrão.");
			profiles.add(ServerConfig.DEFAULT_CONNECTION_PROFILE);
		}

		profiles.sort(Comparator.comparing(ConnectionProfile::getId, String.CASE_INSENSITIVE_ORDER));
		return profiles;
	}

	private ConnectionProfile findDefaultProfile(List<ConnectionProfile> profiles) {
		for (ConnectionProfile profile : profiles) {
			if (ConnectionProfile.DEFAULT_ID.equalsIgnoreCase(profile.getId())) {
				return profile;
			}
		}
		return ServerConfig.DEFAULT_CONNECTION_PROFILE;
	}

	private ConnectionProfile parseConnectionProfile(String value, List<ConnectionProfile> availableProfiles,
			ConnectionProfile defaultValue) {
		if (value == null || value.trim().isEmpty()) {
			logger.warn("Perfil de conexão ausente ou vazio. Usando valor padrão | valorPadrao={}",
					defaultValue.getId());
			return defaultValue;
		}

		for (ConnectionProfile profile : availableProfiles) {
			if (profile.getId().equalsIgnoreCase(value.trim())) {
				return profile;
			}
		}

		logger.warn("Perfil de conexão inválido. Usando valor padrão | valorInformado={} | valorPadrao={}", value,
				defaultValue.getId());
		return defaultValue;
	}

	public File getConfigFile() {
		return configFile;
	}
}
