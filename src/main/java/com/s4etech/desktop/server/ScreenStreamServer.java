package com.s4etech.desktop.server;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;

import com.s4etech.desktop.config.ConnectionProfile;
import com.s4etech.desktop.listener.ServerHandshakeListener;
import com.s4etech.desktop.session.ActiveSessionContext;

public class ScreenStreamServer {

    private static final AtomicBoolean GST_INITIALIZED = new AtomicBoolean(false);
    private static final int MAX_PIPELINE_START_ATTEMPTS = 3;
    private static final long PIPELINE_RETRY_DELAY_MS = 1000L;

    private final int handshakePort;
    private final ConnectionProfile connectionProfile;
    private final ActiveSessionContext activeSessionContext;

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
        this(7000, ConnectionProfile.DEFAULT, new ActiveSessionContext());
    }

    public ScreenStreamServer(int handshakePort) {
        this(handshakePort, ConnectionProfile.DEFAULT, new ActiveSessionContext());
    }

    public ScreenStreamServer(int handshakePort, ConnectionProfile connectionProfile) {
        this(handshakePort, connectionProfile, new ActiveSessionContext());
    }

    public ScreenStreamServer(int handshakePort, ConnectionProfile connectionProfile, ActiveSessionContext activeSessionContext) {
        this.handshakePort = handshakePort;
        this.connectionProfile = connectionProfile != null ? connectionProfile : ConnectionProfile.DEFAULT;
        this.activeSessionContext = activeSessionContext != null ? activeSessionContext : new ActiveSessionContext();
    }

    public synchronized void start() {
        if (running) {
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
                if (serverThread == localServerThread) {
                    serverThread = null;
                }
            }
        }

        clearCurrentSession();
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

    public ConnectionProfile getConnectionProfile() {
        return connectionProfile;
    }

    private void runServer() {
        try {
            handshakeListener = new ServerHandshakeListener(handshakePort);
            handshakeThread = new Thread(handshakeListener, "handshake-listener");
            handshakeThread.start();

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
            clearCurrentSession();

            if (Thread.currentThread() == serverThread) {
                serverThread = null;
            }
        }
    }

    private synchronized void activateSession(ServerHandshakeListener.HandshakeSession session) {
        if (!isValidSession(session)) {
            System.out.println("Sessão de handshake ignorada por ser inválida.");
            return;
        }

        InetAddress newClientIp = session.getClientAddress();
        int newVideoPort = session.getClientVideoPort();
        int newControlPort = session.getClientControlPort();
        long newSessionId = session.getSessionId();

        if (isSameStreamingTarget(newClientIp, newVideoPort)) {
            this.sessionId = newSessionId;
            activeSessionContext.activate(newSessionId, newClientIp, newVideoPort, newControlPort);

            System.out.println("Sessão redundante detectada e reaproveitada | sessionId=" + newSessionId
                    + " | destino=" + newClientIp.getHostAddress() + ":" + newVideoPort);
            return;
        }

        if (clientIp != null && !clientIp.equals(newClientIp)) {
            System.out.println("Nova sessão assumindo transmissão | anterior=" + clientIp.getHostAddress() + ":"
                    + videoPort + " | nova=" + newClientIp.getHostAddress() + ":" + newVideoPort
                    + " | sessionId=" + newSessionId);
        }

        this.sessionId = newSessionId;
        this.clientIp = newClientIp;
        this.videoPort = newVideoPort;

        activeSessionContext.activate(newSessionId, newClientIp, newVideoPort, newControlPort);

        System.out.println("Ativando sessão " + sessionId + " para " + clientIp.getHostAddress() + ":" + videoPort
                + " | perfil=" + connectionProfile.getDisplayName());

        restartPipelineForCurrentSession();
    }

    private boolean isValidSession(ServerHandshakeListener.HandshakeSession session) {
        return running
                && session != null
                && session.getClientAddress() != null
                && isValidPort(session.getClientVideoPort())
                && isValidPort(session.getClientControlPort());
    }

    private boolean isSameStreamingTarget(InetAddress newClientIp, int newVideoPort) {
        return streaming
                && clientIp != null
                && clientIp.equals(newClientIp)
                && videoPort == newVideoPort;
    }

    private boolean isValidPort(int port) {
        return port > 0 && port <= 65535;
    }

    private void restartPipelineForCurrentSession() {
        stopPipeline();

        boolean started = tryStartPipelineWithRetry();
        if (!started) {
            System.err.println("Falha definitiva ao iniciar pipeline para "
                    + clientIp.getHostAddress() + ":" + videoPort + " | sessionId=" + sessionId);
            streaming = false;
        }
    }

    private boolean tryStartPipelineWithRetry() {
        for (int attempt = 1; attempt <= MAX_PIPELINE_START_ATTEMPTS; attempt++) {
            if (!running || clientIp == null || !isValidPort(videoPort)) {
                return false;
            }

            try {
                startPipelineOnce();
                System.out.println("Pipeline iniciado com sucesso | tentativa=" + attempt
                        + " | destino=" + clientIp.getHostAddress() + ":" + videoPort);
                return true;
            } catch (Exception e) {
                streaming = false;
                stopPipeline();

                System.err.println("Falha ao iniciar pipeline | tentativa=" + attempt
                        + " | destino=" + clientIp.getHostAddress() + ":" + videoPort
                        + " | erro=" + e.getMessage());

                if (attempt < MAX_PIPELINE_START_ATTEMPTS) {
                    sleepQuietly(PIPELINE_RETRY_DELAY_MS);
                }
            }
        }

        return false;
    }

    private void startPipelineOnce() {
        String pipelineStr = buildPipeline(clientIp, videoPort);

        pipeline = (Pipeline) Gst.parseLaunch(pipelineStr);

        System.out.println("Servidor de tela enviando para " + clientIp.getHostAddress() + ":" + videoPort
                + " | perfil=" + connectionProfile.getDisplayName() + " | " + connectionProfile.getWidth() + "x"
                + connectionProfile.getHeight() + " @" + connectionProfile.getFps() + "fps" + " bitrate="
                + connectionProfile.getBitrateKbps() + "kbps");

        pipeline.play();
        streaming = true;
    }

    private String buildPipeline(InetAddress clientIp, int videoPort) {
        String queueSegment = connectionProfile.isLeakyQueue()
                ? "queue leaky=downstream max-size-buffers=2 ! "
                : "queue ! ";

        return "dx9screencapsrc monitor=0 cursor=false ! "
                + queueSegment
                + "videoconvert ! videoscale ! "
                + "video/x-raw,format=I420,width=" + connectionProfile.getWidth()
                + ",height=" + connectionProfile.getHeight()
                + ",framerate=" + connectionProfile.getFps() + "/1 ! "
                + "x264enc tune=" + connectionProfile.getEncoderTune()
                + " speed-preset=" + connectionProfile.getEncoderPreset()
                + " bitrate=" + connectionProfile.getBitrateKbps()
                + " key-int-max=" + connectionProfile.getKeyIntMax()
                + " ! h264parse ! rtph264pay pt=96 config-interval=1 ! "
                + "udpsink host=" + clientIp.getHostAddress()
                + " port=" + videoPort
                + " sync=false async=false";
    }

    private void stopPipeline() {
        Pipeline localPipeline = pipeline;
        pipeline = null;

        if (localPipeline != null) {
            System.out.println("stopPipeline: iniciando parada do pipeline");

            try {
                localPipeline.stop();
                System.out.println("stopPipeline: pipeline parado com sucesso");
            } catch (Exception e) {
                System.err.println("Falha ao parar pipeline: " + e.getMessage());
            }

            try {
                localPipeline.dispose();
                System.out.println("stopPipeline: pipeline liberado com sucesso");
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

            if (!localHandshakeThread.isAlive()) {
                if (handshakeThread == localHandshakeThread) {
                    handshakeThread = null;
                }
            }
        } else {
            handshakeThread = null;
        }
    }

    private void clearCurrentSession() {
        clientIp = null;
        videoPort = 0;
        sessionId = -1;
        activeSessionContext.clear();
    }

    private static void initGStreamerOnce() {
        if (GST_INITIALIZED.compareAndSet(false, true)) {
            Gst.init("ScreenStreamServer", new String[0]);
        }
    }

    public static void shutdownGStreamer() {
        if (GST_INITIALIZED.compareAndSet(true, false)) {
            try {
                System.out.println("shutdownGStreamer: iniciando Gst.deinit()");
                Gst.deinit();
                System.out.println("shutdownGStreamer: Gst.deinit() executado com sucesso");
            } catch (Exception e) {
                System.err.println("Falha ao finalizar GStreamer: " + e.getMessage());
            }
        }
    }

    private static void joinQuietly(Thread thread, long timeoutMillis) {
        try {
            thread.join(timeoutMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
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
