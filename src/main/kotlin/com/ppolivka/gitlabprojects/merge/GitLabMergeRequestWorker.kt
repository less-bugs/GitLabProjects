package com.ppolivka.gitlabprojects.merge

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.ppolivka.gitlabprojects.configuration.ProjectState
import com.ppolivka.gitlabprojects.configuration.SettingsState
import com.ppolivka.gitlabprojects.exception.MergeRequestException
import com.ppolivka.gitlabprojects.merge.helper.GitLabProjectMatcher
import com.ppolivka.gitlabprojects.util.GitLabUtil
import com.ppolivka.gitlabprojects.util.MessageUtil
import git4idea.commands.Git
import git4idea.repo.GitRepository
import org.gitlab.api.models.GitlabProject

/**
 * Interface for worker classes that are related to merge requests
 *
 * @author ppolivka
 * @since 31.10.2015
 */
interface GitLabMergeRequestWorker {
    var git: Git?

    var project: Project?

    var projectState: ProjectState?

    var gitRepository: GitRepository?

    var remoteUrl: String?

    var gitlabProject: GitlabProject?

    var remoteProjectName: String?

    var diffViewWorker: GitLabDiffViewWorker?

    object Util {
        private val projectMatcher = GitLabProjectMatcher()

        @Throws(MergeRequestException::class)
        fun fillRequiredInfo(
            mergeRequestWorker: GitLabMergeRequestWorker,
            project: Project,
            file: VirtualFile?
        ) {
            val projectState = ProjectState.getInstance(project)
            mergeRequestWorker.projectState = projectState

            mergeRequestWorker.project = project

            val git = ServiceManager.getService(
                Git::class.java
            )
            mergeRequestWorker.git = git

            val gitRepository = GitLabUtil.getGitRepository(project, file)
            if (gitRepository == null) {
                MessageUtil.showErrorDialog(
                    project,
                    "Can't find git repository",
                    CANNOT_CREATE_MERGE_REQUEST
                )
                throw MergeRequestException()
            }
            gitRepository.update()
            mergeRequestWorker.gitRepository = gitRepository

            val remote = GitLabUtil.findGitLabRemote(gitRepository)
            if (remote == null) {
                MessageUtil.showErrorDialog(
                    project,
                    "Can't find GitLab remote",
                    CANNOT_CREATE_MERGE_REQUEST
                )
                throw MergeRequestException()
            }

            val remoteProjectName = remote.first.name
            mergeRequestWorker.remoteProjectName = remoteProjectName
            mergeRequestWorker.remoteUrl = remote.getSecond()

            try {
                val projectId: Int
                val gitlabProject =
                    projectMatcher.resolveProject(projectState, remote.getFirst(), gitRepository)
                projectId = gitlabProject.orElseThrow { RuntimeException("No project found") }.id

                mergeRequestWorker.gitlabProject =
                    SettingsState.instance.api(gitRepository).getProject(projectId)
            } catch (e: Exception) {
                MessageUtil.showErrorDialog(
                    project,
                    "Cannot find this project in GitLab Remote",
                    CANNOT_CREATE_MERGE_REQUEST
                )
                throw MergeRequestException(
                    e
                )
            }

            mergeRequestWorker.diffViewWorker =
                GitLabDiffViewWorker(project, mergeRequestWorker.gitRepository!!)
        }
    }

    companion object {
        const val CANNOT_CREATE_MERGE_REQUEST: String = "Cannot Create Merge Request"
    }
}
