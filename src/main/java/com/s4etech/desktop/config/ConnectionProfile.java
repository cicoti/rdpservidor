package com.s4etech.desktop.config;

import java.text.Normalizer;
import java.util.Locale;

public class ConnectionProfile {

	public static final String DEFAULT_ENCODER_PRESET = "ultrafast";
	public static final String DEFAULT_ENCODER_TUNE = "zerolatency";
	public static final boolean DEFAULT_LEAKY_QUEUE = false;
	public static final String DEFAULT_CAPTURE_SOURCE = "dx9"; //"dx9";

	public static final String LAN_ID = "LAN";
	public static final String LAN_DISPLAY_NAME = "Rede local";
	public static final int LAN_WIDTH = 1920;
	public static final int LAN_HEIGHT = 1080;
	public static final int LAN_FPS = 30;
	public static final int LAN_BITRATE_KBPS = 6000;
	public static final int LAN_KEY_INT_MAX = 30;
	public static final String LAN_ENCODER_PRESET = "ultrafast";
	public static final String LAN_ENCODER_TUNE = "zerolatency";
	public static final boolean LAN_LEAKY_QUEUE = false;
	public static final String LAN_CAPTURE_SOURCE = DEFAULT_CAPTURE_SOURCE;

	public static final String WIFI_ID = "WIFI";
	public static final String WIFI_DISPLAY_NAME = "Wi-Fi";
	public static final int WIFI_WIDTH = 1280;
	public static final int WIFI_HEIGHT = 720;
	public static final int WIFI_FPS = 20;
	public static final int WIFI_BITRATE_KBPS = 2500;
	public static final int WIFI_KEY_INT_MAX = 24;
	public static final String WIFI_ENCODER_PRESET = "veryfast";
	public static final String WIFI_ENCODER_TUNE = "zerolatency";
	public static final boolean WIFI_LEAKY_QUEUE = true;
	public static final String WIFI_CAPTURE_SOURCE = DEFAULT_CAPTURE_SOURCE;

	public static final ConnectionProfile LAN = new ConnectionProfile(LAN_ID, LAN_DISPLAY_NAME, LAN_WIDTH, LAN_HEIGHT,
			LAN_FPS, LAN_BITRATE_KBPS, LAN_KEY_INT_MAX, LAN_ENCODER_PRESET, LAN_ENCODER_TUNE, LAN_LEAKY_QUEUE,
			LAN_CAPTURE_SOURCE, true);

	public static final ConnectionProfile WIFI = new ConnectionProfile(WIFI_ID, WIFI_DISPLAY_NAME, WIFI_WIDTH,
			WIFI_HEIGHT, WIFI_FPS, WIFI_BITRATE_KBPS, WIFI_KEY_INT_MAX, WIFI_ENCODER_PRESET, WIFI_ENCODER_TUNE,
			WIFI_LEAKY_QUEUE, WIFI_CAPTURE_SOURCE, true);

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
	private final String captureSource;
	private final boolean systemProfile;

	public ConnectionProfile(String id, String displayName, int width, int height, int fps, int bitrateKbps,
			int keyIntMax, String encoderPreset, String encoderTune, boolean leakyQueue, String captureSource) {
		this(id, displayName, width, height, fps, bitrateKbps, keyIntMax, encoderPreset, encoderTune, leakyQueue,
				captureSource, false);
	}

	public ConnectionProfile(String id, String displayName, int width, int height, int fps, int bitrateKbps,
			int keyIntMax, String encoderPreset, String encoderTune, boolean leakyQueue, String captureSource,
			boolean systemProfile) {
		this.id = normalizeId(id);
		this.displayName = normalizeText(displayName, this.id);
		this.width = width;
		this.height = height;
		this.fps = fps;
		this.bitrateKbps = bitrateKbps;
		this.keyIntMax = keyIntMax;
		this.encoderPreset = normalizeText(encoderPreset, DEFAULT_ENCODER_PRESET);
		this.encoderTune = normalizeText(encoderTune, DEFAULT_ENCODER_TUNE);
		this.leakyQueue = leakyQueue;
		this.captureSource = normalizeCaptureSource(captureSource);
		this.systemProfile = systemProfile;
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

	public String getCaptureSource() {
		return captureSource;
	}

	public boolean isSystemProfile() {
		return systemProfile;
	}

	public String toPropertyValue() {
		return String.join(",", id, displayName, String.valueOf(width), String.valueOf(height), String.valueOf(fps),
				String.valueOf(bitrateKbps), String.valueOf(keyIntMax), encoderPreset, encoderTune,
				String.valueOf(leakyQueue), captureSource);
	}

	public ConnectionProfile copyAsCustom(String newId, String newDisplayName) {
		return new ConnectionProfile(newId, newDisplayName, width, height, fps, bitrateKbps, keyIntMax, encoderPreset,
				encoderTune, leakyQueue, captureSource, false);
	}

	public static ConnectionProfile fromPropertyValue(String value) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("Valor do perfil de conexão ausente.");
		}

		String[] parts = value.split(",", -1);

		if (parts.length != 11) {
			throw new IllegalArgumentException(
					"Perfil de conexão inválido. Esperado 11 campos, encontrado " + parts.length + ".");
		}

		String id = normalizeId(parts[0]);
		String displayName = normalizeText(parts[1], id);
		int width = parsePositiveInt(parts[2], "width");
		int height = parsePositiveInt(parts[3], "height");
		int fps = parsePositiveInt(parts[4], "fps");
		int bitrateKbps = parsePositiveInt(parts[5], "bitrateKbps");
		int keyIntMax = parsePositiveInt(parts[6], "keyIntMax");
		String encoderPreset = normalizeText(parts[7], DEFAULT_ENCODER_PRESET);
		String encoderTune = normalizeText(parts[8], DEFAULT_ENCODER_TUNE);
		boolean leakyQueue = Boolean.parseBoolean(parts[9].trim());
		String captureSource = normalizeCaptureSource(parts[10]);

		return new ConnectionProfile(id, displayName, width, height, fps, bitrateKbps, keyIntMax, encoderPreset,
				encoderTune, leakyQueue, captureSource, false);
	}

	public static boolean isReservedId(String id) {
		String normalized = normalizeId(id);
		return LAN_ID.equalsIgnoreCase(normalized) || WIFI_ID.equalsIgnoreCase(normalized);
	}

	public static String normalizeId(String value) {
		String normalized = normalizeText(value, LAN_ID);
		normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
		normalized = normalized.replaceAll("[^A-Za-z0-9]+", "_");
		normalized = normalized.replaceAll("_+", "_");
		normalized = normalized.replaceAll("^_", "").replaceAll("_$", "");
		normalized = normalized.toUpperCase(Locale.ROOT);
		return normalized.isEmpty() ? DEFAULT.getId() : normalized;
	}

	public static String normalizeCaptureSource(String value) {
		String normalized = normalizeText(value, DEFAULT_CAPTURE_SOURCE).toLowerCase(Locale.ROOT);
		return switch (normalized) {
			case "d3d11" -> "d3d11";
			case "dx9" -> "dx9";
			default -> DEFAULT_CAPTURE_SOURCE;
		};
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
		return id.toUpperCase(Locale.ROOT).hashCode();
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
