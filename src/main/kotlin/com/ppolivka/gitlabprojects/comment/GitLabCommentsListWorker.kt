package com.ppolivka.gitlabprojects.comment

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.Convertor
import com.ppolivka.gitlabprojects.configuration.SettingsState
import com.ppolivka.gitlabprojects.util.GitLabUtil
import com.ppolivka.gitlabprojects.util.MessageUtil
import org.gitlab.api.models.GitlabMergeRequest
import org.gitlab.api.models.GitlabNote
import java.io.IOException

/**
 * Worker for extracting comments on merge request
 *
 * @author ppolivka
 * @since 1.3.2
 */
class GitLabCommentsListWorker {
    lateinit var mergeRequest: GitlabMergeRequest
    var comments: List<GitlabNote>? = null
    var file: VirtualFile? = null

    companion object {

        fun create(
            project: Project,
            mergeRequest: GitlabMergeRequest,
            file: VirtualFile?
        ): GitLabCommentsListWorker {
            return GitLabUtil.computeValueInModal(
                project,
                "Loading comments...",
                Convertor { indicator: ProgressIndicator? ->
                    val commentsListWorker = GitLabCommentsListWorker()
                    commentsListWorker.mergeRequest = mergeRequest
                    try {
                        commentsListWorker.comments =
                            SettingsState.instance.api(project, file)
                                .getMergeRequestComments(mergeRequest)
                    } catch (e: IOException) {
                        commentsListWorker.comments = emptyList<GitlabNote>()
                        MessageUtil.showErrorDialog(
                            project,
                            "Cannot load comments from GitLab API",
                            "Cannot Load Comments"
                        )
                    }
                    commentsListWorker
                } as Convertor<ProgressIndicator?, GitLabCommentsListWorker>)
        }
    }
}
