package com.s4etech.desktop.server;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.s4etech.desktop.config.ServerConfig;
import com.s4etech.desktop.config.ServerConfigDialog;
import com.s4etech.desktop.config.ServerConfigManager;
import com.s4etech.desktop.help.HelpDialog;
import com.s4etech.desktop.path.ApplicationPaths;

public class RemoteDesktopServer {

	private static final Logger logger = LoggerFactory.getLogger(RemoteDesktopServer.class);

	private static final String APP_NAME = "Remote Desktop Server";
	private static final String APP_VERSION = "1.2.0";
	private static final String LOCK_FILE_NAME = "remote_desktop_server.lock";

	private static TrayIcon trayIcon;

	private static RandomAccessFile lockRandomAccessFile;
	private static FileChannel lockChannel;
	private static FileLock lock;

	private static ScreenStreamServer screenServer;
	private static MouseControlServer mouseServer;

	private static ServerConfigManager configManager;
	private static ServerConfig currentConfig;

	private static final AtomicBoolean shuttingDown = new AtomicBoolean(false);

	private static native boolean setProcessDPIAware();

	public static void main(String[] args) {
		System.setProperty("app.base.dir", ApplicationPaths.getApplicationBaseDirectory().getAbsolutePath());

		logger.info("Aplicação iniciando");

		if (!isSingleInstance()) {
			showAlreadyRunningMessage();
			return;
		}

		configManager = new ServerConfigManager();
		currentConfig = configManager.load();

		logger.info("Configuração carregada com sucesso | handshake={} | controle={} | perfil={} ({})",
				currentConfig.getHandshakePort(), currentConfig.getControlPort(),
				currentConfig.getConnectionProfile().getId(),
				currentConfig.getConnectionProfile().getDisplayName());

		screenServer = new ScreenStreamServer(currentConfig.getHandshakePort(), currentConfig.getConnectionProfile());
		mouseServer = new MouseControlServer(currentConfig.getControlPort());

		logger.info("Servidores criados com sucesso");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> performShutdown(false), "remote-desktop-shutdown-hook"));

		enableDPIAwareness();
		logDpiScale();
		initializeSystemTray();
		startServers();

		logger.info("Fluxo principal de inicialização concluído");
	}

	private static boolean isSingleInstance() {
		File file = ApplicationPaths.getLockFile();

		logger.info("Validando instância única | lockFile={}", file.getAbsolutePath());

		try {
			lockRandomAccessFile = new RandomAccessFile(file, "rw");
			lockChannel = lockRandomAccessFile.getChannel();

			try {
				lock = lockChannel.tryLock();
			} catch (OverlappingFileLockException e) {
				logger.warn("Lock já está em uso por esta JVM");
				lock = null;
			}

			if (lock == null) {
				logger.warn("Outra instância da aplicação já está em execução");
				releaseSingleInstance();
				return false;
			}

			logger.info("Lock de instância única obtido com sucesso");
			return true;

		} catch (Exception e) {
			logger.error("Erro ao validar instância única da aplicação", e);
			releaseSingleInstance();
			return false;
		}
	}

	private static void releaseSingleInstance() {
		try {
			if (lock != null && lock.isValid()) {
				lock.release();
			}
		} catch (Exception ignored) {
		} finally {
			lock = null;
		}

		try {
			if (lockChannel != null && lockChannel.isOpen()) {
				lockChannel.close();
			}
		} catch (Exception ignored) {
		} finally {
			lockChannel = null;
		}

		try {
			if (lockRandomAccessFile != null) {
				lockRandomAccessFile.close();
			}
		} catch (Exception ignored) {
		} finally {
			lockRandomAccessFile = null;
		}
	}

	private static void showAlreadyRunningMessage() {
		String message = APP_NAME + " já está em execução.";

		logger.warn(message);

		try {
			if (!GraphicsEnvironment.isHeadless()) {
				JOptionPane.showMessageDialog(null, message, "Aviso", JOptionPane.WARNING_MESSAGE);
			}
		} catch (Exception ignored) {
		}
	}

	private static synchronized void startServers() {
		if (screenServer == null || mouseServer == null) {
			logger.warn("Servidores não foram inicializados corretamente");
			return;
		}

		logger.info("Iniciando servidores");

		if (!screenServer.isRunning()) {
			logger.info("Iniciando ScreenStreamServer");
			screenServer.start();
		} else {
			logger.info("ScreenStreamServer já estava ativo");
		}

		if (!mouseServer.isRunning()) {
			logger.info("Iniciando MouseControlServer");
			mouseServer.start();
		} else {
			logger.info("MouseControlServer já estava ativo");
		}

		logServersState("Estado após start");
	}

	private static synchronized void stopServers() {
		logger.info("Parando servidores");

		if (screenServer != null) {
			if (screenServer.isRunning()) {
				logger.info("Parando ScreenStreamServer");
				screenServer.stop();
			} else {
				logger.info("ScreenStreamServer já estava parado");
			}
		} else {
			logger.warn("ScreenStreamServer não foi instanciado");
		}

		if (mouseServer != null) {
			if (mouseServer.isRunning()) {
				logger.info("Parando MouseControlServer");
				mouseServer.stop();
			} else {
				logger.info("MouseControlServer já estava parado");
			}
		} else {
			logger.warn("MouseControlServer não foi instanciado");
		}

		logServersState("Estado após stop");
	}

	private static void logServersState(String context) {
		String videoState = (screenServer != null && screenServer.isRunning()) ? "ativo" : "parado";
		String mouseState = (mouseServer != null && mouseServer.isRunning()) ? "ativo" : "parado";

		logger.info("{} | vídeo={} | mouse={}", context, videoState, mouseState);
	}

	private static void logDpiScale() {
		try {
			GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
					.getDefaultConfiguration();

			AffineTransform tx = gc.getDefaultTransform();
			double scaleX = tx.getScaleX();
			double scaleY = tx.getScaleY();

			logger.info("DPI detectado | scaleX={} | scaleY={}", scaleX, scaleY);
		} catch (Exception e) {
			logger.error("Erro ao obter escala de DPI", e);
		}
	}

	private static void initializeSystemTray() {
		if (GraphicsEnvironment.isHeadless()) {
			logger.warn("Ambiente sem interface gráfica. System tray não será inicializado");
			return;
		}

		if (!SystemTray.isSupported()) {
			logger.warn("SystemTray não é suportado neste ambiente");
			return;
		}

		logger.info("Inicializando system tray");

		EventQueue.invokeLater(() -> {
			try {
				Image trayImage = loadTrayImage();
				if (trayImage == null) {
					logger.warn("Inicialização do tray cancelada porque o ícone não foi carregado");
					return;
				}

				PopupMenu popupMenu = new PopupMenu();

				MenuItem statusItem = new MenuItem("Status");
				statusItem.addActionListener(e -> showTrayMessage(APP_NAME, buildStatusMessage(), MessageType.INFO));

				MenuItem configItem = new MenuItem("Configuração");
				configItem.addActionListener(e -> openConfigurationDialog());

				MenuItem helpItem = new MenuItem("Ajuda");
				helpItem.addActionListener(e -> openHelpDialog());

				MenuItem exitItem = new MenuItem("Sair");
				exitItem.addActionListener(e -> shutdownApplication());

				popupMenu.add(statusItem);
				popupMenu.addSeparator();
				popupMenu.add(configItem);
				popupMenu.addSeparator();
				popupMenu.add(helpItem);
				popupMenu.addSeparator();
				popupMenu.add(exitItem);

				trayIcon = new TrayIcon(trayImage, APP_NAME, popupMenu);
				trayIcon.setImageAutoSize(true);

				trayIcon.addActionListener(e -> showTrayMessage(APP_NAME, buildStatusMessage(), MessageType.INFO));

				SystemTray.getSystemTray().add(trayIcon);

				logger.info("System tray inicializado com sucesso");
				showTrayMessage(APP_NAME, "Aplicação iniciada com sucesso.", MessageType.INFO);

			} catch (AWTException e) {
				logger.error("Erro ao adicionar ícone ao system tray", e);
			} catch (Exception e) {
				logger.error("Erro inesperado durante inicialização do system tray", e);
			}
		});
	}

	private static void openConfigurationDialog() {
		try {
			logger.info("Abrindo tela de configuração");

			ServerConfig latestConfig = configManager.load();

			int activeHandshakePort = currentConfig != null ? currentConfig.getHandshakePort()
					: ServerConfig.DEFAULT_HANDSHAKE_PORT;
			int activeControlPort = currentConfig != null ? currentConfig.getControlPort()
					: ServerConfig.DEFAULT_CONTROL_PORT;

			logger.info("Configuração atual para abertura da tela | handshake={} | controle={}", activeHandshakePort,
					activeControlPort);

			SwingUtilities.invokeLater(() -> {
				ServerConfigDialog dialog = new ServerConfigDialog(null, latestConfig, configManager,
						activeHandshakePort, activeControlPort);
				dialog.setVisible(true);
				currentConfig = configManager.load();

				logger.info("Tela de configuração encerrada | handshakeAtual={} | controleAtual={} | perfilAtual={}",
						currentConfig.getHandshakePort(), currentConfig.getControlPort(),
						currentConfig.getConnectionProfile().getId());
			});

		} catch (Exception e) {
			logger.error("Erro ao abrir tela de configuração", e);
		}
	}

	private static void openHelpDialog() {
		try {
			logger.info("Abrindo tela de ajuda");
			HelpDialog.showHelpDialog();
		} catch (Exception e) {
			logger.error("Erro ao abrir tela de ajuda", e);
		}
	}

	private static String buildStatusMessage() {
		return "Vídeo: " + ((screenServer != null && screenServer.isRunning()) ? "ativo" : "parado")
				+ " | Mouse: " + ((mouseServer != null && mouseServer.isRunning()) ? "ativo" : "parado")
				+ " | Handshake: " + (currentConfig != null ? currentConfig.getHandshakePort() : "-")
				+ " | Controle: " + (currentConfig != null ? currentConfig.getControlPort() : "-")
				+ " | Perfil: " + (currentConfig != null ? currentConfig.getConnectionProfile().getDisplayName() : "-")
				+ " | Versão: " + APP_VERSION;
	}

	private static Image loadTrayImage() {
		logger.info("Carregando ícone do system tray");

		String[] caminhos = { "icon_remote_server.png", "resources/icon_remote_server.png" };

		for (String caminho : caminhos) {
			try (java.io.InputStream is = RemoteDesktopServer.class.getClassLoader().getResourceAsStream(caminho)) {

				if (is == null) {
					logger.warn("Arquivo {} não encontrado no classpath", caminho);
					continue;
				}

				java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(is);

				if (image == null) {
					logger.warn("Falha ao ler {}", caminho);
					continue;
				}

				logger.info("Ícone do system tray carregado com sucesso | caminho={}", caminho);
				return image;

			} catch (Exception e) {
				logger.error("Erro ao carregar ícone do system tray | caminho={}", caminho, e);
			}
		}

		logger.warn("Nenhum ícone válido foi encontrado no classpath");
		return null;
	}

	private static void showTrayMessage(String title, String message, MessageType type) {
		if (trayIcon != null) {
			trayIcon.displayMessage(title, message, type);
		} else {
			logger.info("{}: {}", title, message);
		}
	}

	public static void requestShutdown() {
		shutdownApplication();
	}
	
	private static void shutdownApplication() {
		logger.info("Solicitação de encerramento da aplicação recebida");
		performShutdown(true);
	}

	private static void performShutdown(boolean exitJvm) {
		if (!shuttingDown.compareAndSet(false, true)) {
			// logger.warn("Shutdown já está em andamento");
			return;
		}

		logger.info("Iniciando encerramento da aplicação");

		try {
			stopServers();
		} catch (Exception e) {
			logger.error("Erro ao parar servidores durante encerramento", e);
		}

		try {
			removeTrayIcon();
		} catch (Exception e) {
			logger.error("Erro ao remover system tray durante encerramento", e);
		}

		try {
			releaseSingleInstance();
		} catch (Exception e) {
			logger.error("Erro ao liberar lock da aplicação durante encerramento", e);
		}

		logger.info("Encerramento da aplicação concluído");

		if (exitJvm) {
			logger.info("JVM Finalizanda");
			System.exit(0);
		}
	}

	private static void removeTrayIcon() {
		try {
			if (trayIcon != null && SystemTray.isSupported()) {
				SystemTray.getSystemTray().remove(trayIcon);
				trayIcon = null;
				logger.info("Ícone do system tray removido com sucesso");
			}
		} catch (Exception ignored) {
		}
	}

	public static void enableDPIAwareness() {
		try {
			com.sun.jna.Native.register("user32");
			setProcessDPIAware();
			logger.info("DPI awareness habilitado com sucesso");
		} catch (Throwable ignored) {
			logger.warn("Não foi possível habilitar DPI awareness");
		}
	}
}
