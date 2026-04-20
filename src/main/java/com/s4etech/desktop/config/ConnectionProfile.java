package com.s4etech.desktop.config;

public enum ConnectionProfile {

	SATELLITE_1("SATELLITE_1", "Satélite 1", 1024, 576, 10, 1000, 20, "veryfast", "zerolatency", true),
	SATELLITE_2("SATELLITE_2", "Satélite 2", 1280, 720, 12, 1400, 24, "veryfast", "zerolatency", true),
	SATELLITE_3("SATELLITE_3", "Satélite 3", 1280, 720, 15, 1800, 30, "veryfast", "zerolatency", true),
	WIFI("WIFI", "Wi-Fi", 1280, 720, 20, 2500, 30, "veryfast", "zerolatency", true),
	LAN("LAN", "Rede local", 1920, 1080, 30, 6000, 30, "ultrafast", "zerolatency", false);

	public static final ConnectionProfile DEFAULT = LAN;

	private final String id;
	private final String displayName;
	private final int width;
	private final int height;
	private final int fps;
	private final int bitrateKbps;
	private final int keyIntMax;
	private final String encoderPreset;
	private final String encoderTune;
	private final boolean leakyQueue;

	ConnectionProfile(String id, String displayName, int width, int height, int fps, int bitrateKbps, int keyIntMax,
			String encoderPreset, String encoderTune, boolean leakyQueue) {
		this.id = id;
		this.displayName = displayName;
		this.width = width;
		this.height = height;
		this.fps = fps;
		this.bitrateKbps = bitrateKbps;
		this.keyIntMax = keyIntMax;
		this.encoderPreset = encoderPreset;
		this.encoderTune = encoderTune;
		this.leakyQueue = leakyQueue;
	}

	public String getId() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getFps() {
		return fps;
	}

	public int getBitrateKbps() {
		return bitrateKbps;
	}

	public int getKeyIntMax() {
		return keyIntMax;
	}

	public String getEncoderPreset() {
		return encoderPreset;
	}

	public String getEncoderTune() {
		return encoderTune;
	}

	public boolean isLeakyQueue() {
		return leakyQueue;
	}

	public static ConnectionProfile fromId(String id) {
		if (id == null || id.trim().isEmpty()) {
			return DEFAULT;
		}

		for (ConnectionProfile profile : values()) {
			if (profile.id.equalsIgnoreCase(id.trim())) {
				return profile;
			}
		}

		return DEFAULT;
	}
}
