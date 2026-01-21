import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;

public class RemoteDesktopServer {

	private static native boolean setProcessDPIAware();
	
    public static void main(String[] args) {

        System.out.println("Iniciando Remote Desktop Server...");
        
        enableDPIAwareness();
        
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
        

        // Thread do vídeo
        Thread videoThread = new Thread(() -> {
            try {
                ScreenStreamServer.main(new String[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "VideoServer");

        // Thread do mouse
        Thread mouseThread = new Thread(() -> {
            try {
                MouseControlServer.main(new String[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "MouseServer");

        videoThread.start();
        mouseThread.start();

        System.out.println("Servidor de vídeo e mouse iniciados.");
    }
    
    public static void enableDPIAwareness() {
        try {
            com.sun.jna.Native.register("user32");
            setProcessDPIAware();
        } catch (Throwable ignored) {
        }
    }
}
