package br.com.ctech.remote.server;

import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenStreamServer {

    private static final AtomicBoolean GST_INITIALIZED = new AtomicBoolean(false);

    private final int handshakePort;

    private volatile boolean running;
    private volatile boolean streaming;

    private volatile Thread serverThread;
    private volatile Thread handshakeThread;

    private volatile ServerHandshakeListener handshakeListener;
    private volatile Pipeline pipeline;

    private volatile InetAddress clientIp;
    private volatile int videoPort;
    private volatile long sessionId = -1;

    public ScreenStreamServer() {
        this(7000);
    }

    public ScreenStreamServer(int handshakePort) {
        this.handshakePort = handshakePort;
    }

    public synchronized void start() {
        if (running) {
            //System.out.println("ScreenStreamServer já está em execução.");
            return;
        }

        initGStreamerOnce();

        running = true;
        serverThread = new Thread(this::runServer, "ScreenStreamServer");
        serverThread.start();
    }

    public synchronized void stop() {
        if (!running && serverThread == null) {
            return;
        }

        //System.out.println("Parando ScreenStreamServer...");

        running = false;
        streaming = false;

        stopPipeline();
        stopHandshakeListener();

        Thread localServerThread = serverThread;

        if (localServerThread != null) {
            localServerThread.interrupt();
            joinQuietly(localServerThread, 2000);

            if (localServerThread.isAlive()) {
                System.err.println("A thread principal do ScreenStreamServer não encerrou no tempo esperado.");
            } else {
                //System.out.println("Thread principal do ScreenStreamServer encerrada com sucesso.");
                if (serverThread == localServerThread) {
                    serverThread = null;
                }
            }
        }

        clientIp = null;
        videoPort = 0;
        sessionId = -1;

        //System.out.println("Estado final do ScreenStreamServer | running=" + running + " | streaming=" + streaming);
    }

    public synchronized void restart() {
        stop();
        start();
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public InetAddress getClientIp() {
        return clientIp;
    }

    public int getVideoPort() {
        return videoPort;
    }

    public long getSessionId() {
        return sessionId;
    }

    private void runServer() {
        try {
            handshakeListener = new ServerHandshakeListener(handshakePort);
            handshakeThread = new Thread(handshakeListener, "handshake-listener");
            handshakeThread.start();

            //System.out.println("ScreenStreamServer pronto. Aguardando cliente...");

            while (running) {
                ServerHandshakeListener.HandshakeSession newSession = handshakeListener.consumePendingSession();

                if (newSession != null) {
                    activateSession(newSession);
                }

                Thread.sleep(200);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Erro no ScreenStreamServer: " + e.getMessage());
            e.printStackTrace();
        } finally {
            streaming = false;
            stopPipeline();
            stopHandshakeListener();
            running = false;
            clientIp = null;
            videoPort = 0;
            sessionId = -1;

            if (Thread.currentThread() == serverThread) {
                serverThread = null;
            }

            //System.out.println("Loop principal do ScreenStreamServer finalizado.");
        }
    }

    private synchronized void activateSession(ServerHandshakeListener.HandshakeSession session) {
        if (!running) {
            return;
        }

        this.sessionId = session.getSessionId();
        this.clientIp = session.getClientAddress();
        this.videoPort = session.getClientVideoPort();

        System.out.println(
                "Ativando sessão " + sessionId +
                " para " + clientIp.getHostAddress() +
                ":" + videoPort
        );

        restartPipelineForCurrentSession();
    }

    private void restartPipelineForCurrentSession() {
        stopPipeline();

        String pipelineStr = buildPipeline(clientIp, videoPort);

        pipeline = (Pipeline) Gst.parseLaunch(pipelineStr);

        pipeline.getBus().connect((Bus.MESSAGE) (bus, msg) -> {
            System.out.println(msg);
        });

        System.out.println(
                "Servidor de tela enviando para " +
                clientIp.getHostAddress() + ":" + videoPort
        );

        pipeline.play();
        streaming = true;
    }

    private String buildPipeline(InetAddress clientIp, int videoPort) {
        return "d3d11screencapturesrc ! queue ! d3d11convert ! d3d11download ! videoconvert ! " +
               "video/x-raw,framerate=30/1 ! " +
               "x264enc tune=zerolatency speed-preset=ultrafast bitrate=6000 key-int-max=30 ! " +
               "h264parse ! rtph264pay pt=96 config-interval=1 ! " +
               "udpsink host=" + clientIp.getHostAddress() +
               " port=" + videoPort +
               " sync=false";
    }

    private void stopPipeline() {
        Pipeline localPipeline = pipeline;
        pipeline = null;

        if (localPipeline != null) {
            try {
                localPipeline.stop();
            } catch (Exception e) {
                System.err.println("Falha ao parar pipeline: " + e.getMessage());
            }

            try {
                localPipeline.dispose();
            } catch (Exception e) {
                System.err.println("Falha ao liberar pipeline: " + e.getMessage());
            }
        }

        streaming = false;
    }

    private void stopHandshakeListener() {
        ServerHandshakeListener localHandshakeListener = handshakeListener;
        Thread localHandshakeThread = handshakeThread;

        handshakeListener = null;

        if (localHandshakeListener != null) {
            localHandshakeListener.stop();
        }

        if (localHandshakeThread != null) {
            localHandshakeThread.interrupt();
            joinQuietly(localHandshakeThread, 1000);

            if (localHandshakeThread.isAlive()) {
                //System.err.println("A thread de handshake não encerrou no tempo esperado.");
            } else {
                //System.out.println("Thread de handshake encerrada com sucesso.");
                if (handshakeThread == localHandshakeThread) {
                    handshakeThread = null;
                }
            }
        } else {
            handshakeThread = null;
        }
    }

    private static void initGStreamerOnce() {
        if (GST_INITIALIZED.compareAndSet(false, true)) {
            Gst.init("ScreenStreamServer", new String[0]);
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
        ScreenStreamServer server = new ScreenStreamServer();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        Thread.currentThread().join();
    }
}