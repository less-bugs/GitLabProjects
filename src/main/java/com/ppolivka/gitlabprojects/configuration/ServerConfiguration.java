package com.ppolivka.gitlabprojects.configuration;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.EnumComboBoxModel;
import com.ppolivka.gitlabprojects.dto.GitlabServer;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public class ServerConfiguration extends DialogWrapper {

    private GitlabServer gitlabServer;
    private SettingsState settingsState = SettingsState.Companion.getInstance();
    private ExecutorService executor = Executors.newSingleThreadExecutor();


    private JPanel panel;
    private JTextField apiURl;
    private JTextField repositoryUrl;
    private JTextField token;
    private JButton tokenPage;
    private JComboBox checkoutMethod;
    private JCheckBox removeOnMerge;

    protected ServerConfiguration(@Nullable GitlabServer gitlabServer) {
        super(false);
        if(gitlabServer == null) {
            this.gitlabServer = new GitlabServer();
        } else {
            this.gitlabServer = gitlabServer;
        }
        init();
        setTitle("GitLab Server Details");
    }

    @Override
    protected void init() {
        super.init();

        setupModel();
        fillFormFromDto();
        setupListeners();

    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        final String apiUrl = apiURl.getText();
        final String tokenString = token.getText();
        if(StringUtils.isBlank(apiUrl) && StringUtils.isBlank(tokenString)) {
            return null;
        }
        try {
            if (isNotBlank(apiUrl) && isNotBlank(tokenString)) {
                if (!isValidUrl(apiUrl)) {
                    return new ValidationInfo(SettingError.NOT_A_URL.message(), apiURl);
                } else {

                    Future<ValidationInfo> infoFuture = executor.submit(() -> {
						settingsState.isApiValid(apiUrl, tokenString);
						return null;
					});
                    try {
                        ValidationInfo info = infoFuture.get(5000, TimeUnit.MILLISECONDS);
                        return info;
                    } catch (Exception e) {
                        return new ValidationInfo(SettingError.GENERAL_ERROR.message());
                    }
                }
            }
        } catch (Exception e) {
            return new ValidationInfo(SettingError.GENERAL_ERROR.message());
        }
        return null;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        gitlabServer.setApiUrl(apiURl.getText());
        gitlabServer.setApiToken(token.getText());
        if(StringUtils.isNotBlank(repositoryUrl.getText())) {
            gitlabServer.setRepositoryUrl(repositoryUrl.getText());
        } else {
            gitlabServer.setRepositoryUrl(ApiToRepoUrlConverter.INSTANCE.convertApiUrlToRepoUrl(apiURl.getText()));
        }
        gitlabServer.setPreferredConnection(GitlabServer.CheckoutType.values()[checkoutMethod.getSelectedIndex()]);
        gitlabServer.setRemoveSourceBranch(removeOnMerge.isSelected());
        settingsState.addServer(gitlabServer);
    }

    private static boolean isValidUrl(String s) {
        try {
            URI uri = new URI(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void setupListeners() {
        tokenPage.addActionListener(e -> openWebPage(generateHelpUrl()));
        onServerChange();
        apiURl.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onServerChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onServerChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onServerChange();
            }
        });
    }

    private void setupModel() {
        checkoutMethod.setModel(new EnumComboBoxModel(GitlabServer.CheckoutType.class));
    }

    private void fillFormFromDto() {
        checkoutMethod.setSelectedIndex(gitlabServer.getPreferredConnection().ordinal());
        removeOnMerge.setSelected(gitlabServer.getRemoveSourceBranch());
        apiURl.setText(gitlabServer.getApiUrl());
        repositoryUrl.setText(gitlabServer.getRepositoryUrl());
        token.setText(gitlabServer.getApiToken());
    }

    private void openWebPage(String uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URI(uri));
            } catch (Exception ignored) {
            }
        }
    }

    private String generateHelpUrl() {
        final String hostText = apiURl.getText();
        StringBuilder helpUrl = new StringBuilder();
        helpUrl.append(hostText);
        if (!hostText.endsWith("/")) {
            helpUrl.append("/");
        }
        helpUrl.append("profile/personal_access_tokens");
        return helpUrl.toString();
    }

    private void onServerChange() {
        ValidationInfo validationInfo = doValidate();
        if (validationInfo == null || (!validationInfo.message.equals(SettingError.NOT_A_URL.message()))) {
            tokenPage.setEnabled(true);
            tokenPage.setToolTipText("API Key can be find in your profile setting inside GitLab Server: \n" + generateHelpUrl());
        } else {
            tokenPage.setEnabled(false);
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }
}
