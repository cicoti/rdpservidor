package com.s4etech.desktop.config;

public class ConnectionProfile {

	public static final String DEFAULT_ID = "LAN";
	public static final String DEFAULT_DISPLAY_NAME = "Rede local";
	public static final int DEFAULT_WIDTH = 1920;
	public static final int DEFAULT_HEIGHT = 1080;
	public static final int DEFAULT_FPS = 30;
	public static final int DEFAULT_BITRATE_KBPS = 6000;
	public static final int DEFAULT_KEY_INT_MAX = 30;
	public static final String DEFAULT_ENCODER_PRESET = "ultrafast";
	public static final String DEFAULT_ENCODER_TUNE = "zerolatency";
	public static final boolean DEFAULT_LEAKY_QUEUE = false;

	public static final ConnectionProfile DEFAULT = new ConnectionProfile(DEFAULT_ID, DEFAULT_DISPLAY_NAME,
			DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_FPS, DEFAULT_BITRATE_KBPS, DEFAULT_KEY_INT_MAX,
			DEFAULT_ENCODER_PRESET, DEFAULT_ENCODER_TUNE, DEFAULT_LEAKY_QUEUE);

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

	public ConnectionProfile(String id, String displayName, int width, int height, int fps, int bitrateKbps,
			int keyIntMax, String encoderPreset, String encoderTune, boolean leakyQueue) {
		this.id = normalizeText(id, DEFAULT_ID);
		this.displayName = normalizeText(displayName, DEFAULT_DISPLAY_NAME);
		this.width = width;
		this.height = height;
		this.fps = fps;
		this.bitrateKbps = bitrateKbps;
		this.keyIntMax = keyIntMax;
		this.encoderPreset = normalizeText(encoderPreset, DEFAULT_ENCODER_PRESET);
		this.encoderTune = normalizeText(encoderTune, DEFAULT_ENCODER_TUNE);
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

	public String toPropertyValue() {
		return String.join(",", id, displayName, String.valueOf(width), String.valueOf(height), String.valueOf(fps),
				String.valueOf(bitrateKbps), String.valueOf(keyIntMax), encoderPreset, encoderTune,
				String.valueOf(leakyQueue));
	}

	public static ConnectionProfile fromPropertyValue(String value) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("Valor do perfil de conexão ausente.");
		}

		String[] parts = value.split(",", -1);

		if (parts.length != 10) {
			throw new IllegalArgumentException(
					"Perfil de conexão inválido. Esperado 10 campos, encontrado " + parts.length + ".");
		}

		String id = normalizeText(parts[0], DEFAULT_ID);
		String displayName = normalizeText(parts[1], DEFAULT_DISPLAY_NAME);
		int width = parsePositiveInt(parts[2], "width");
		int height = parsePositiveInt(parts[3], "height");
		int fps = parsePositiveInt(parts[4], "fps");
		int bitrateKbps = parsePositiveInt(parts[5], "bitrateKbps");
		int keyIntMax = parsePositiveInt(parts[6], "keyIntMax");
		String encoderPreset = normalizeText(parts[7], DEFAULT_ENCODER_PRESET);
		String encoderTune = normalizeText(parts[8], DEFAULT_ENCODER_TUNE);
		boolean leakyQueue = Boolean.parseBoolean(parts[9].trim());

		return new ConnectionProfile(id, displayName, width, height, fps, bitrateKbps, keyIntMax, encoderPreset,
				encoderTune, leakyQueue);
	}

	private static int parsePositiveInt(String value, String fieldName) {
		try {
			int parsed = Integer.parseInt(value.trim());
			if (parsed > 0) {
				return parsed;
			}
		} catch (Exception e) {
			// fallback below
		}

		throw new IllegalArgumentException("Valor inválido para " + fieldName + ": " + value);
	}

	private static String normalizeText(String value, String defaultValue) {
		if (value == null || value.trim().isEmpty()) {
			return defaultValue;
		}
		return value.trim();
	}

	@Override
	public String toString() {
		return displayName;
	}

	@Override
	public int hashCode() {
		return id.toUpperCase().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ConnectionProfile other)) {
			return false;
		}
		return id.equalsIgnoreCase(other.id);
	}
}
