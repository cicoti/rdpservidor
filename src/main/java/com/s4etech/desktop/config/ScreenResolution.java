package com.s4etech.desktop.config;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Toolkit;

public final class ScreenResolution {

	private static final int FALLBACK_WIDTH = 1920;
	private static final int FALLBACK_HEIGHT = 1080;

	private ScreenResolution() {
	}

	public static Resolution current() {
		try {
			GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			if (device != null) {
				DisplayMode displayMode = device.getDisplayMode();
				if (displayMode != null && displayMode.getWidth() > 0 && displayMode.getHeight() > 0) {
					return new Resolution(displayMode.getWidth(), displayMode.getHeight());
				}
			}
		} catch (HeadlessException e) {
			// fallback below
		}

		try {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			if (screenSize != null && screenSize.width > 0 && screenSize.height > 0) {
				return new Resolution(screenSize.width, screenSize.height);
			}
		} catch (HeadlessException e) {
			// fallback below
		}

		return new Resolution(FALLBACK_WIDTH, FALLBACK_HEIGHT);
	}

	public record Resolution(int width, int height) {

		public String toDisplayText() {
			return width + " x " + height;
		}
	}
}
