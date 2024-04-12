package com.ppolivka.gitlabprojects.merge.list

import com.intellij.notification.NotificationListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsNotifier
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.Convertor
import com.ppolivka.gitlabprojects.configuration.ProjectState
import com.ppolivka.gitlabprojects.configuration.SettingsState
import com.ppolivka.gitlabprojects.exception.MergeRequestException
import com.ppolivka.gitlabprojects.merge.GitLabDiffViewWorker
import com.ppolivka.gitlabprojects.merge.GitLabMergeRequestWorker
import com.ppolivka.gitlabprojects.util.GitLabUtil
import com.ppolivka.gitlabprojects.util.MessageUtil
import git4idea.commands.Git
import git4idea.repo.GitRepository
import org.gitlab.api.models.GitlabMergeRequest
import org.gitlab.api.models.GitlabProject
import java.io.IOException

/**
 * Worker for listing and accepting merge request
 *
 * @author ppolivka
 * @since 31.10.2015
 */
class GitLabMergeRequestListWorker : GitLabMergeRequestWorker {
    override var git: Git? = null
    override var project: Project? = null
    override var projectState: ProjectState? = null
    override var gitRepository: GitRepository? = null
    override var remoteUrl: String? = null
    override var gitlabProject: GitlabProject? = null
    override var remoteProjectName: String? = null
    override var diffViewWorker: GitLabDiffViewWorker? = null

    lateinit var mergeRequests: List<GitlabMergeRequest>

    fun mergeBranches(project: Project, mergeRequest: GitlabMergeRequest) {
        object : Task.Backgroundable(project, "Merging Branches...") {
            override fun run(indicator: ProgressIndicator) {
                try {
                    SettingsState.instance.api(gitRepository!!)
                        .acceptMergeRequest(gitlabProject, mergeRequest)
                    VcsNotifier.getInstance(project)
                        .notifyImportantInfo(
                            MessageUtil.DISPLAY_ID,
                            "Merged",
                            "Merge request is merged.",
                            NotificationListener.URL_OPENING_LISTENER
                        )
                } catch (e: IOException) {
                    MessageUtil.showErrorDialog(
                        project,
                        "Cannot create merge this request",
                        "Cannot Merge"
                    )
                }
            }
        }.queue()
    }

    companion object {

        fun create(project: Project, file: VirtualFile?): GitLabMergeRequestListWorker? {
            return GitLabUtil.computeValueInModal(project, "Loading data...", Convertor {
                val mergeRequestListWorker = GitLabMergeRequestListWorker()
                try {
                    GitLabMergeRequestWorker.Util.fillRequiredInfo(
                        mergeRequestListWorker,
                        project,
                        file
                    )
                } catch (e: MergeRequestException) {
                    return@Convertor null
                }

                try {
                    mergeRequestListWorker.mergeRequests = SettingsState.instance.api(project, file)
                        .getMergeRequests(mergeRequestListWorker.gitlabProject)
                } catch (e: IOException) {
                    mergeRequestListWorker.mergeRequests = emptyList()
                    MessageUtil.showErrorDialog(
                        project,
                        "Cannot load merge requests from GitLab API",
                        "Cannot Load Merge Requests"
                    )
                }
                mergeRequestListWorker
            })
        }
    }
}
