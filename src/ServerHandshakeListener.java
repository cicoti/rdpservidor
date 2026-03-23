import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ServerHandshakeListener implements Runnable {

    public static final byte TYPE_HELLO = 100;

    private final int port;
    private volatile InetAddress clientAddress;
    private volatile int clientVideoPort;
    private volatile int clientControlPort;

    public ServerHandshakeListener(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {

            System.out.println("Handshake server escutando na porta " + port);

            byte[] buf = new byte[256];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                DataInputStream dis = new DataInputStream(
                        new ByteArrayInputStream(packet.getData(), 0, packet.getLength())
                );

                byte type = dis.readByte();

                if (type == TYPE_HELLO) {

                	String ip = dis.readUTF();
                	int videoPort = dis.readInt();
                	int controlPort = dis.readInt();

                	clientAddress = packet.getAddress();
                	clientVideoPort = videoPort;
                	clientControlPort = controlPort;

                    byte[] ack = new byte[] { 101 };
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
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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
}
