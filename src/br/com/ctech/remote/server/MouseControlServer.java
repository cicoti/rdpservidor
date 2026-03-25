package br.com.ctech.remote.server;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

public class MouseControlServer {

    private static final byte EVT_MOVE = 1;
    private static final byte EVT_LEFT_DOWN = 2;
    private static final byte EVT_LEFT_UP = 3;
    private static final byte EVT_RIGHT_DOWN = 4;
    private static final byte EVT_RIGHT_UP = 5;
    private static final byte EVT_KEY_DOWN = 10;
    private static final byte EVT_KEY_UP = 11;
    private static final byte EVT_KEY_TYPED = 12;

    private final int port;

    private volatile boolean running;
    private volatile Thread serverThread;
    private volatile DatagramSocket socket;

    private volatile double scaleX = 1.0;
    private volatile double scaleY = 1.0;

    private volatile int screenW;
    private volatile int screenH;

    private Robot robot;

    private static native boolean SetProcessDPIAware();

    public MouseControlServer() {
        this(6000);
    }

    public MouseControlServer(int port) {
        this.port = port;
    }

    public synchronized void start() {
        if (running) {
            System.out.println("MouseControlServer já está em execução.");
            return;
        }

        running = true;
        serverThread = new Thread(this::runServer, "MouseControlServer");
        serverThread.start();
    }

    public synchronized void stop() {
        if (!running && serverThread == null) {
            return;
        }

        System.out.println("Parando MouseControlServer...");

        running = false;
        closeSocket();

        if (serverThread != null) {
            serverThread.interrupt();
            joinQuietly(serverThread, 2000);
            serverThread = null;
        }

        System.out.println("MouseControlServer parado.");
    }

    public synchronized void restart() {
        stop();
        start();
    }

    public boolean isRunning() {
        return running;
    }

    private void runServer() {
        try {
            enableDPIAwareness();
            initRobot();
            detectScreenConfiguration();

            socket = new DatagramSocket(port);
            System.out.println("MouseControlServer rodando na porta " + port);

            while (running) {
                byte[] buf = new byte[32];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                try {
                    socket.receive(packet);
                    processPacket(packet);
                } catch (SocketException e) {
                    if (running) {
                        System.err.println("SocketException no MouseControlServer: " + e.getMessage());
                    }
                    break;
                } catch (Exception e) {
                    System.err.println("Falha ao processar pacote de "
                            + packet.getAddress().getHostAddress()
                            + ":" + packet.getPort()
                            + " -> " + e.getClass().getSimpleName()
                            + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Erro no MouseControlServer: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeSocket();
            running = false;
        }
    }

    private void initRobot() throws Exception {
        robot = new Robot();
        robot.setAutoDelay(0);
        robot.setAutoWaitForIdle(false);
    }

    private void detectScreenConfiguration() {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        screenW = d.width;
        screenH = d.height;

        GraphicsConfiguration gc =
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice()
                        .getDefaultConfiguration();

        AffineTransform tx = gc.getDefaultTransform();
        scaleX = tx.getScaleX();
        scaleY = tx.getScaleY();

        System.out.println("DPI scale X=" + scaleX + " Y=" + scaleY);
        System.out.println("Resolucao detectada: " + screenW + " x " + screenH);
    }

    private static void enableDPIAwareness() {
        try {
            Native.register("user32");
            SetProcessDPIAware();
        } catch (Throwable t) {
            System.err.println("Falha ao ativar DPI awareness: " + t.getMessage());
        }
    }

    private void processPacket(DatagramPacket packet) throws Exception {
        if (packet == null || packet.getLength() <= 0) {
            return;
        }

        DataInputStream dis = new DataInputStream(
                new ByteArrayInputStream(packet.getData(), 0, packet.getLength())
        );

        byte type = dis.readByte();

        switch (type) {
            case EVT_MOVE: {
                if (packet.getLength() < 9) {
                    return;
                }

                int xNorm = dis.readInt();
                int yNorm = dis.readInt();

                if (xNorm < 0 || xNorm > 10000 || yNorm < 0 || yNorm > 10000) {
                    return;
                }

                int logicalX = (int) ((xNorm / 10000.0) * screenW);
                int logicalY = (int) ((yNorm / 10000.0) * screenH);

                int realX = (int) (logicalX * scaleX);
                int realY = (int) (logicalY * scaleY);

                robot.mouseMove(realX, realY);
                break;
            }

            case EVT_LEFT_DOWN:
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                break;

            case EVT_LEFT_UP:
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                break;

            case EVT_RIGHT_DOWN:
                robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                break;

            case EVT_RIGHT_UP:
                robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                break;

            case EVT_KEY_DOWN: {
                if (packet.getLength() < 5) {
                    return;
                }

                int keyCode = dis.readInt();
                if (keyCode > 0) {
                    robot.keyPress(keyCode);
                }
                break;
            }

            case EVT_KEY_UP: {
                if (packet.getLength() < 5) {
                    return;
                }

                int keyCode = dis.readInt();
                if (keyCode > 0) {
                    robot.keyRelease(keyCode);
                }
                break;
            }

            case EVT_KEY_TYPED: {
                if (packet.getLength() < 3) {
                    return;
                }

                char ch = dis.readChar();
                sendUnicodeChar(ch);
                break;
            }

            default:
                break;
        }
    }

    private void sendUnicodeChar(char ch) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki");
        input.input.ki.wVk = new WinDef.WORD(0);
        input.input.ki.wScan = new WinDef.WORD(ch);
        input.input.ki.dwFlags = new WinDef.DWORD(WinUser.KEYBDINPUT.KEYEVENTF_UNICODE);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);

        WinUser.INPUT inputUp = new WinUser.INPUT();
        inputUp.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        inputUp.input.setType("ki");
        inputUp.input.ki.wVk = new WinDef.WORD(0);
        inputUp.input.ki.wScan = new WinDef.WORD(ch);
        inputUp.input.ki.dwFlags = new WinDef.DWORD(
                WinUser.KEYBDINPUT.KEYEVENTF_UNICODE | WinUser.KEYBDINPUT.KEYEVENTF_KEYUP
        );
        inputUp.input.ki.time = new WinDef.DWORD(0);
        inputUp.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);

        User32.INSTANCE.SendInput(
                new WinDef.DWORD(1),
                (WinUser.INPUT[]) input.toArray(1),
                input.size()
        );

        User32.INSTANCE.SendInput(
                new WinDef.DWORD(1),
                (WinUser.INPUT[]) inputUp.toArray(1),
                inputUp.size()
        );
    }

    private void closeSocket() {
        DatagramSocket localSocket = socket;
        socket = null;

        if (localSocket != null && !localSocket.isClosed()) {
            localSocket.close();
        }
    }

    private static void joinQuietly(Thread thread, long timeoutMillis) {
        try {
            thread.join(timeoutMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws Exception {
        MouseControlServer server = new MouseControlServer();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        Thread.currentThread().join();
    }
}