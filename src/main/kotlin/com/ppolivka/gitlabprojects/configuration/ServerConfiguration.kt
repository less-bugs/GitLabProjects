package com.ppolivka.gitlabprojects.configuration

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.EnumComboBoxModel
import com.ppolivka.gitlabprojects.configuration.ApiToRepoUrlConverter.convertApiUrlToRepoUrl
import com.ppolivka.gitlabprojects.configuration.SettingsState.Companion.instance
import com.ppolivka.gitlabprojects.dto.GitlabServer
import com.ppolivka.gitlabprojects.dto.GitlabServer.CheckoutType
import java.awt.Desktop
import java.net.URI
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ServerConfiguration(private val gitlabServer: GitlabServer) :
    DialogWrapper(false) {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private lateinit var panel: JPanel
    private lateinit var apiURl: JTextField
    private lateinit var repositoryUrl: JTextField
    private lateinit var token: JTextField
    private lateinit var tokenPage: JButton
    private lateinit var checkoutMethod: JComboBox<CheckoutType>
    private lateinit var removeOnMerge: JCheckBox

    init {
        init()
        title = "GitLab Server Details"
    }

    override fun init() {
        super.init()

        setupModel()
        fillFormFromDto()
        setupListeners()
    }

    override fun doValidate(): ValidationInfo? {
        val apiUrl = apiURl.text
        val tokenString = token.text
        if (apiUrl.isNullOrBlank() && tokenString.isNullOrBlank()) {
            return null
        }
        try {
            if (apiUrl.isNotBlank() && tokenString.isNotBlank()) {
                if (!isValidUrl(apiUrl)) {
                    return ValidationInfo(SettingError.NOT_A_URL.message(), apiURl)
                } else {
                    val infoFuture = executor.submit<ValidationInfo?> {
                        instance.isApiValid(apiUrl, tokenString)
                        null
                    }
                    return try {
                        infoFuture[5000, TimeUnit.MILLISECONDS]
                    } catch (e: Exception) {
                        ValidationInfo(SettingError.GENERAL_ERROR.message())
                    }
                }
            }
        } catch (e: Exception) {
            return ValidationInfo(SettingError.GENERAL_ERROR.message())
        }
        return null
    }

    override fun doOKAction() {
        super.doOKAction()
        gitlabServer.apiUrl = apiURl.text
        gitlabServer.apiToken = token.text
        if (repositoryUrl.text.isNullOrBlank()) {
            gitlabServer.repositoryUrl = repositoryUrl.text
        } else {
            gitlabServer.repositoryUrl = convertApiUrlToRepoUrl(apiURl.text)
        }
        gitlabServer.preferredConnection = CheckoutType.entries[checkoutMethod.selectedIndex]
        gitlabServer.removeSourceBranch = removeOnMerge.isSelected
        instance.addServer(gitlabServer)
    }

    private fun setupListeners() {
        tokenPage.addActionListener { openWebPage(generateHelpUrl()) }
        onServerChange()
        apiURl.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) {
                onServerChange()
            }

            override fun removeUpdate(e: DocumentEvent) {
                onServerChange()
            }

            override fun changedUpdate(e: DocumentEvent) {
                onServerChange()
            }
        })
    }

    private fun setupModel() {
        checkoutMethod.setModel(EnumComboBoxModel(CheckoutType::class.java))
    }

    private fun fillFormFromDto() {
        checkoutMethod.selectedIndex = gitlabServer.preferredConnection.ordinal
        removeOnMerge.isSelected = gitlabServer.removeSourceBranch
        apiURl.text = gitlabServer.apiUrl
        repositoryUrl.text = gitlabServer.repositoryUrl
        token.text = gitlabServer.apiToken
    }

    private fun openWebPage(uri: String) {
        val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(URI(uri))
            } catch (ignored: Exception) {
            }
        }
    }

    private fun generateHelpUrl(): String {
        val hostText = apiURl.text
        val helpUrl = StringBuilder()
        helpUrl.append(hostText)
        if (!hostText.endsWith("/")) {
            helpUrl.append("/")
        }
        helpUrl.append("profile/personal_access_tokens")
        return helpUrl.toString()
    }

    private fun onServerChange() {
        val validationInfo = doValidate()
        if (validationInfo == null || (validationInfo.message != SettingError.NOT_A_URL.message())) {
            tokenPage.isEnabled = true
            tokenPage.toolTipText =
                """
                API Key can be find in your profile setting inside GitLab Server: 
                ${generateHelpUrl()}
                """.trimIndent()
        } else {
            tokenPage.isEnabled = false
        }
    }

    override fun createCenterPanel(): JComponent {
        return panel
    }

    companion object {
        private fun isValidUrl(s: String): Boolean {
            try {
                URI(s)
                return true
            } catch (e: Exception) {
                return false
            }
        }
    }
}
