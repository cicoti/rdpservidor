package com.s4etech.desktop.server;

import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;

public class GStreamerProbe {

	public static void main(String[] args) {
		try {
			Gst.init("GStreamerProbe", new String[0]);

			testPipeline("d3d11-basic", "d3d11screencapturesrc monitor-index=0 show-cursor=false ! fakesink");
			testPipeline("d3d11-minimal", "d3d11screencapturesrc ! fakesink");

		} catch (Throwable t) {
			System.err
					.println("Falha geral no GStreamerProbe: " + t.getClass().getSimpleName() + ": " + t.getMessage());
			t.printStackTrace();
		} finally {
			try {
				Gst.deinit();
			} catch (Exception ignored) {
			}
		}
	}

	private static void testPipeline(String name, String pipelineStr) {
		System.out.println("==================================================");
		System.out.println("Teste: " + name);
		System.out.println("Pipeline: " + pipelineStr);

		Pipeline pipeline = null;

		try {
			pipeline = (Pipeline) Gst.parseLaunch(pipelineStr);
			System.out.println("Resultado: SUCESSO no parseLaunch");
		} catch (Throwable t) {
			System.err.println("Resultado: FALHA no parseLaunch | tipo=" + t.getClass().getSimpleName() + " | mensagem="
					+ t.getMessage());
			t.printStackTrace();
		} finally {
			if (pipeline != null) {
				try {
					pipeline.stop();
				} catch (Exception ignored) {
				}
				try {
					pipeline.dispose();
				} catch (Exception ignored) {
				}
			}
		}
	}
}
