package com.ppolivka.gitlabprojects.share

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.CollectionComboBoxModel
import com.ppolivka.gitlabprojects.api.dto.NamespaceDto
import com.ppolivka.gitlabprojects.configuration.SettingsState
import com.ppolivka.gitlabprojects.dto.GitlabServer
import com.ppolivka.gitlabprojects.util.MessageUtil.showErrorDialog
import org.apache.commons.lang3.StringUtils
import java.io.IOException
import javax.swing.*
import javax.swing.border.Border

/**
 * Dialog that is displayed when sharing project to git lab
 *
 * @author ppolivka
 * @since 28.10.2015
 */
class GitLabShareDialog(private val project: Project?) : DialogWrapper(
    project
) {
    lateinit var mainView: JPanel
    lateinit var isPrivate: JRadioButton
    lateinit var isPublic: JRadioButton
    lateinit var projectName: JTextField
    lateinit var commitMessage: JTextArea
    lateinit var isInternal: JRadioButton
    lateinit var groupList: JComboBox<NamespaceDto?>
    lateinit var refreshButton: JButton
    lateinit var isSSHAuth: JRadioButton
    lateinit var isHTTPAuth: JRadioButton
    lateinit var serverList: JComboBox<GitlabServer>

    init {
        init()
    }

    override fun init() {
        super.init()
        title = "Share on GitLab"
        setOKButtonText("Share")

        val servers = ArrayList(SettingsState.instance.gitlabServers)
        serverList.setModel(CollectionComboBoxModel(servers, servers[0]))

        val emptyBorder: Border = BorderFactory.createCompoundBorder()
        refreshButton.border = emptyBorder

        commitMessage.text = "Initial commit"

        isInternal.isSelected = true

        val visibilityGroup = ButtonGroup()
        visibilityGroup.add(isPrivate)
        visibilityGroup.add(isInternal)
        visibilityGroup.add(isPublic)

        isSSHAuth.isSelected = true

        val authGroup = ButtonGroup()
        authGroup.add(isHTTPAuth)
        authGroup.add(isSSHAuth)

        reloadGroupList()

        refreshButton.addActionListener { reloadGroupList() }

        serverList.addActionListener { reloadGroupList() }
    }

    override fun doValidate(): ValidationInfo? {
        if (StringUtils.isBlank(projectName.text)) {
            return ValidationInfo("Project name cannot be empty", projectName)
        }
        if (StringUtils.isBlank(commitMessage.text)) {
            return ValidationInfo("Initial commit message cannot be empty", commitMessage)
        }
        return null
    }

    private fun reloadGroupList() {
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project, "Refreshing group list.."
        ) {
            var isError: Boolean = false
            override fun run(progressIndicator: ProgressIndicator) {
                try {
                    val namespaces: MutableList<NamespaceDto?> = ArrayList()
                    namespaces.add(object : NamespaceDto() {
                        init {
                            id = 0
                            path = "Default"
                        }
                    })
                    val remoteNamespaces: List<NamespaceDto?> = SettingsState.instance.api(
                        (serverList.selectedItem as GitlabServer)
                    ).namespaces
                    namespaces.addAll(remoteNamespaces)
                    val collectionComboBoxModel: CollectionComboBoxModel<NamespaceDto?> =
                        CollectionComboBoxModel<NamespaceDto?>(namespaces, namespaces[0])
                    groupList.setModel(collectionComboBoxModel)
                } catch (e: IOException) {
                    isError = true
                }
            }

            override fun onSuccess() {
                super.onSuccess()
                if (isError) {
                    showErrorDialog(project, "Groups cannot be refreshed", "Error Loading Groups")
                    close(CLOSE_EXIT_CODE)
                }
            }
        })
    }

    override fun createCenterPanel(): JComponent {
        return mainView
    }

}
