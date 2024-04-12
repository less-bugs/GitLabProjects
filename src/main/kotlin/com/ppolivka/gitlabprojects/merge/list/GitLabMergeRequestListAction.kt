package com.ppolivka.gitlabprojects.merge.list

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.ppolivka.gitlabprojects.common.GitLabApiAction
import git4idea.DialogManager

/**
 * Action for accepting merge request
 *
 * @author ppolivka
 * @since 31.10.2015
 */
class GitLabMergeRequestListAction : GitLabApiAction(
    "_List Merge Requests...",
    "List of all merge requests for this project",
    AllIcons.Vcs.Merge
) {
    override fun apiValidAction(anActionEvent: AnActionEvent) {
        val mergeRequestListWorker = GitLabMergeRequestListWorker.create(project, file)
        DialogManager.show(GitLabMergeRequestListDialog(project, mergeRequestListWorker!!))
    }
}
