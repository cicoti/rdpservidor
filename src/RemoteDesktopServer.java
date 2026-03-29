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

import br.com.ctech.remote.server.MouseControlServer;
import br.com.ctech.remote.server.ScreenStreamServer;
import br.com.ctech.remote.server.ServerConfig;
import br.com.ctech.remote.server.ServerConfigDialog;
import br.com.ctech.remote.server.ServerConfigManager;


public class RemoteDesktopServer {

    private static final String APP_NAME = "Remote Desktop Server";
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
        //System.out.println("Iniciando " + APP_NAME + "...");

        if (!isSingleInstance()) {
            showAlreadyRunningMessage();
            return;
        }

        configManager = new ServerConfigManager();
        currentConfig = configManager.load();

        //System.out.println("Configuração carregada | handshake=" + currentConfig.getHandshakePort()
        //        + " | controle=" + currentConfig.getControlPort());

        screenServer = new ScreenStreamServer(currentConfig.getHandshakePort());
        mouseServer = new MouseControlServer(currentConfig.getControlPort());

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> performShutdown(false), "remote-desktop-shutdown-hook")
        );

        enableDPIAwareness();
        logDpiScale();
        initializeSystemTray();
        startServers();

        //System.out.println("Servidor de vídeo e mouse iniciados.");
    }

    private static boolean isSingleInstance() {
        try {
            File file = new File(System.getProperty("java.io.tmpdir"), LOCK_FILE_NAME);

            lockRandomAccessFile = new RandomAccessFile(file, "rw");
            lockChannel = lockRandomAccessFile.getChannel();

            try {
                lock = lockChannel.tryLock();
            } catch (OverlappingFileLockException e) {
                lock = null;
            }

            if (lock == null) {
                releaseSingleInstance();
                return false;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
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

        System.out.println(message);

        try {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(
                        null,
                        message,
                        "Aviso",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        } catch (Exception ignored) {
        }
    }

    private static synchronized void startServers() {
        if (screenServer == null || mouseServer == null) {
            //System.out.println("Servidores não foram inicializados corretamente.");
            return;
        }

        if (!screenServer.isRunning()) {
            //System.out.println("Iniciando ScreenStreamServer...");
            screenServer.start();
        } else {
            //System.out.println("ScreenStreamServer já estava ativo.");
        }

        if (!mouseServer.isRunning()) {
            //System.out.println("Iniciando MouseControlServer...");
            mouseServer.start();
        } else {
            //System.out.println("MouseControlServer já estava ativo.");
        }

        logServersState("Estado após start");
    }

    private static synchronized void stopServers() {
        //System.out.println("Parando servidores...");

        if (screenServer != null) {
            if (screenServer.isRunning()) {
                //System.out.println("Parando ScreenStreamServer...");
                screenServer.stop();
            } else {
                //System.out.println("ScreenStreamServer já estava parado.");
            }
        } else {
            //System.out.println("ScreenStreamServer não foi instanciado.");
        }

        if (mouseServer != null) {
            if (mouseServer.isRunning()) {
                //System.out.println("Parando MouseControlServer...");
                mouseServer.stop();
            } else {
                //System.out.println("MouseControlServer já estava parado.");
            }
        } else {
            //System.out.println("MouseControlServer não foi instanciado.");
        }

        logServersState("Estado após stop");
    }

    private static void logServersState(String context) {
        String videoState = (screenServer != null && screenServer.isRunning()) ? "ativo" : "parado";
        String mouseState = (mouseServer != null && mouseServer.isRunning()) ? "ativo" : "parado";

        System.out.println(context + " | vídeo=" + videoState + " | mouse=" + mouseState);
    }

    private static void logDpiScale() {
        double scaleX = 1.5;
        double scaleY = 1.5;

        GraphicsConfiguration gc =
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice()
                        .getDefaultConfiguration();

        AffineTransform tx = gc.getDefaultTransform();
        scaleX = tx.getScaleX();
        scaleY = tx.getScaleY();

        //System.out.println("DPI scale X=" + scaleX + " Y=" + scaleY);
    }

    private static void initializeSystemTray() {
        if (GraphicsEnvironment.isHeadless()) {
            //System.out.println("Ambiente sem interface gráfica. Tray não disponível.");
            return;
        }

        if (!SystemTray.isSupported()) {
            //System.out.println("SystemTray não suportado neste ambiente.");
            return;
        }

        EventQueue.invokeLater(() -> {
            try {
                Image trayImage = loadTrayImage();
                if (trayImage == null) {
                    //System.out.println("Ícone da bandeja não encontrado em resources.");
                    return;
                }

                PopupMenu popupMenu = new PopupMenu();

                MenuItem statusItem = new MenuItem("Status");
                statusItem.addActionListener(e ->
                        showTrayMessage(APP_NAME, buildStatusMessage(), MessageType.INFO));

                MenuItem configItem = new MenuItem("Configuração");
                configItem.addActionListener(e -> openConfigurationDialog());

                MenuItem exitItem = new MenuItem("Sair");
                exitItem.addActionListener(e -> shutdownApplication());

                popupMenu.add(statusItem);
                popupMenu.addSeparator();
                popupMenu.add(configItem);
                popupMenu.addSeparator();
                popupMenu.add(exitItem);

                trayIcon = new TrayIcon(trayImage, APP_NAME, popupMenu);
                trayIcon.setImageAutoSize(true);

                trayIcon.addActionListener(e ->
                        showTrayMessage(APP_NAME, buildStatusMessage(), MessageType.INFO));

                SystemTray.getSystemTray().add(trayIcon);

                showTrayMessage(APP_NAME, "Aplicação iniciada com sucesso.", MessageType.INFO);

            } catch (AWTException e) {
                //System.out.println("Erro ao adicionar ícone na bandeja.");
                e.printStackTrace();
            } catch (Exception e) {
                //System.out.println("Erro inesperado ao inicializar a bandeja.");
                e.printStackTrace();
            }
        });
    }

    private static void openConfigurationDialog() {
        try {
            ServerConfig latestConfig = configManager.load();

            int activeHandshakePort = currentConfig != null ? currentConfig.getHandshakePort() : ServerConfig.DEFAULT_HANDSHAKE_PORT;
            int activeControlPort = currentConfig != null ? currentConfig.getControlPort() : ServerConfig.DEFAULT_CONTROL_PORT;

            SwingUtilities.invokeLater(() -> {
                ServerConfigDialog dialog = new ServerConfigDialog(
                        null,
                        latestConfig,
                        configManager,
                        activeHandshakePort,
                        activeControlPort
                );
                dialog.setVisible(true);
                currentConfig = configManager.load();
            });

        } catch (Exception e) {
            //System.out.println("Erro ao abrir tela de configuração.");
            e.printStackTrace();
        }
    }

    private static String buildStatusMessage() {
        return "Vídeo: " + ((screenServer != null && screenServer.isRunning()) ? "ativo" : "parado")
                + " | Mouse: " + ((mouseServer != null && mouseServer.isRunning()) ? "ativo" : "parado")
                + " | Handshake: " + (currentConfig != null ? currentConfig.getHandshakePort() : "-")
                + " | Controle: " + (currentConfig != null ? currentConfig.getControlPort() : "-");
    }

    private static Image loadTrayImage() {
        try (java.io.InputStream is =
                     RemoteDesktopServer.class.getClassLoader().getResourceAsStream("icon_remote_server.png")) {

            if (is == null) {
                //System.out.println("Arquivo icon_remote_server.png não encontrado no classpath.");
                return null;
            }

            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(is);

            if (image == null) {
                //System.out.println("Falha ao ler icon_remote_server.png.");
                return null;
            }

            return image;

        } catch (Exception e) {
            //System.out.println("Erro ao carregar ícone da bandeja.");
            e.printStackTrace();
            return null;
        }
    }

    private static void showTrayMessage(String title, String message, MessageType type) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, type);
        } else {
            System.out.println(title + ": " + message);
        }
    }

    private static void shutdownApplication() {
        performShutdown(true);
    }

    private static void performShutdown(boolean exitJvm) {
        if (!shuttingDown.compareAndSet(false, true)) {
            //System.out.println("Shutdown já está em andamento.");
            return;
        }

        //System.out.println("Iniciando encerramento da aplicação...");

        try {
            stopServers();
        } catch (Exception e) {
            //System.out.println("Erro ao parar servidores.");
            e.printStackTrace();
        }

        try {
            removeTrayIcon();
        } catch (Exception e) {
            //System.out.println("Erro ao remover tray icon.");
            e.printStackTrace();
        }

        try {
            releaseSingleInstance();
        } catch (Exception e) {
            //System.out.println("Erro ao liberar lock da aplicação.");
            e.printStackTrace();
        }

        //System.out.println("Encerramento finalizado.");

        if (exitJvm) {
            //System.out.println("Finalizando JVM...");
            System.exit(0);
        }
    }

    private static void removeTrayIcon() {
        try {
            if (trayIcon != null && SystemTray.isSupported()) {
                SystemTray.getSystemTray().remove(trayIcon);
                trayIcon = null;
            }
        } catch (Exception ignored) {
        }
    }

    public static void enableDPIAwareness() {
        try {
            com.sun.jna.Native.register("user32");
            setProcessDPIAware();
        } catch (Throwable ignored) {
        }
    }
}