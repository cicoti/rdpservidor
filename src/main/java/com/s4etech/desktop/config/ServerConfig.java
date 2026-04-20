package com.s4etech.desktop.config;

public class ServerConfig {

	public static final int DEFAULT_HANDSHAKE_PORT = 7000;
	public static final int DEFAULT_CONTROL_PORT = 5000;
	public static final ConnectionProfile DEFAULT_CONNECTION_PROFILE = ConnectionProfile.DEFAULT;

	private int handshakePort;
	private int controlPort;
	private ConnectionProfile connectionProfile;

	public ServerConfig() {
		this.handshakePort = DEFAULT_HANDSHAKE_PORT;
		this.controlPort = DEFAULT_CONTROL_PORT;
		this.connectionProfile = DEFAULT_CONNECTION_PROFILE;
	}

	public ServerConfig(int handshakePort, int controlPort) {
		this(handshakePort, controlPort, DEFAULT_CONNECTION_PROFILE);
	}

	public ServerConfig(int handshakePort, int controlPort, ConnectionProfile connectionProfile) {
		this.handshakePort = handshakePort;
		this.controlPort = controlPort;
		this.connectionProfile = connectionProfile != null ? connectionProfile : DEFAULT_CONNECTION_PROFILE;
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

	public ConnectionProfile getConnectionProfile() {
		return connectionProfile;
	}

	public void setConnectionProfile(ConnectionProfile connectionProfile) {
		this.connectionProfile = connectionProfile != null ? connectionProfile : DEFAULT_CONNECTION_PROFILE;
	}
}
