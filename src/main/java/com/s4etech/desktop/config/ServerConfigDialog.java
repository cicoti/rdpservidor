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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

import com.s4etech.desktop.server.RemoteDesktopServer;
import com.s4etech.desktop.validador.PortValidator;

public class ServerConfigDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final String[] RESOLUTION_OPTIONS = {
            "640 x 360",
            "854 x 480",
            "960 x 540",
            "1280 x 720",
            "1366 x 768",
            "1600 x 900",
            "1920 x 1080"
    };

    private static final Integer[] FPS_OPTIONS = { 10, 12, 15, 20, 24 };
    private static final Integer[] BITRATE_OPTIONS = { 600, 800, 1000, 1200, 1400, 1800, 2500, 3000 };
    private static final Integer[] KEY_INT_OPTIONS = { 10, 15, 20, 24, 30 };
    private static final String[] PRESET_OPTIONS = { "ultrafast", "superfast", "veryfast", "faster" };
    private static final String[] TUNE_OPTIONS = { "zerolatency", "fastdecode" };
    private static final Boolean[] LEAKY_QUEUE_OPTIONS = { Boolean.TRUE, Boolean.FALSE };

    private final JTextField handshakeField;
    private final JTextField controlField;
    private final JComboBox<ConnectionProfile> profileComboBox;
    private final JTextField profileNameField;
    private final JComboBox<String> resolutionComboBox;
    private final JComboBox<Integer> fpsComboBox;
    private final JComboBox<Integer> bitrateComboBox;
    private final JComboBox<Integer> keyIntComboBox;
    private final JComboBox<String> presetComboBox;
    private final JComboBox<String> tuneComboBox;
    private final JComboBox<Boolean> leakyQueueComboBox;
    private final JButton duplicateButton;
    private final JButton deleteButton;
    private final JLabel profileStatusLabel;

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
        this.profileNameField = new JTextField(24);
        this.resolutionComboBox = new JComboBox<>(RESOLUTION_OPTIONS);
        this.fpsComboBox = new JComboBox<>(FPS_OPTIONS);
        this.bitrateComboBox = new JComboBox<>(BITRATE_OPTIONS);
        this.keyIntComboBox = new JComboBox<>(KEY_INT_OPTIONS);
        this.presetComboBox = new JComboBox<>(PRESET_OPTIONS);
        this.tuneComboBox = new JComboBox<>(TUNE_OPTIONS);
        this.leakyQueueComboBox = new JComboBox<>(LEAKY_QUEUE_OPTIONS);
        this.duplicateButton = new JButton("Duplicar");
        this.deleteButton = new JButton("Excluir");
        this.profileStatusLabel = createProfileStatusLabel();

        buildUi();
        selectCurrentProfile();
        loadSelectedProfileIntoForm();
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

        setPreferredSize(new Dimension(720, 760));
        pack();
        setMinimumSize(new Dimension(720, 760));
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

        JLabel subtitleLabel = new JLabel("Defina as portas locais e gerencie os perfis de conexão do servidor.");
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

        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JLabel profileLabel = new JLabel("Perfil de conexão");
        profileLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        profileLabel.setAlignmentX(LEFT_ALIGNMENT);

        JPanel selectionRowPanel = new JPanel(new BorderLayout(12, 0));
        selectionRowPanel.setOpaque(false);
        selectionRowPanel.setAlignmentX(LEFT_ALIGNMENT);

        profileComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        profileComboBox.setAlignmentX(LEFT_ALIGNMENT);
        profileComboBox.addActionListener(e -> loadSelectedProfileIntoForm());

        JPanel comboPanel = new JPanel(new BorderLayout());
        comboPanel.setOpaque(false);
        comboPanel.add(profileComboBox, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);

        duplicateButton.setPreferredSize(new Dimension(110, 32));
        duplicateButton.addActionListener(e -> duplicateSelectedProfile());

        deleteButton.setPreferredSize(new Dimension(110, 32));
        deleteButton.addActionListener(e -> deleteSelectedProfile());

        actionPanel.add(duplicateButton);
        actionPanel.add(deleteButton);

        selectionRowPanel.add(comboPanel, BorderLayout.CENTER);
        selectionRowPanel.add(actionPanel, BorderLayout.EAST);

        topPanel.add(profileLabel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        topPanel.add(selectionRowPanel);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(Box.createRigidArea(new Dimension(0, 12)), BorderLayout.CENTER);
        panel.add(buildProfileEditorPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildProfileEditorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 8, 12);

        addEditorRow(panel, gbc, "Nome do perfil", profileNameField);
        addEditorRow(panel, gbc, "Resolução", resolutionComboBox,
                "Define o tamanho do vídeo transmitido. Resoluções maiores aumentam qualidade e consumo de banda.");
        addEditorRow(panel, gbc, "FPS", fpsComboBox,
                "Quantidade de quadros por segundo. Valores maiores deixam o vídeo mais fluido, mas aumentam uso de CPU e rede.");
        addEditorRow(panel, gbc, "Bitrate Kbps", bitrateComboBox,
                "Taxa de bits do vídeo. Valores maiores melhoram qualidade visual, mas exigem mais banda disponível.");
        addEditorRow(panel, gbc, "Key Int Max", keyIntComboBox,
                "Intervalo máximo entre quadros-chave. Valores menores facilitam recuperação da imagem, mas aumentam o bitrate.");
        addEditorRow(panel, gbc, "Encoder Preset", presetComboBox,
                "Define o equilíbrio entre velocidade de codificação e eficiência de compressão.");
        addEditorRow(panel, gbc, "Encoder Tune", tuneComboBox,
                "Ajusta o encoder para cenários específicos. Ex.: zerolatency reduz atraso na transmissão.");
        addEditorRow(panel, gbc, "Leaky Queue", leakyQueueComboBox,
                "Quando ativado, descarta quadros antigos em congestionamento para reduzir latência.");

        GridBagConstraints statusConstraints = new GridBagConstraints();
        statusConstraints.gridx = 0;
        statusConstraints.gridy = gbc.gridy;
        statusConstraints.gridwidth = 2;
        statusConstraints.anchor = GridBagConstraints.WEST;
        statusConstraints.insets = new Insets(8, 0, 0, 0);
        statusConstraints.weightx = 1;
        statusConstraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(profileStatusLabel, statusConstraints);

        return panel;
    }

    private void addEditorRow(JPanel panel, GridBagConstraints gbc, String labelText, java.awt.Component component) {
        addEditorRow(panel, gbc, labelText, component, null);
    }

    private void addEditorRow(JPanel panel, GridBagConstraints gbc, String labelText, java.awt.Component component,
            String helpText) {
        GridBagConstraints labelConstraints = (GridBagConstraints) gbc.clone();
        labelConstraints.gridx = 0;
        labelConstraints.weightx = 0;
        labelConstraints.fill = GridBagConstraints.NONE;

        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        labelPanel.setOpaque(false);

        JLabel label = new JLabel(labelText + ":");
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        labelPanel.add(label);

        if (helpText != null && !helpText.isBlank()) {
            labelPanel.add(createHelpLabel(helpText));
        }

        panel.add(labelPanel, labelConstraints);

        GridBagConstraints valueConstraints = (GridBagConstraints) gbc.clone();
        valueConstraints.gridx = 1;
        valueConstraints.weightx = 1;
        valueConstraints.fill = GridBagConstraints.HORIZONTAL;
        valueConstraints.insets = new Insets(0, 0, 8, 0);
        panel.add(component, valueConstraints);

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
        panel.setPreferredSize(new Dimension(680, 44));
        panel.setMinimumSize(new Dimension(680, 44));

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

    private void loadSelectedProfileIntoForm() {
        ConnectionProfile profile = getSelectedProfile();
        profileNameField.setText(profile.getDisplayName());
        resolutionComboBox.setSelectedItem(profile.getWidth() + " x " + profile.getHeight());
        fpsComboBox.setSelectedItem(profile.getFps());
        bitrateComboBox.setSelectedItem(profile.getBitrateKbps());
        keyIntComboBox.setSelectedItem(profile.getKeyIntMax());
        presetComboBox.setSelectedItem(profile.getEncoderPreset());
        tuneComboBox.setSelectedItem(profile.getEncoderTune());
        leakyQueueComboBox.setSelectedItem(profile.isLeakyQueue());

        boolean systemProfile = profile.isSystemProfile();
        profileNameField.setEditable(!systemProfile);
        resolutionComboBox.setEnabled(!systemProfile);
        fpsComboBox.setEnabled(!systemProfile);
        bitrateComboBox.setEnabled(!systemProfile);
        keyIntComboBox.setEnabled(!systemProfile);
        presetComboBox.setEnabled(!systemProfile);
        tuneComboBox.setEnabled(!systemProfile);
        leakyQueueComboBox.setEnabled(!systemProfile);
        deleteButton.setEnabled(!systemProfile);
        duplicateButton.setEnabled(true);
        profileStatusLabel.setText(systemProfile ? "Perfil protegido do sistema" : "Perfil customizado editável");
    }

    private void restoreDefaults() {
        handshakeField.setText(String.valueOf(ServerConfig.DEFAULT_HANDSHAKE_PORT));
        controlField.setText(String.valueOf(ServerConfig.DEFAULT_CONTROL_PORT));
        profileComboBox.setSelectedItem(ServerConfig.DEFAULT_CONNECTION_PROFILE);
        loadSelectedProfileIntoForm();
        handshakeField.requestFocus();
        handshakeField.selectAll();
    }

    private void duplicateSelectedProfile() {
        ConnectionProfile selectedProfile = getSelectedProfile();
        String baseName = selectedProfile.getDisplayName();
        String newDisplayName = generateNextDisplayName(baseName);
        String newId = generateUniqueProfileId(newDisplayName, null);
        ConnectionProfile duplicated = selectedProfile.copyAsCustom(newId, newDisplayName);
        availableProfiles.add(duplicated);
        sortProfiles();
        refreshProfileComboBox(duplicated);
        loadSelectedProfileIntoForm();
    }

    private void deleteSelectedProfile() {
        ConnectionProfile selectedProfile = getSelectedProfile();
        if (selectedProfile.isSystemProfile()) {
            showWarning("Perfis protegidos do sistema não podem ser excluídos.");
            return;
        }

        int option = JOptionPane.showConfirmDialog(this,
                "Deseja realmente excluir o perfil '" + selectedProfile.getDisplayName() + "'?",
                "Excluir perfil", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (option != JOptionPane.YES_OPTION) {
            return;
        }

        availableProfiles.remove(selectedProfile);
        ConnectionProfile fallback = currentConfig.getConnectionProfile();
        if (fallback == null || !availableProfiles.contains(fallback)) {
            fallback = ServerConfig.DEFAULT_CONNECTION_PROFILE;
        }
        refreshProfileComboBox(fallback);
        loadSelectedProfileIntoForm();
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
        ConnectionProfile profileToActivate = selectedProfile;

        if (!selectedProfile.isSystemProfile()) {
            String displayName = profileNameField.getText() != null ? profileNameField.getText().trim() : "";
            if (displayName.isEmpty()) {
                showWarning("Informe o nome do perfil.");
                return;
            }

            Resolution resolution = parseResolution((String) resolutionComboBox.getSelectedItem());
            if (resolution == null) {
                showWarning("Selecione uma resolução válida.");
                return;
            }

            Integer fps = (Integer) fpsComboBox.getSelectedItem();
            Integer bitrate = (Integer) bitrateComboBox.getSelectedItem();
            Integer keyInt = (Integer) keyIntComboBox.getSelectedItem();
            String preset = (String) presetComboBox.getSelectedItem();
            String tune = (String) tuneComboBox.getSelectedItem();
            Boolean leakyQueue = (Boolean) leakyQueueComboBox.getSelectedItem();

            if (fps == null || bitrate == null || keyInt == null || preset == null || tune == null || leakyQueue == null) {
                showWarning("Preencha todos os parâmetros do perfil.");
                return;
            }

            String generatedId = generateUniqueProfileId(displayName, selectedProfile);
            profileToActivate = new ConnectionProfile(generatedId, displayName, resolution.width(), resolution.height(),
                    fps, bitrate, keyInt, preset, tune, leakyQueue, false);

            availableProfiles.remove(selectedProfile);
            availableProfiles.add(profileToActivate);
            sortProfiles();
        }

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

        if (handshakePort == activeControlPort && controlPort == activeHandshakePort) {
            showWarning("As portas informadas entram em conflito com as portas atualmente ativas.");
            return;
        }

        try {
            ServerConfig newConfig = new ServerConfig(handshakePort, controlPort, profileToActivate);
            newConfig.setAvailableProfiles(new ArrayList<>(availableProfiles));
            configManager.save(newConfig);

            int option = JOptionPane.showConfirmDialog(
                    this,
                    "Configuração salva com sucesso.\nDeseja encerrar a aplicação agora para aplicar as novas configurações?",
                    "Configuração salva",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            dispose();

            if (option == JOptionPane.YES_OPTION) {
                RemoteDesktopServer.requestShutdown();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar a configuração: " + e.getMessage(), "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshProfileComboBox(ConnectionProfile selectedProfile) {
        profileComboBox.removeAllItems();
        for (ConnectionProfile profile : availableProfiles) {
            profileComboBox.addItem(profile);
        }
        profileComboBox.setSelectedItem(selectedProfile);
    }

    private void sortProfiles() {
        availableProfiles.sort((a, b) -> {
            if (a.isSystemProfile() && b.isSystemProfile()) {
                if (ConnectionProfile.LAN_ID.equalsIgnoreCase(a.getId())) {
                    return -1;
                }
                if (ConnectionProfile.LAN_ID.equalsIgnoreCase(b.getId())) {
                    return 1;
                }
                return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
            }
            if (a.isSystemProfile()) {
                return -1;
            }
            if (b.isSystemProfile()) {
                return 1;
            }
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        });
    }

    private String generateNextDisplayName(String baseName) {
        String normalizedBase = (baseName == null || baseName.trim().isEmpty()) ? "Novo Perfil" : baseName.trim();
        Set<String> existingNames = new LinkedHashSet<>();
        for (ConnectionProfile profile : availableProfiles) {
            existingNames.add(profile.getDisplayName().trim().toLowerCase());
        }

        if (!existingNames.contains(normalizedBase.toLowerCase())) {
            return normalizedBase;
        }

        int suffix = 1;
        while (existingNames.contains((normalizedBase + " " + suffix).toLowerCase())) {
            suffix++;
        }
        return normalizedBase + " " + suffix;
    }

    private String generateUniqueProfileId(String displayName, ConnectionProfile currentProfile) {
        String baseId = ConnectionProfile.normalizeId(displayName);
        if (ConnectionProfile.isReservedId(baseId)) {
            baseId = baseId + "_1";
        }

        String candidate = baseId;
        int suffix = 1;
        while (profileIdExists(candidate, currentProfile)) {
            candidate = baseId + "_" + suffix;
            suffix++;
        }
        return candidate;
    }

    private boolean profileIdExists(String id, ConnectionProfile currentProfile) {
        for (ConnectionProfile profile : availableProfiles) {
            if (currentProfile != null && profile.equals(currentProfile)) {
                continue;
            }
            if (profile.getId().equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    private JLabel createHelpLabel(String helpText) {
        JLabel helpLabel = new JLabel("?");
        helpLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        helpLabel.setForeground(new Color(70, 130, 180));
        helpLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        helpLabel.setToolTipText("<html><div style='width:260px;'>" + helpText + "</div></html>");
        helpLabel.setCursor(java.awt.Cursor.getDefaultCursor());
        return helpLabel;
    }

    private Resolution parseResolution(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String[] parts = value.toLowerCase().split("x");
        if (parts.length != 2) {
            return null;
        }

        try {
            int width = Integer.parseInt(parts[0].trim());
            int height = Integer.parseInt(parts[1].trim());
            return new Resolution(width, height);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private JLabel createProfileStatusLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        label.setForeground(descriptionColor);
        return label;
    }

    private List<ConnectionProfile> buildAvailableProfiles(ServerConfig config) {
        List<ConnectionProfile> profiles = new ArrayList<>(config.getAvailableProfiles());
        if (profiles.isEmpty()) {
            profiles.add(ConnectionProfile.LAN);
            profiles.add(ConnectionProfile.WIFI);
        }
        profiles.sort(Comparator.comparing(ConnectionProfile::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        profiles.removeIf(profile -> profile == null);
        profiles.removeIf(profile -> profile.isSystemProfile() && !(ConnectionProfile.LAN.equals(profile) || ConnectionProfile.WIFI.equals(profile)));
        sortProfilesList(profiles);
        return profiles;
    }

    private void sortProfilesList(List<ConnectionProfile> profiles) {
        profiles.sort((a, b) -> {
            if (a.isSystemProfile() && b.isSystemProfile()) {
                if (ConnectionProfile.LAN_ID.equalsIgnoreCase(a.getId())) {
                    return -1;
                }
                if (ConnectionProfile.LAN_ID.equalsIgnoreCase(b.getId())) {
                    return 1;
                }
                return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
            }
            if (a.isSystemProfile()) {
                return -1;
            }
            if (b.isSystemProfile()) {
                return 1;
            }
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        });
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Validação", JOptionPane.WARNING_MESSAGE);
    }

    private record Resolution(int width, int height) {
    }
}
