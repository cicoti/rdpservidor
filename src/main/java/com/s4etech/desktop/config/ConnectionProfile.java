package com.s4etech.desktop.config;

import java.text.Normalizer;
import java.util.Locale;

public class ConnectionProfile {

	public static final String DEFAULT_ENCODER_PRESET = "ultrafast";
	public static final String DEFAULT_ENCODER_TUNE = "zerolatency";
	public static final boolean DEFAULT_LEAKY_QUEUE = false;
	public static final String DEFAULT_CAPTURE_SOURCE = "d3d11";

	public static final String LAN_ID = "LAN";
	public static final String LAN_DISPLAY_NAME = "Rede local";
	public static final int LAN_FPS = 30;
	public static final int LAN_BITRATE_KBPS = 6000;
	public static final int LAN_KEY_INT_MAX = 30;
	public static final String LAN_ENCODER_PRESET = "ultrafast";
	public static final String LAN_ENCODER_TUNE = "zerolatency";
	public static final boolean LAN_LEAKY_QUEUE = false;
	public static final String LAN_CAPTURE_SOURCE = DEFAULT_CAPTURE_SOURCE;

	public static final String WIFI_ID = "WIFI";
	public static final String WIFI_DISPLAY_NAME = "Wi-Fi";
	public static final int WIFI_FPS = 20;
	public static final int WIFI_BITRATE_KBPS = 2500;
	public static final int WIFI_KEY_INT_MAX = 24;
	public static final String WIFI_ENCODER_PRESET = "veryfast";
	public static final String WIFI_ENCODER_TUNE = "zerolatency";
	public static final boolean WIFI_LEAKY_QUEUE = true;
	public static final String WIFI_CAPTURE_SOURCE = DEFAULT_CAPTURE_SOURCE;

	public static final String STARLINK_ID = "STARLINK";
	public static final String STARLINK_DISPLAY_NAME = "Starlink";
	public static final int STARLINK_FPS = 10;
	public static final int STARLINK_BITRATE_KBPS = 900;
	public static final int STARLINK_KEY_INT_MAX = 20;
	public static final String STARLINK_ENCODER_PRESET = "veryfast";
	public static final String STARLINK_ENCODER_TUNE = "zerolatency";
	public static final boolean STARLINK_LEAKY_QUEUE = true;
	public static final String STARLINK_CAPTURE_SOURCE = DEFAULT_CAPTURE_SOURCE;

	public static final ConnectionProfile LAN = new ConnectionProfile(LAN_ID, LAN_DISPLAY_NAME, LAN_FPS,
			LAN_BITRATE_KBPS, LAN_KEY_INT_MAX, LAN_ENCODER_PRESET, LAN_ENCODER_TUNE, LAN_LEAKY_QUEUE,
			LAN_CAPTURE_SOURCE, true);

	public static final ConnectionProfile WIFI = new ConnectionProfile(WIFI_ID, WIFI_DISPLAY_NAME, WIFI_FPS,
			WIFI_BITRATE_KBPS, WIFI_KEY_INT_MAX, WIFI_ENCODER_PRESET, WIFI_ENCODER_TUNE, WIFI_LEAKY_QUEUE,
			WIFI_CAPTURE_SOURCE, true);

	public static final ConnectionProfile STARLINK = new ConnectionProfile(STARLINK_ID, STARLINK_DISPLAY_NAME,
			STARLINK_FPS, STARLINK_BITRATE_KBPS, STARLINK_KEY_INT_MAX, STARLINK_ENCODER_PRESET, STARLINK_ENCODER_TUNE,
			STARLINK_LEAKY_QUEUE, STARLINK_CAPTURE_SOURCE, true);

	public static final ConnectionProfile DEFAULT = LAN;

	private final String id;
	private final String displayName;
	private final int fps;
	private final int bitrateKbps;
	private final int keyIntMax;
	private final String encoderPreset;
	private final String encoderTune;
	private final boolean leakyQueue;
	private final String captureSource;
	private final boolean systemProfile;

	public ConnectionProfile(String id, String displayName, int fps, int bitrateKbps, int keyIntMax,
			String encoderPreset, String encoderTune, boolean leakyQueue, String captureSource) {
		this(id, displayName, fps, bitrateKbps, keyIntMax, encoderPreset, encoderTune, leakyQueue, captureSource, false);
	}

	public ConnectionProfile(String id, String displayName, int fps, int bitrateKbps, int keyIntMax,
			String encoderPreset, String encoderTune, boolean leakyQueue, String captureSource, boolean systemProfile) {
		this.id = normalizeId(id);
		this.displayName = normalizeText(displayName, this.id);
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
		return String.join(",", id, displayName, String.valueOf(fps), String.valueOf(bitrateKbps),
				String.valueOf(keyIntMax), encoderPreset, encoderTune, String.valueOf(leakyQueue), captureSource);
	}

	public ConnectionProfile copyAsCustom(String newId, String newDisplayName) {
		return new ConnectionProfile(newId, newDisplayName, fps, bitrateKbps, keyIntMax, encoderPreset, encoderTune,
				leakyQueue, captureSource, false);
	}

	public static ConnectionProfile fromPropertyValue(String value) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("Valor do perfil de conexao ausente.");
		}

		String[] parts = value.split(",", -1);
		boolean legacyFormat = parts.length == 10 || parts.length == 11;
		boolean currentFormat = parts.length == 8 || parts.length == 9;

		if (!legacyFormat && !currentFormat) {
			throw new IllegalArgumentException(
					"Perfil de conexao invalido. Esperado 8 ou 9 campos, encontrado " + parts.length + ".");
		}

		String id = normalizeId(parts[0]);
		String displayName = normalizeText(parts[1], id);

		// Old profile lines had width/height. They are accepted for compatibility and ignored.
		int fpsIndex = legacyFormat ? 4 : 2;
		int bitrateIndex = legacyFormat ? 5 : 3;
		int keyIntIndex = legacyFormat ? 6 : 4;
		int presetIndex = legacyFormat ? 7 : 5;
		int tuneIndex = legacyFormat ? 8 : 6;
		int leakyQueueIndex = legacyFormat ? 9 : 7;
		int captureSourceIndex = legacyFormat ? 10 : 8;

		int fps = parsePositiveInt(parts[fpsIndex], "fps");
		int bitrateKbps = parsePositiveInt(parts[bitrateIndex], "bitrateKbps");
		int keyIntMax = parsePositiveInt(parts[keyIntIndex], "keyIntMax");
		String encoderPreset = normalizeText(parts[presetIndex], DEFAULT_ENCODER_PRESET);
		String encoderTune = normalizeText(parts[tuneIndex], DEFAULT_ENCODER_TUNE);
		boolean leakyQueue = Boolean.parseBoolean(parts[leakyQueueIndex].trim());
		String captureSource = captureSourceIndex < parts.length ? normalizeCaptureSource(parts[captureSourceIndex])
				: DEFAULT_CAPTURE_SOURCE;

		return new ConnectionProfile(id, displayName, fps, bitrateKbps, keyIntMax, encoderPreset, encoderTune,
				leakyQueue, captureSource, false);
	}

	public static boolean isReservedId(String id) {
		String normalized = normalizeId(id);
		return LAN_ID.equalsIgnoreCase(normalized) || WIFI_ID.equalsIgnoreCase(normalized)
				|| STARLINK_ID.equalsIgnoreCase(normalized);
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

		throw new IllegalArgumentException("Valor invalido para " + fieldName + ": " + value);
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
