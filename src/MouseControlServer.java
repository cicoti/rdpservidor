import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.sun.jna.Native;

public class MouseControlServer {

    private static final int PORT = 6000;

    private static final byte EVT_MOVE = 1;
    private static final byte EVT_LEFT_DOWN = 2;
    private static final byte EVT_LEFT_UP = 3;
    private static final byte EVT_RIGHT_DOWN = 4;
    private static final byte EVT_RIGHT_UP = 5;

    private static final byte EVT_KEY_DOWN = 10;
    private static final byte EVT_KEY_UP   = 11;

    private static double scaleX = 1.0;
    private static double scaleY = 1.0;

    private static int screenW;
    private static int screenH;

    private static Robot robot;

    // ===== DPI AWARENESS =====
    private static native boolean SetProcessDPIAware();

    private static void enableDPIAwareness() {
        try {
            Native.register("user32");
            SetProcessDPIAware();
        } catch (Throwable t) {
            System.err.println("Falha ao ativar DPI awareness: " + t.getMessage());
        }
    }
    // =========================

    public static void main(String[] args) throws Exception {

        System.out.println("Iniciando Remote Desktop Server...");

        // 1) Tornar o processo DPI aware
        enableDPIAwareness();

        // 2) Robot
        robot = new Robot();
        robot.setAutoDelay(0);
        robot.setAutoWaitForIdle(false);

        // 3) Detectar resolução
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        screenW = d.width;
        screenH = d.height;

        // 4) Detectar DPI real
        GraphicsConfiguration gc =
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice()
                        .getDefaultConfiguration();

        AffineTransform tx = gc.getDefaultTransform();
        scaleX = tx.getScaleX();
        scaleY = tx.getScaleY();

        System.out.println("DPI scale X=" + scaleX + " Y=" + scaleY);
        System.out.println("Resolucao detectada: " + screenW + " x " + screenH);

        DatagramSocket socket = new DatagramSocket(PORT);
        System.out.println("MouseControlServer rodando na porta " + PORT);

        byte[] buf = new byte[32];

        while (true) {

            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            DataInputStream dis = new DataInputStream(
                    new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));

            byte type = dis.readByte();

            switch (type) {

                case EVT_MOVE: {
                    int xNorm = dis.readInt();
                    int yNorm = dis.readInt();

                    if (xNorm < 0 || yNorm < 0) break;

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
                    int keyCode = dis.readInt();
                    robot.keyPress(keyCode);
                    break;
                }

                case EVT_KEY_UP: {
                    int keyCode = dis.readInt();
                    robot.keyRelease(keyCode);
                    break;
                }

                default:
                    // evento desconhecido
                    break;
            }
        }
    }
}
