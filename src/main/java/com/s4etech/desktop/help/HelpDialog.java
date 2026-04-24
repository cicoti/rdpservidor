package com.s4etech.desktop.help;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.s4etech.desktop.path.ApplicationPaths;

public final class HelpDialog {

	private static final Logger logger = LoggerFactory.getLogger(HelpDialog.class);
	private static final String HELP_FILE_PATH = "docs/perfis-conexao.html";
	private static final String HELP_RESOURCE_PATH = "docs/perfis-conexao.html";

	private HelpDialog() {
	}

	public static void showHelpDialog() {
		File helpFile = ensureHelpFileExists();

		try {
			if (helpFile == null || !helpFile.exists()) {
				JOptionPane.showMessageDialog(null, "Arquivo de ajuda não encontrado.", "Ajuda",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			if (!Desktop.isDesktopSupported()) {
				JOptionPane.showMessageDialog(null, "Abertura no navegador não é suportada neste ambiente.", "Ajuda",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			Desktop.getDesktop().browse(helpFile.toURI());
		} catch (Exception e) {
			logger.error("Erro ao abrir ajuda no navegador", e);
			JOptionPane.showMessageDialog(null, "Erro ao abrir ajuda no navegador:\n" + e.getMessage(), "Ajuda",
					JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private static File ensureHelpFileExists() {
		File helpFile = new File(ApplicationPaths.getApplicationBaseDirectory(), HELP_FILE_PATH);

		logger.info("Verificando arquivo de ajuda | destino={}", helpFile.getAbsolutePath());

		if (helpFile.exists()) {
			logger.info("Arquivo de ajuda já existe | caminho={}", helpFile.getAbsolutePath());
			return helpFile;
		}

		File parentDir = helpFile.getParentFile();
		if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
			logger.warn("Não foi possível criar diretório da ajuda | caminho={}", parentDir.getAbsolutePath());
			return helpFile;
		}

		String[] resourcePaths = {
				HELP_RESOURCE_PATH,
				"resources/" + HELP_RESOURCE_PATH
		};

		for (String resourcePath : resourcePaths) {
			try (InputStream inputStream = HelpDialog.class.getClassLoader().getResourceAsStream(resourcePath)) {
				if (inputStream == null) {
					logger.warn("Recurso de ajuda não encontrado no classpath | recurso={}", resourcePath);
					continue;
				}

				try (FileOutputStream outputStream = new FileOutputStream(helpFile)) {
					inputStream.transferTo(outputStream);
				}

				logger.info("Arquivo de ajuda copiado com sucesso | origem={} | destino={}",
						resourcePath, helpFile.getAbsolutePath());
				return helpFile;

			} catch (Exception e) {
				logger.error("Erro ao copiar arquivo de ajuda | recurso={} | destino={}",
						resourcePath, helpFile.getAbsolutePath(), e);
			}
		}

		logger.warn("Nenhum recurso de ajuda válido foi encontrado no classpath");
		return helpFile;
	}


}
