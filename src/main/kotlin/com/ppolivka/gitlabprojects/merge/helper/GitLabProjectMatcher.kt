package com.ppolivka.gitlabprojects.merge.helper

import com.ppolivka.gitlabprojects.configuration.ProjectState
import com.ppolivka.gitlabprojects.configuration.SettingsState
import com.ppolivka.gitlabprojects.exception.GitLabException
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import org.apache.commons.lang3.StringUtils
import org.gitlab.api.models.GitlabProject
import java.util.*

class GitLabProjectMatcher {
    fun resolveProject(
        projectState: ProjectState,
        remote: GitRemote,
        repository: GitRepository?
    ): Optional<GitlabProject> {
        val remoteProjectName = remote.name
        val remoteUrl = remote.firstUrl!!

        if (projectState.getProjectId(remoteUrl) == null) {
            try {
                val projects = SettingsState.instance.api(repository!!).projects
                for (gitlabProject in projects) {
                    if (gitlabProject.name.lowercase(Locale.getDefault()) == remoteProjectName.lowercase(
                            Locale.getDefault()
                        ) || urlMatch(remoteUrl, gitlabProject.sshUrl) || urlMatch(
                            remoteUrl,
                            gitlabProject.httpUrl
                        )
                    ) {
                        val projectId = gitlabProject.id
                        projectState.setProjectId(remoteUrl, projectId)
                        return Optional.of(gitlabProject)
                    }
                }
            } catch (throwable: Throwable) {
                throw GitLabException(
                    "Cannot match project.",
                    throwable
                )
            }
        } else {
            try {
                return Optional.of(
                    SettingsState.instance.api(repository!!)
                        .getProject(projectState.getProjectId(remoteUrl))
                )
            } catch (e: Exception) {
                projectState.setProjectId(remoteUrl, null)
            }
        }

        return Optional.empty()
    }

    private fun urlMatch(remoteUrl: String?, apiUrl: String): Boolean {
        var formattedRemoteUrl = remoteUrl!!.trim { it <= ' ' }
        val formattedApiUrl = apiUrl.trim { it <= ' ' }
        formattedRemoteUrl = formattedRemoteUrl.replace("https://", "")
        formattedRemoteUrl = formattedRemoteUrl.replace("http://", "")
        return StringUtils.isNotBlank(formattedApiUrl) && StringUtils.isNotBlank(formattedRemoteUrl) && formattedApiUrl.lowercase(
            Locale.getDefault()
        ).contains(formattedRemoteUrl.lowercase(Locale.getDefault()))
    }

}
