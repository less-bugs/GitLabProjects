package com.ppolivka.gitlabprojects.checkout

import com.intellij.dvcs.hosting.RepositoryListLoader
import com.intellij.dvcs.hosting.RepositoryListLoadingException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.ppolivka.gitlabprojects.api.dto.ProjectDto
import com.ppolivka.gitlabprojects.configuration.SettingsDialog
import com.ppolivka.gitlabprojects.configuration.SettingsState
import com.ppolivka.gitlabprojects.dto.GitlabServer
import com.ppolivka.gitlabprojects.util.GitLabUtil
import git4idea.DialogManager
import git4idea.remote.GitRepositoryHostingService
import java.io.IOException
import java.util.function.Consumer

class GitLabRepositoryHostingService : GitRepositoryHostingService() {
    override fun getServiceDisplayName(): String {
        return "GitLab"
    }

    override fun getRepositoryListLoader(project: Project): RepositoryListLoader {
        return object : RepositoryListLoader {
            private val settingsState: SettingsState = SettingsState.instance

            override fun isEnabled(): Boolean {
                return settingsState.isEnabled
            }

            override fun enable(): Boolean {
                val settingsDialog = SettingsDialog(project)
                DialogManager.show(settingsDialog)
                return isEnabled
            }

            @Throws(RepositoryListLoadingException::class)
            override fun getAvailableRepositories(progressIndicator: ProgressIndicator): List<String> {
                try {
                    val repos: MutableList<String> = ArrayList()
                    GitLabUtil.runInterruptable<Map<GitlabServer, Collection<ProjectDto>>>(
                        progressIndicator
                    ) {
                        try {
                            return@runInterruptable settingsState.loadMapOfServersAndProjects(
                                settingsState.gitlabServers
                            )
                        } catch (throwable: Throwable) {
                            throwable.printStackTrace()
                        }
                        HashMap()
                    }
                        .forEach { (server: GitlabServer, projects: Collection<ProjectDto>) ->
                            if (GitlabServer.CheckoutType.SSH == server.preferredConnection) {
                                projects.forEach(Consumer { project: ProjectDto -> repos.add(project.sshUrl) })
                            } else {
                                projects.forEach(Consumer { project: ProjectDto -> repos.add(project.httpUrl) })
                            }
                        }
                    return repos
                } catch (e: IOException) {
                    return emptyList()
                }
            }
        }
    }
}
