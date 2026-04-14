package com.s4etech.desktop.config;

public class ServerConfig {

    public static final int DEFAULT_HANDSHAKE_PORT = 7000;
    public static final int DEFAULT_CONTROL_PORT = 5000;

    private int handshakePort;
    private int controlPort;

    public ServerConfig() {
        this.handshakePort = DEFAULT_HANDSHAKE_PORT;
        this.controlPort = DEFAULT_CONTROL_PORT;
    }

    public ServerConfig(int handshakePort, int controlPort) {
        this.handshakePort = handshakePort;
        this.controlPort = controlPort;
    }

    public int getHandshakePort() {
        return handshakePort;
    }

    public void setHandshakePort(int handshakePort) {
        this.handshakePort = handshakePort;
    }

    public int getControlPort() {
        return controlPort;
    }

    public void setControlPort(int controlPort) {
        this.controlPort = controlPort;
    }
}