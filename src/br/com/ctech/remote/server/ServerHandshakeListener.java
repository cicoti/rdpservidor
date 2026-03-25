package br.com.ctech.remote.server;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ServerHandshakeListener implements Runnable, AutoCloseable {

    public static final byte TYPE_HELLO = 100;
    public static final byte TYPE_ACK = 101;

    private final int port;

    private volatile boolean running;
    private volatile DatagramSocket socket;

    private volatile InetAddress clientAddress;
    private volatile int clientVideoPort;
    private volatile int clientControlPort;

    public ServerHandshakeListener(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        resetClient();

        try {
            running = true;
            socket = new DatagramSocket(port);

            System.out.println("Handshake server escutando na porta " + port);

            while (running && !hasClient()) {
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                try {
                    socket.receive(packet);
                    processPacket(packet);
                } catch (SocketException e) {
                    if (running) {
                        System.err.println("Erro no socket do handshake: " + e.getMessage());
                    }
                    break;
                }
            }

        } catch (Exception e) {
            if (running) {
                System.err.println("Erro no ServerHandshakeListener: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            running = false;
            closeSocket();
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

        if (type != TYPE_HELLO) {
            return;
        }

        String ip = dis.readUTF();
        int videoPort = dis.readInt();
        int controlPort = dis.readInt();

        clientAddress = packet.getAddress();
        clientVideoPort = videoPort;
        clientControlPort = controlPort;

        byte[] ack = new byte[] { TYPE_ACK };
        DatagramPacket ackPacket = new DatagramPacket(
                ack,
                ack.length,
                packet.getAddress(),
                packet.getPort()
        );
        socket.send(ackPacket);

        System.out.println("Cliente conectado: " + ip +
                " videoPort=" + videoPort +
                " controlPort=" + controlPort);

        running = false;
    }

    public synchronized void stop() {
        running = false;
        closeSocket();
    }

    @Override
    public void close() {
        stop();
    }

    public boolean isRunning() {
        return running;
    }

    public boolean hasClient() {
        return clientAddress != null;
    }

    public InetAddress getClientAddress() {
        return clientAddress;
    }

    public int getClientVideoPort() {
        return clientVideoPort;
    }

    public int getClientControlPort() {
        return clientControlPort;
    }

    private void resetClient() {
        clientAddress = null;
        clientVideoPort = 0;
        clientControlPort = 0;
    }

    private void closeSocket() {
        DatagramSocket localSocket = socket;
        socket = null;

        if (localSocket != null && !localSocket.isClosed()) {
            localSocket.close();
        }
    }
}