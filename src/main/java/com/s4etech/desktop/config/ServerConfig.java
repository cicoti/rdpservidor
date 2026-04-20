package com.s4etech.desktop.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerConfig {

	public static final int DEFAULT_HANDSHAKE_PORT = 7000;
	public static final int DEFAULT_CONTROL_PORT = 5000;
	public static final ConnectionProfile DEFAULT_CONNECTION_PROFILE = ConnectionProfile.DEFAULT;

	private int handshakePort;
	private int controlPort;
	private ConnectionProfile connectionProfile;
	private List<ConnectionProfile> availableProfiles;

	public ServerConfig() {
		this.handshakePort = DEFAULT_HANDSHAKE_PORT;
		this.controlPort = DEFAULT_CONTROL_PORT;
		this.connectionProfile = DEFAULT_CONNECTION_PROFILE;
		this.availableProfiles = new ArrayList<>();
		this.availableProfiles.add(DEFAULT_CONNECTION_PROFILE);
	}

	public ServerConfig(int handshakePort, int controlPort) {
		this(handshakePort, controlPort, DEFAULT_CONNECTION_PROFILE);
	}

	public ServerConfig(int handshakePort, int controlPort, ConnectionProfile connectionProfile) {
		this.handshakePort = handshakePort;
		this.controlPort = controlPort;
		this.connectionProfile = connectionProfile != null ? connectionProfile : DEFAULT_CONNECTION_PROFILE;
		this.availableProfiles = new ArrayList<>();
		this.availableProfiles.add(this.connectionProfile);
		ensureDefaultProfileAvailable();
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
		ensureSelectedProfileAvailable();
	}

	public List<ConnectionProfile> getAvailableProfiles() {
		return Collections.unmodifiableList(availableProfiles);
	}

	public void setAvailableProfiles(List<ConnectionProfile> availableProfiles) {
		this.availableProfiles = new ArrayList<>();

		if (availableProfiles != null) {
			for (ConnectionProfile profile : availableProfiles) {
				if (profile != null && !this.availableProfiles.contains(profile)) {
					this.availableProfiles.add(profile);
				}
			}
		}

		ensureDefaultProfileAvailable();
		ensureSelectedProfileAvailable();
	}

	private void ensureDefaultProfileAvailable() {
		if (!availableProfiles.contains(DEFAULT_CONNECTION_PROFILE)) {
			availableProfiles.add(DEFAULT_CONNECTION_PROFILE);
		}
	}

	private void ensureSelectedProfileAvailable() {
		if (connectionProfile == null) {
			connectionProfile = DEFAULT_CONNECTION_PROFILE;
		}
		if (!availableProfiles.contains(connectionProfile)) {
			availableProfiles.add(connectionProfile);
		}
	}
}
