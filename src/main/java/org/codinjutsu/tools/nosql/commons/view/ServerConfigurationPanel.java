/*
 * Copyright (c) 2015 David Boissier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codinjutsu.tools.nosql.commons.view;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.Ref;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.commons.lang.StringUtils;
import org.codinjutsu.tools.nosql.DatabaseVendor;
import org.codinjutsu.tools.nosql.ServerConfiguration;
import org.codinjutsu.tools.nosql.commons.logic.ConfigurationException;
import org.codinjutsu.tools.nosql.commons.logic.DatabaseClient;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ServerConfigurationPanel extends JPanel {

    private JPanel rootPanel;
    private JTextField labelField;
    private JPanel authenticationContainer;
    private JTextField serverUrlField;
    private JTextField userDatabaseField;
    private JCheckBox autoConnectCheckBox;
    private JButton testConnectionButton;
    private JLabel databaseTipsLabel;
    private JLabel databaseVendorLabel;

    private final Project project;

    private final DatabaseClient databaseClient;
    private final DatabaseVendor databaseVendor;
    private final AuthenticationView authenticationView;


    public ServerConfigurationPanel(Project project,
                                    DatabaseVendor databaseVendor,
                                    DatabaseClient databaseClient,
                                    AuthenticationView authenticationView) {
        this.project = project;
        $$$setupUI$$$();

        Font theFont = getTheFont(null, Font.ITALIC, -1, databaseTipsLabel.getFont());
        if (theFont != null) {
            databaseTipsLabel.setFont(theFont);
        }
        this.databaseClient = databaseClient;
        this.databaseVendor = databaseVendor;
        this.authenticationView = authenticationView;

        setLayout(new BorderLayout());
        add(rootPanel, BorderLayout.CENTER);
        authenticationContainer.add(authenticationView.getComponent());

        labelField.setName("labelField");
        databaseVendorLabel.setName("databaseVendorLabel");
        databaseVendorLabel.setText(databaseVendor.name);
        databaseVendorLabel.setIcon(databaseVendor.icon);

        databaseTipsLabel.setName("databaseTipsLabel");
        databaseTipsLabel.setText(databaseVendor.tips);

        serverUrlField.setName("serverUrlField");

        authenticationContainer.setBorder(IdeBorderFactory.createTitledBorder("Authentication settings", true));
        userDatabaseField.setName("userDatabaseField");
        userDatabaseField.setToolTipText("If your access is restricted to a specific database (e.g.: MongoLab), you can set it right here");

        autoConnectCheckBox.setName("autoConnectField");

        testConnectionButton.setName("testConnection");

        initListeners();
    }

    private void initListeners() {
        testConnectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {

                final Ref<Exception> excRef = new Ref<>();
                final ProgressManager progressManager = ProgressManager.getInstance();
                progressManager.runProcessWithProgressSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        ServerConfiguration configuration = createServerConfigurationForTesting();

                        final ProgressIndicator progressIndicator = progressManager.getProgressIndicator();
                        if (progressIndicator != null) {
                            progressIndicator.setText("Connecting to " + configuration.getServerUrl());
                        }
                        try {
                            databaseClient.connect(configuration);
                        } catch (Exception ex) {
                            excRef.set(ex);
                        }
                    }

                }, "Testing connection for " + databaseVendor.name, true, ServerConfigurationPanel.this.project);

                if (!excRef.isNull()) {
                    Messages.showErrorDialog(rootPanel, excRef.get().getMessage(), "Connection test failed");
                } else {
                    Messages.showInfoMessage(rootPanel, "Connection test successful for " + databaseVendor.name, "Connection test successful");
                }
            }
        });
    }

    public void loadConfigurationData(ServerConfiguration configuration) {
        labelField.setText(configuration.getLabel());
        serverUrlField.setText(configuration.getServerUrl());
        userDatabaseField.setText(configuration.getUserDatabase());
        autoConnectCheckBox.setSelected(configuration.isConnectOnIdeStartup());

        authenticationView.load(configuration.getAuthenticationSettings());
    }

    @NotNull
    private ServerConfiguration createServerConfigurationForTesting() {
        ServerConfiguration configuration = new ServerConfiguration();
        configuration.setDatabaseVendor(databaseVendor);
        configuration.setServerUrl(getServerUrls());
        configuration.setAuthenticationSettings(authenticationView.create());
        configuration.setUserDatabase(getUserDatabase());
        return configuration;
    }

    public void applyConfigurationData(ServerConfiguration configuration) {

        configuration.setLabel(getLabel());
        configuration.setDatabaseVendor(databaseVendor);
        configuration.setServerUrl(getServerUrls());
        configuration.setAuthenticationSettings(authenticationView.create());

        configuration.setUserDatabase(getUserDatabase());
        configuration.setConnectOnIdeStartup(isAutoConnect());
    }

    public ValidationInfo validateInputs() {
        if (StringUtils.isEmpty(getLabel())) {
            return new ValidationInfo("Label should be set");
        }
        String serverUrl = getServerUrls();
        if (serverUrl == null) {
            return new ValidationInfo("URL(s) should be set");
        }
        return null;
    }


    private void validateUrls() {
        String serverUrl = getServerUrls();
        if (serverUrl == null) {
            throw new ConfigurationException("URL(s) should be set");
        }
        for (String subServerUrl : serverUrl.split(",")) {
            String[] host_port = subServerUrl.split(":");
            if (host_port.length < 2) {
                throw new ConfigurationException(String.format("URL '%s' format is incorrect. It should be 'host:port'", subServerUrl));
            }

            try {
                Integer.valueOf(host_port[1].trim());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(String.format("Port in the URL '%s' is incorrect. It should be a number", subServerUrl));
            }
        }

    }

    private String getLabel() {
        String label = labelField.getText();
        if (StringUtils.isNotBlank(label)) {
            return label;
        }
        return null;
    }

    private String getServerUrls() {
        String serverUrl = serverUrlField.getText();
        if (StringUtils.isNotBlank(serverUrl)) {
            return serverUrl;
        }
        return null;
    }

    private String getUserDatabase() {
        String userDatabase = userDatabaseField.getText();
        if (StringUtils.isNotBlank(userDatabase)) {
            return userDatabase;
        }
        return null;
    }


    private boolean isAutoConnect() {
        return autoConnectCheckBox.isSelected();
    }

    private void createUIComponents() {
//        shellWorkingDirField = new TextFieldWithBrowseButton();
//        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false);
//        ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> browseFolderActionListener =
//                new ComponentWithBrowseButton.BrowseFolderActionListener<>("Shell working directory",
//                        null,
//                        shellWorkingDirField,
//                        null,
//                        fileChooserDescriptor,
//                        TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
//        shellWorkingDirField.addBrowseFolderListener(null, browseFolderActionListener, false);
//        shellWorkingDirField.setName("shellWorkingDirField");
    }


    private Font getTheFont(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(9, 3, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Server url(s):");
        rootPanel.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        rootPanel.add(spacer1, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        serverUrlField = new JTextField();
        serverUrlField.setText("");
        rootPanel.add(serverUrlField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel1, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        testConnectionButton = new JButton();
        testConnectionButton.setText("Test Connection");
        panel1.add(testConnectionButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel1.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Label:");
        rootPanel.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelField = new JTextField();
        rootPanel.add(labelField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        databaseTipsLabel = new JLabel();
        databaseTipsLabel.setText("format: host:port. If replicat set: host:port1,host:port2,...");
        rootPanel.add(databaseTipsLabel, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("User Database:");
        rootPanel.add(label3, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        autoConnectCheckBox = new JCheckBox();
        autoConnectCheckBox.setHorizontalTextPosition(11);
        autoConnectCheckBox.setText("Connect on IDE startup");
        rootPanel.add(autoConnectCheckBox, new GridConstraints(7, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        userDatabaseField = new JTextField();
        rootPanel.add(userDatabaseField, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Database Vendor:");
        rootPanel.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        databaseVendorLabel = new JLabel();
        databaseVendorLabel.setText("Label");
        rootPanel.add(databaseVendorLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        authenticationContainer = new JPanel();
        authenticationContainer.setLayout(new BorderLayout(0, 0));
        rootPanel.add(authenticationContainer, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }
}
