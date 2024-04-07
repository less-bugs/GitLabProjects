package com.ppolivka.gitlabprojects.common

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.ppolivka.gitlabprojects.configuration.SettingsState
import javax.swing.Icon

/**
 * Abstract Action class that provides method for validating GitLab API Settings
 *
 * @author ppolivka
 * @since 22.12.2015
 */
abstract class NoGitLabApiAction : DumbAwareAction {
    protected lateinit var project: Project
    protected var file: VirtualFile? = null

    constructor()

    constructor(text: String?) : super(text)

    constructor(text: String?, description: String?, icon: Icon?) : super(text, description, icon)

    override fun actionPerformed(anActionEvent: AnActionEvent) {
        project = anActionEvent.getData(CommonDataKeys.PROJECT) ?: return
        file = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE)

        if (project.isDisposed) {
            return
        }

        if (SettingsState.instance.gitlabServers.isEmpty()) {
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

}
