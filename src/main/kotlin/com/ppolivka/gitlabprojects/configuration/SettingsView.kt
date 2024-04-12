package com.ppolivka.gitlabprojects.configuration

import com.intellij.openapi.options.SearchableConfigurable
import com.ppolivka.gitlabprojects.common.ReadOnlyTableModel
import com.ppolivka.gitlabprojects.configuration.SettingsState.Companion.instance
import com.ppolivka.gitlabprojects.dto.GitlabServer
import git4idea.DialogManager
import org.jetbrains.annotations.Nls
import javax.swing.*
import javax.swing.table.TableModel

/**
 * Dialog for GitLab setting configuration
 *
 * @author ppolivka
 * @since 27.10.2015
 */
class SettingsView : SearchableConfigurable {
    private lateinit var mainPanel: JPanel
    private lateinit var serverTable: JTable
    private lateinit var addNewOneButton: JButton
    private lateinit var editButton: JButton
    private lateinit var deleteButton: JButton

    fun setup() {
        addNewOneButton.addActionListener {
            val serverConfiguration = ServerConfiguration(GitlabServer.EMPTY)
            DialogManager.show(serverConfiguration)
            reset()
        }
        deleteButton.addActionListener {
            val server = selectedServer
            if (server != null) {
                instance.deleteServer(server)
                reset()
            }
        }
        editButton.addActionListener {
            val server = selectedServer ?: GitlabServer.EMPTY
            val serverConfiguration = ServerConfiguration(server)
            DialogManager.show(serverConfiguration)
            reset()
        }
    }

    private val selectedServer: GitlabServer?
        get() {
            if (serverTable.selectedRow >= 0) {
                return serverTable.getValueAt(serverTable.selectedRow, 0) as GitlabServer
            }
            return null
        }

    private fun serverModel(servers: Collection<GitlabServer>): TableModel {
        val columnNames = arrayOf("", "Server", "Token", "Checkout Method")
        val data = Array(servers.size) { arrayOfNulls<Any>(columnNames.size) }
        for ((i, server) in servers.withIndex()) {
            val row = arrayOfNulls<Any>(columnNames.size)
            row[0] = server
            row[1] = server.apiUrl
            row[2] = server.apiToken
            row[3] = server.preferredConnection.name
            data[i] = row
        }
        return ReadOnlyTableModel(data, columnNames)
    }

    override fun getId(): String {
        return DIALOG_TITLE
    }

    override fun enableSearch(s: String): Runnable? {
        return null
    }

    override fun getDisplayName(): @Nls String {
        return DIALOG_TITLE
    }

    override fun getHelpTopic(): String? {
        return null
    }

    override fun createComponent(): JComponent {
        reset()
        return mainPanel
    }

    override fun isModified(): Boolean {
        return false
    }

    override fun apply() {
    }

    override fun reset() {
        fill(instance)
    }

    override fun disposeUIResources() {
    }

    fun fill(settingsState: SettingsState) {
        serverTable.model = serverModel(settingsState.gitlabServers)
        serverTable.columnModel.getColumn(0).minWidth = 0
        serverTable.columnModel.getColumn(0).maxWidth = 0
        serverTable.columnModel.getColumn(0).width = 0
        serverTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        serverTable.selectionModel.addListSelectionListener {
            editButton.isEnabled = true
            deleteButton.isEnabled = true
        }
    }

    companion object {
        const val DIALOG_TITLE: String = "GitLab Settings"
    }
}
