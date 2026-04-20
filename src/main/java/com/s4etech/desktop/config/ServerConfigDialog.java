package com.s4etech.desktop.config;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.s4etech.desktop.validador.PortValidator;

public class ServerConfigDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final JTextField handshakeField;
	private final JTextField controlField;
	private final JComboBox<ConnectionProfile> profileComboBox;

	private final JLabel profileIdValueLabel;
	private final JLabel profileDisplayNameValueLabel;
	private final JLabel profileResolutionValueLabel;
	private final JLabel profileFpsValueLabel;
	private final JLabel profileBitrateValueLabel;
	private final JLabel profileKeyIntValueLabel;
	private final JLabel profilePresetValueLabel;
	private final JLabel profileTuneValueLabel;
	private final JLabel profileLeakyQueueValueLabel;

	private final ServerConfigManager configManager;
	private final ServerConfig currentConfig;
	private final List<ConnectionProfile> availableProfiles;

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
		this.availableProfiles = buildAvailableProfiles(currentConfig);

		this.handshakeField = new JTextField(String.valueOf(currentConfig.getHandshakePort()), 14);
		this.controlField = new JTextField(String.valueOf(currentConfig.getControlPort()), 14);
		this.profileComboBox = new JComboBox<>(this.availableProfiles.toArray(new ConnectionProfile[0]));

		this.profileIdValueLabel = createProfileValueLabel();
		this.profileDisplayNameValueLabel = createProfileValueLabel();
		this.profileResolutionValueLabel = createProfileValueLabel();
		this.profileFpsValueLabel = createProfileValueLabel();
		this.profileBitrateValueLabel = createProfileValueLabel();
		this.profileKeyIntValueLabel = createProfileValueLabel();
		this.profilePresetValueLabel = createProfileValueLabel();
		this.profileTuneValueLabel = createProfileValueLabel();
		this.profileLeakyQueueValueLabel = createProfileValueLabel();

		buildUi();
		selectCurrentProfile();
		updateProfileDetails(getSelectedProfile());
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

		setPreferredSize(new Dimension(620, 700));
		pack();
		setMinimumSize(new Dimension(620, 700));
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
		JPanel panel = new JPanel(new BorderLayout(12, 0));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)),
				new EmptyBorder(16, 16, 16, 16)));

		JPanel leftPanel = new JPanel();
		leftPanel.setOpaque(false);
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

		JLabel profileLabel = new JLabel("Perfil de conexão");
		profileLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
		profileLabel.setAlignmentX(LEFT_ALIGNMENT);

		profileComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		profileComboBox.setAlignmentX(LEFT_ALIGNMENT);
		profileComboBox.addActionListener(e -> updateProfileDetails(getSelectedProfile()));

		leftPanel.add(profileLabel);
		leftPanel.add(Box.createRigidArea(new Dimension(0, 8)));
		leftPanel.add(profileComboBox);

		panel.add(leftPanel, BorderLayout.WEST);
		panel.add(buildProfileDetailsPanel(), BorderLayout.CENTER);

		return panel;
	}

	private JPanel buildProfileDetailsPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setOpaque(false);
		panel.setBorder(new EmptyBorder(0, 12, 0, 0));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(0, 0, 6, 12);

		addProfileDetailRow(panel, gbc, "ID", profileIdValueLabel);
		addProfileDetailRow(panel, gbc, "Nome", profileDisplayNameValueLabel);
		addProfileDetailRow(panel, gbc, "Resolução", profileResolutionValueLabel);
		addProfileDetailRow(panel, gbc, "FPS", profileFpsValueLabel);
		addProfileDetailRow(panel, gbc, "Bitrate", profileBitrateValueLabel);
		addProfileDetailRow(panel, gbc, "Key-int-max", profileKeyIntValueLabel);
		addProfileDetailRow(panel, gbc, "Preset", profilePresetValueLabel);
		addProfileDetailRow(panel, gbc, "Tune", profileTuneValueLabel);
		addProfileDetailRow(panel, gbc, "Leaky queue", profileLeakyQueueValueLabel);

		gbc.gridx = 0;
		gbc.weighty = 1;
		gbc.gridwidth = 2;
		panel.add(Box.createVerticalGlue(), gbc);

		return panel;
	}

	private void addProfileDetailRow(JPanel panel, GridBagConstraints gbc, String labelText, JLabel valueLabel) {
		GridBagConstraints labelConstraints = (GridBagConstraints) gbc.clone();
		labelConstraints.gridx = 0;
		labelConstraints.weightx = 0;
		labelConstraints.fill = GridBagConstraints.NONE;

		JLabel label = new JLabel(labelText + ":");
		label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
		panel.add(label, labelConstraints);

		GridBagConstraints valueConstraints = (GridBagConstraints) gbc.clone();
		valueConstraints.gridx = 1;
		valueConstraints.weightx = 1;
		valueConstraints.fill = GridBagConstraints.HORIZONTAL;
		valueConstraints.insets = new Insets(0, 0, 6, 0);
		panel.add(valueLabel, valueConstraints);

		gbc.gridy++;
	}

	private JPanel buildInfoPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		panel.setAlignmentX(LEFT_ALIGNMENT);

		JLabel infoLabel = new JLabel(
				"<html><span style='color:rgb(110,110,110);'>"
						+ "As alterações salvas só serão aplicadas após reiniciar a aplicação."
						+ "</span></html>",
				SwingConstants.LEFT);
		infoLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

		panel.add(infoLabel, BorderLayout.WEST);
		return panel;
	}

	private JPanel buildButtonPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
		panel.setPreferredSize(new Dimension(580, 44));
		panel.setMinimumSize(new Dimension(580, 44));

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
		profileComboBox.setSelectedItem(profile);
	}

	private ConnectionProfile getSelectedProfile() {
		ConnectionProfile selectedProfile = (ConnectionProfile) profileComboBox.getSelectedItem();
		return selectedProfile != null ? selectedProfile : ServerConfig.DEFAULT_CONNECTION_PROFILE;
	}

	private void restoreDefaults() {
		handshakeField.setText(String.valueOf(ServerConfig.DEFAULT_HANDSHAKE_PORT));
		controlField.setText(String.valueOf(ServerConfig.DEFAULT_CONTROL_PORT));
		selectProfileById(ConnectionProfile.DEFAULT_ID);
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
				|| !selectedProfile.equals(currentConfig.getConnectionProfile());

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
			newConfig.setAvailableProfiles(availableProfiles);
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

	private void updateProfileDetails(ConnectionProfile profile) {
		ConnectionProfile selectedProfile = profile != null ? profile : ServerConfig.DEFAULT_CONNECTION_PROFILE;
		profileIdValueLabel.setText(selectedProfile.getId());
		profileDisplayNameValueLabel.setText(selectedProfile.getDisplayName());
		profileResolutionValueLabel
				.setText(selectedProfile.getWidth() + " x " + selectedProfile.getHeight());
		profileFpsValueLabel.setText(selectedProfile.getFps() + " fps");
		profileBitrateValueLabel.setText(selectedProfile.getBitrateKbps() + " kbps");
		profileKeyIntValueLabel.setText(String.valueOf(selectedProfile.getKeyIntMax()));
		profilePresetValueLabel.setText(selectedProfile.getEncoderPreset());
		profileTuneValueLabel.setText(selectedProfile.getEncoderTune());
		profileLeakyQueueValueLabel.setText(selectedProfile.isLeakyQueue() ? "Sim" : "Não");
	}

	private JLabel createProfileValueLabel() {
		JLabel label = new JLabel("-");
		label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		label.setForeground(descriptionColor);
		return label;
	}

	private List<ConnectionProfile> buildAvailableProfiles(ServerConfig config) {
		List<ConnectionProfile> profiles = new ArrayList<>(config.getAvailableProfiles());
		if (profiles.isEmpty()) {
			profiles.add(ServerConfig.DEFAULT_CONNECTION_PROFILE);
		}
		profiles.sort(Comparator.comparing(ConnectionProfile::getDisplayName, String.CASE_INSENSITIVE_ORDER));
		return profiles;
	}

	private void selectProfileById(String profileId) {
		for (int i = 0; i < profileComboBox.getItemCount(); i++) {
			ConnectionProfile profile = profileComboBox.getItemAt(i);
			if (profile != null && profile.getId().equalsIgnoreCase(profileId)) {
				profileComboBox.setSelectedIndex(i);
				return;
			}
		}
		profileComboBox.setSelectedItem(ServerConfig.DEFAULT_CONNECTION_PROFILE);
	}

	private void showWarning(String message) {
		JOptionPane.showMessageDialog(this, message, "Validação", JOptionPane.WARNING_MESSAGE);
	}
}
