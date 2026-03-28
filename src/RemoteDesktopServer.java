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
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import br.com.ctech.remote.server.MouseControlServer;
import br.com.ctech.remote.server.ScreenStreamServer;

public class RemoteDesktopServer {

    private static final String APP_NAME = "Remote Desktop Server";
    private static final String LOCK_FILE_NAME = "remote_desktop_server.lock";

    private static TrayIcon trayIcon;

    private static RandomAccessFile lockRandomAccessFile;
    private static FileChannel lockChannel;
    private static FileLock lock;

    private static final ScreenStreamServer screenServer = new ScreenStreamServer();
    private static final MouseControlServer mouseServer = new MouseControlServer(5000);

    private static final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    private static native boolean setProcessDPIAware();

    public static void main(String[] args) {
        System.out.println("Iniciando " + APP_NAME + "...");

        if (!isSingleInstance()) {
            showAlreadyRunningMessage();
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> performShutdown(false), "remote-desktop-shutdown-hook"));

        enableDPIAwareness();
        logDpiScale();
        initializeSystemTray();
        startServers();

        System.out.println("Servidor de vídeo e mouse iniciados.");
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
        if (!screenServer.isRunning()) {
            System.out.println("Iniciando ScreenStreamServer...");
            screenServer.start();
        } else {
            System.out.println("ScreenStreamServer já estava ativo.");
        }

        if (!mouseServer.isRunning()) {
            System.out.println("Iniciando MouseControlServer...");
            mouseServer.start();
        } else {
            System.out.println("MouseControlServer já estava ativo.");
        }

        logServersState("Estado após start");
    }

    private static synchronized void stopServers() {
        System.out.println("Parando servidores...");

        if (screenServer.isRunning()) {
            System.out.println("Parando ScreenStreamServer...");
            screenServer.stop();
        } else {
            System.out.println("ScreenStreamServer já estava parado.");
        }

        if (mouseServer.isRunning()) {
            System.out.println("Parando MouseControlServer...");
            mouseServer.stop();
        } else {
            System.out.println("MouseControlServer já estava parado.");
        }

        logServersState("Estado após stop");
    }

    private static synchronized void restartServers() {
        System.out.println("Reiniciando servidores...");
        stopServers();
        startServers();
    }

    private static void logServersState(String context) {
        System.out.println(
                context
                        + " | vídeo=" + (screenServer.isRunning() ? "ativo" : "parado")
                        + " | mouse=" + (mouseServer.isRunning() ? "ativo" : "parado")
        );
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

        System.out.println("DPI scale X=" + scaleX + " Y=" + scaleY);
    }

    private static void initializeSystemTray() {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Ambiente sem interface gráfica. Tray não disponível.");
            return;
        }

        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray não suportado neste ambiente.");
            return;
        }

        EventQueue.invokeLater(() -> {
            try {
                Image trayImage = loadTrayImage();
                if (trayImage == null) {
                    System.out.println("Ícone da bandeja não encontrado em resources.");
                    return;
                }

                PopupMenu popupMenu = new PopupMenu();

                MenuItem restartItem = new MenuItem("Reiniciar");
                restartItem.addActionListener(e -> {
                    restartServers();
                    showTrayMessage(APP_NAME, "Servidores reiniciados.", MessageType.INFO);
                });

                MenuItem statusItem = new MenuItem("Status");
                statusItem.addActionListener(e ->
                        showTrayMessage(APP_NAME, buildStatusMessage(), MessageType.INFO));

                MenuItem exitItem = new MenuItem("Sair");
                exitItem.addActionListener(e -> shutdownApplication());

                popupMenu.add(restartItem);
                popupMenu.addSeparator();
                popupMenu.add(statusItem);
                popupMenu.addSeparator();
                popupMenu.add(exitItem);

                trayIcon = new TrayIcon(trayImage, APP_NAME, popupMenu);
                trayIcon.setImageAutoSize(true);

                trayIcon.addActionListener(e ->
                        showTrayMessage(APP_NAME, buildStatusMessage(), MessageType.INFO));

                SystemTray.getSystemTray().add(trayIcon);

                showTrayMessage(APP_NAME, "Aplicação iniciada com sucesso.", MessageType.INFO);

            } catch (AWTException e) {
                System.out.println("Erro ao adicionar ícone na bandeja.");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Erro inesperado ao inicializar a bandeja.");
                e.printStackTrace();
            }
        });
    }

    private static String buildStatusMessage() {
        return "Vídeo: " + (screenServer.isRunning() ? "ativo" : "parado")
                + " | Mouse: " + (mouseServer.isRunning() ? "ativo" : "parado");
    }

    private static Image loadTrayImage() {
        try (java.io.InputStream is =
                     RemoteDesktopServer.class.getClassLoader().getResourceAsStream("icon_remote_server.png")) {

            if (is == null) {
                System.out.println("Arquivo icon_remote_server.png não encontrado no classpath.");
                return null;
            }

            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(is);

            if (image == null) {
                System.out.println("Falha ao ler icon_remote_server.png.");
                return null;
            }

            return image;

        } catch (Exception e) {
            System.out.println("Erro ao carregar ícone da bandeja.");
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
            System.out.println("Shutdown já está em andamento.");
            return;
        }

        System.out.println("Iniciando encerramento da aplicação...");

        try {
            stopServers();
        } catch (Exception e) {
            System.out.println("Erro ao parar servidores.");
            e.printStackTrace();
        }

        try {
            removeTrayIcon();
        } catch (Exception e) {
            System.out.println("Erro ao remover tray icon.");
            e.printStackTrace();
        }

        try {
            releaseSingleInstance();
        } catch (Exception e) {
            System.out.println("Erro ao liberar lock da aplicação.");
            e.printStackTrace();
        }

        System.out.println("Encerramento finalizado.");

        if (exitJvm) {
            System.out.println("Finalizando JVM...");
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