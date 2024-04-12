package com.ppolivka.gitlabprojects.merge.request

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.ppolivka.gitlabprojects.common.GitLabApiAction
import com.ppolivka.gitlabprojects.request.CreateMergeRequestDialog
import com.ppolivka.gitlabprojects.util.GitLabUtil
import git4idea.DialogManager

/**
 * GitLab Merge Request Action class
 *
 * @author ppolivka
 * @since 30.10.2015
 */
class GitLabMergeRequestAction : GitLabApiAction(
    "Create _Merge Request...",
    "Creates merge request from current branch",
    AllIcons.Vcs.Merge
) {
    override fun apiValidAction(anActionEvent: AnActionEvent) {
        if (!GitLabUtil.testGitExecutable(project)) {
            return
        }

        val mergeRequestWorker = GitLabCreateMergeRequestWorker.create(project, file)
        if (mergeRequestWorker != null) {
            val createMergeRequestDialog =
                CreateMergeRequestDialog(
                    project,
                    mergeRequestWorker
                )
            DialogManager.show(createMergeRequestDialog)
        }
    }
}
