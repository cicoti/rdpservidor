import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Bus;

import java.net.InetAddress;

public class ScreenStreamServer {

    public static void main(String[] args) throws Exception {

        Gst.init("ScreenStreamServer", args);

        int handshakePort = 7000;

        // =========================
        // 1) inicia handshake server
        // =========================
        ServerHandshakeListener handshake = new ServerHandshakeListener(handshakePort);
        Thread hsThread = new Thread(handshake, "handshake-listener");
        hsThread.start();

        System.out.println("Aguardando cliente...");

        // =========================
        // 2) espera cliente se apresentar
        // =========================
        while (!handshake.hasClient()) {
            Thread.sleep(200);
        }

        InetAddress clientIp = handshake.getClientAddress();
        int videoPort = handshake.getClientVideoPort();

        System.out.println("Cliente detectado: " + clientIp.getHostAddress());

        // =========================
        // 3) monta pipeline dinamicamente
        // =========================
        String pipelineStr =
                "d3d11screencapturesrc ! queue ! d3d11convert ! d3d11download ! videoconvert ! " +
                "video/x-raw,framerate=30/1 ! " +
                "x264enc tune=zerolatency speed-preset=ultrafast bitrate=6000 key-int-max=30 ! " +
                "h264parse ! rtph264pay pt=96 config-interval=1 ! " +
                "udpsink host=" + clientIp.getHostAddress() +
                " port=" + videoPort +
                " sync=false";

        Pipeline pipeline = (Pipeline) Gst.parseLaunch(pipelineStr);

        pipeline.getBus().connect((Bus.MESSAGE) (bus, msg) -> {
            System.out.println(msg);
        });

        System.out.println("Servidor de tela enviando para " +
                clientIp.getHostAddress() + ":" + videoPort);

        pipeline.play();

        Gst.main();
    }
}
