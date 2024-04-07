package com.ppolivka.gitlabprojects.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsNotifier

/**
 * Notification utils
 *
 * @author ppolivka
 * @since 5.11.2015
 */
object MessageUtil {
    private const val DISPLAY_ID = "gitlabprojects.msg"

    fun showErrorDialog(project: Project, message: String?, title: String?) {
        VcsNotifier.getInstance(project).notifyError(DISPLAY_ID, title!!, message!!)
    }

    fun showWarningDialog(project: Project, message: String?, title: String?) {
        VcsNotifier.getInstance(project).notifyWarning(DISPLAY_ID, title!!, message!!)
    }

    fun showInfoMessage(project: Project, message: String?, title: String?) {
        VcsNotifier.getInstance(project).notifyInfo(DISPLAY_ID, title!!, message!!)
    }
}
