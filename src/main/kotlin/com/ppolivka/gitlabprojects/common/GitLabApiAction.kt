package com.ppolivka.gitlabprojects.common

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.Convertor
import com.ppolivka.gitlabprojects.configuration.SettingsDialog
import com.ppolivka.gitlabprojects.configuration.SettingsState
import com.ppolivka.gitlabprojects.util.GitLabUtil
import com.ppolivka.gitlabprojects.util.MessageUtil
import javax.swing.Icon

/**
 * Abstract Action class that provides method for validating GitLab API Settings
 *
 * @author ppolivka
 * @since 22.12.2015
 */
abstract class GitLabApiAction(text: String?, description: String?, icon: Icon?) :
    DumbAwareAction(text, description, icon) {

    protected lateinit var project: Project
    protected var file: VirtualFile? = null

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        project = anActionEvent.getData(CommonDataKeys.PROJECT) ?: return
        if (project.isDisposed) {
            return
        }
        file = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE)

        if (!validateGitLabApi(project, file)) {
            return
        }

        apiValidAction(anActionEvent)
    }

    /**
     * Abstract method that is called after GitLab Api is validated,
     * we can assume that login credentials are there and api valid
     *
     * @param anActionEvent event information
     */
    abstract fun apiValidAction(anActionEvent: AnActionEvent)

    companion object {

        /**
         * Validate git lab api settings
         * If API is not valid, Setting dialog will be displayed
         * If API is still not configured after that false is returned
         *
         * @param project the project
         * @return true if API is OK, false if not
         */
        fun validateGitLabApi(project: Project, virtualFile: VirtualFile?): Boolean {
            val isApiSetup = GitLabUtil.computeValueInModal(
                project,
                "Validating GitLab Api...",
                false,
                Convertor {
                    try {
                        SettingsState.instance.isApiValid(project, virtualFile)
                        return@Convertor true
                    } catch (e: Throwable) {
                        return@Convertor false
                    }
                })
            var isOk = true
            if (!isApiSetup) {
                //Git Lab Not configured
                val configurationDialog = SettingsDialog(project)
                configurationDialog.show()
                if (configurationDialog.isOK && configurationDialog.isModified) {
                    try {
                        configurationDialog.apply()
                    } catch (ignored: ConfigurationException) {
                        isOk = false
                    }
                }
                if (isOk) {
                    isOk = configurationDialog.isOK
                }
            }
            if (!isOk) {
                MessageUtil.showErrorDialog(
                    project,
                    "Cannot log-in to GitLab Server with provided token",
                    "Cannot Login To GitLab"
                )
            }
            return isOk
        }
    }
}
