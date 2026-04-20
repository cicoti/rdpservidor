package com.s4etech.desktop.config;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.s4etech.desktop.validador.PortValidator;

public class ServerConfigDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final JTextField handshakeField;
	private final JTextField controlField;

	private final JRadioButton satellite1Radio;
	private final JRadioButton satellite2Radio;
	private final JRadioButton satellite3Radio;
	private final JRadioButton wifiRadio;
	private final JRadioButton lanRadio;

	private final ServerConfigManager configManager;
	private final ServerConfig currentConfig;

	private final int activeHandshakePort;
	private final int activeControlPort;

	private final Color descriptionColor = new Color(110, 110, 110);

	public ServerConfigDialog(JFrame parent, ServerConfig currentConfig, ServerConfigManager configManager,
			int activeHandshakePort, int activeControlPort) {
		super(parent, "Configuração de Portas | S4eTech - RemoteDesktopServer - v1.0", true);

		this.currentConfig = currentConfig;
		this.configManager = configManager;
		this.activeHandshakePort = activeHandshakePort;
		this.activeControlPort = activeControlPort;

		this.handshakeField = new JTextField(String.valueOf(currentConfig.getHandshakePort()), 14);
		this.controlField = new JTextField(String.valueOf(currentConfig.getControlPort()), 14);

		this.satellite1Radio = new JRadioButton("Satélite 1");
		this.satellite2Radio = new JRadioButton("Satélite 2");
		this.satellite3Radio = new JRadioButton("Satélite 3");
		this.wifiRadio = new JRadioButton("Wi-Fi");
		this.lanRadio = new JRadioButton("Rede local - default");

		buildUi();
		selectCurrentProfile();
	}

	private void buildUi() {
		setLayout(new BorderLayout());
		getRootPane().setBorder(new EmptyBorder(16, 16, 16, 16));

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBorder(new EmptyBorder(4, 4, 4, 4));

		contentPanel.add(buildHeaderPanel());
		contentPanel.add(Box.createRigidArea(new Dimension(0, 14)));
		contentPanel.add(buildFormPanel());
		contentPanel.add(Box.createRigidArea(new Dimension(0, 12)));
		contentPanel.add(buildProfilePanel());
		contentPanel.add(Box.createRigidArea(new Dimension(0, 12)));
		contentPanel.add(buildInfoPanel());
		contentPanel.add(Box.createRigidArea(new Dimension(0, 16)));
		contentPanel.add(buildButtonPanel());

		add(contentPanel, BorderLayout.CENTER);

		setPreferredSize(new Dimension(560, 620));
		pack();
		setMinimumSize(new Dimension(560, 620));
		setResizable(true);
		setLocationRelativeTo(getParent());
	}

	private JPanel buildHeaderPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		panel.setAlignmentX(LEFT_ALIGNMENT);

		JPanel textPanel = new JPanel();
		textPanel.setOpaque(false);
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

		JLabel titleLabel = new JLabel("Configuração do Servidor");
		titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
		titleLabel.setAlignmentX(LEFT_ALIGNMENT);

		JLabel subtitleLabel = new JLabel("Defina as portas locais e o perfil de conexão utilizado pelo servidor.");
		subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
		subtitleLabel.setForeground(descriptionColor);
		subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);

		textPanel.add(titleLabel);
		textPanel.add(Box.createRigidArea(new Dimension(0, 4)));
		textPanel.add(subtitleLabel);

		panel.add(textPanel, BorderLayout.WEST);
		return panel;
	}

	private JPanel buildFormPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)),
				new EmptyBorder(16, 16, 16, 16)));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(0, 0, 4, 12);

		JLabel handshakeLabel = new JLabel("Porta Handshake");
		handshakeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
		panel.add(handshakeLabel, gbc);

		gbc.gridx = 1;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 4, 0);
		panel.add(handshakeField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 12, 0);

		JLabel handshakeDesc = new JLabel("Usada para a negociação inicial entre cliente e servidor. Padrão: 7000");
		handshakeDesc.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		handshakeDesc.setForeground(descriptionColor);
		panel.add(handshakeDesc, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 4, 12);

		JLabel controlLabel = new JLabel("Porta Controle Mouse/Teclado");
		controlLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
		panel.add(controlLabel, gbc);

		gbc.gridx = 1;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 4, 0);
		panel.add(controlField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);

		JLabel controlDesc = new JLabel("Usada para eventos de mouse e teclado enviados pelo cliente. Padrão: 5000");
		controlDesc.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		controlDesc.setForeground(descriptionColor);
		panel.add(controlDesc, gbc);

		return panel;
	}

	private JPanel buildProfilePanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)),
				new EmptyBorder(16, 16, 16, 16)));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(0, 0, 8, 0);

		JLabel profileLabel = new JLabel("Perfil de conexão");
		profileLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
		panel.add(profileLabel, gbc);

		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(satellite1Radio);
		buttonGroup.add(satellite2Radio);
		buttonGroup.add(satellite3Radio);
		buttonGroup.add(wifiRadio);
		buttonGroup.add(lanRadio);

		gbc.gridy++;
		panel.add(satellite1Radio, gbc);

		gbc.gridy++;
		panel.add(satellite2Radio, gbc);

		gbc.gridy++;
		panel.add(satellite3Radio, gbc);

		gbc.gridy++;
		panel.add(wifiRadio, gbc);

		gbc.gridy++;
		panel.add(lanRadio, gbc);

		gbc.gridy++;
		gbc.insets = new Insets(10, 0, 0, 0);

		JLabel profileDesc = new JLabel(
				"Selecione o perfil de rede que melhor representa o ambiente de conexão do servidor.");
		profileDesc.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		profileDesc.setForeground(descriptionColor);
		panel.add(profileDesc, gbc);

		return panel;
	}

	private JPanel buildInfoPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		panel.setAlignmentX(LEFT_ALIGNMENT);

		JLabel infoLabel = new JLabel(
		        "<html><span style='color:rgb(110,110,110);'>"
		                + "As alterações salvas só serão aplicadas após reiniciar a aplicação."
		                + "</span></html>",
		        SwingConstants.LEFT
		);
		infoLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

		panel.add(infoLabel, BorderLayout.WEST);
		return panel;
	}

	private JPanel buildButtonPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
		panel.setPreferredSize(new Dimension(520, 44));
		panel.setMinimumSize(new Dimension(520, 44));

		JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		leftPanel.setOpaque(false);

		JButton restoreButton = new JButton("Restaurar padrões");
		restoreButton.setPreferredSize(new Dimension(160, 34));
		restoreButton.addActionListener(e -> restoreDefaults());

		leftPanel.add(restoreButton);

		JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		rightPanel.setOpaque(false);

		JButton cancelButton = new JButton("Cancelar");
		cancelButton.setPreferredSize(new Dimension(110, 34));
		cancelButton.addActionListener(e -> dispose());

		JButton saveButton = new JButton("Salvar");
		saveButton.setPreferredSize(new Dimension(110, 34));
		saveButton.addActionListener(e -> onSave());

		rightPanel.add(cancelButton);
		rightPanel.add(saveButton);

		panel.add(leftPanel, BorderLayout.WEST);
		panel.add(rightPanel, BorderLayout.EAST);

		return panel;
	}

	private void selectCurrentProfile() {
		ConnectionProfile profile = currentConfig.getConnectionProfile();

		switch (profile) {
		case SATELLITE_1:
			satellite1Radio.setSelected(true);
			break;
		case SATELLITE_2:
			satellite2Radio.setSelected(true);
			break;
		case SATELLITE_3:
			satellite3Radio.setSelected(true);
			break;
		case WIFI:
			wifiRadio.setSelected(true);
			break;
		case LAN:
		default:
			lanRadio.setSelected(true);
			break;
		}
	}

	private ConnectionProfile getSelectedProfile() {
		if (satellite1Radio.isSelected()) {
			return ConnectionProfile.SATELLITE_1;
		}
		if (satellite2Radio.isSelected()) {
			return ConnectionProfile.SATELLITE_2;
		}
		if (satellite3Radio.isSelected()) {
			return ConnectionProfile.SATELLITE_3;
		}
		if (wifiRadio.isSelected()) {
			return ConnectionProfile.WIFI;
		}
		return ConnectionProfile.LAN;
	}

	private void restoreDefaults() {
		handshakeField.setText(String.valueOf(ServerConfig.DEFAULT_HANDSHAKE_PORT));
		controlField.setText(String.valueOf(ServerConfig.DEFAULT_CONTROL_PORT));
		lanRadio.setSelected(true);
		handshakeField.requestFocus();
		handshakeField.selectAll();
	}

	private void onSave() {
		int handshakePort;
		int controlPort;

		try {
			handshakePort = Integer.parseInt(handshakeField.getText().trim());
			controlPort = Integer.parseInt(controlField.getText().trim());
		} catch (NumberFormatException e) {
			showWarning("As portas devem ser numéricas.");
			return;
		}

		if (!PortValidator.isValidPort(handshakePort) || !PortValidator.isValidPort(controlPort)) {
			showWarning("As portas devem estar entre 1 e 65535.");
			return;
		}

		if (handshakePort == controlPort) {
			showWarning("A porta de handshake e a porta de controle não podem ser iguais.");
			return;
		}

		ConnectionProfile selectedProfile = getSelectedProfile();

		boolean changed = handshakePort != currentConfig.getHandshakePort()
				|| controlPort != currentConfig.getControlPort()
				|| selectedProfile != currentConfig.getConnectionProfile();

		boolean handshakeChangedAgainstActive = handshakePort != activeHandshakePort;
		boolean controlChangedAgainstActive = controlPort != activeControlPort;

		if (handshakeChangedAgainstActive && !PortValidator.isUdpPortAvailable(handshakePort)) {
			showWarning("A porta de handshake informada já está em uso.");
			return;
		}

		if (controlChangedAgainstActive && !PortValidator.isUdpPortAvailable(controlPort)) {
			showWarning("A porta de controle informada já está em uso.");
			return;
		}

		try {
			ServerConfig newConfig = new ServerConfig(handshakePort, controlPort, selectedProfile);
			configManager.save(newConfig);

			if (changed) {
				JOptionPane.showMessageDialog(this,
						"Configuração salva com sucesso.\nReinicie a aplicação para aplicar as novas configurações.",
						"Configuração salva", JOptionPane.INFORMATION_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(this, "Configuração salva com sucesso.", "Configuração salva",
						JOptionPane.INFORMATION_MESSAGE);
			}

			dispose();

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Erro ao salvar a configuração: " + e.getMessage(), "Erro",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void showWarning(String message) {
		JOptionPane.showMessageDialog(this, message, "Validação", JOptionPane.WARNING_MESSAGE);
	}
}
