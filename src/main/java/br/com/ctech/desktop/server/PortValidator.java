package br.com.ctech.desktop.server;

import java.net.DatagramSocket;

public class PortValidator {

    private PortValidator() {
    }

    public static boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }

    public static boolean isUdpPortAvailable(int port) {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            socket.setReuseAddress(false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}