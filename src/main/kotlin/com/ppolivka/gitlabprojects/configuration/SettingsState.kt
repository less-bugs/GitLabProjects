package com.ppolivka.gitlabprojects.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.xmlb.XmlSerializerUtil
import com.ppolivka.gitlabprojects.api.ApiFacade
import com.ppolivka.gitlabprojects.api.dto.ProjectDto
import com.ppolivka.gitlabprojects.dto.GitlabServer
import com.ppolivka.gitlabprojects.util.GitLabUtil
import git4idea.repo.GitRepository

/**
 * Settings State for GitLab Projects plugin
 *
 * @author ppolivka
 * @since 9.10.2015
 */
@State(
    name = "SettingsState",
    storages = [Storage("\$APP_CONFIG$/gitlab-project-settings-new-format.xml")]
)
class SettingsState : PersistentStateComponent<SettingsState?> {
    //region Getters & Setters
    var host: String? = null

    var token: String? = null

    var isDefaultRemoveBranch: Boolean = false

    var projects: MutableCollection<ProjectDto> = ArrayList()

    var gitlabServers: MutableList<GitlabServer> = mutableListOf()

    override fun getState(): SettingsState {
        return this
    }

    override fun loadState(settingsState: SettingsState) {
        XmlSerializerUtil.copyBean(settingsState, this)
    }

    fun isApiValid(project: Project, file: VirtualFile?) {
        api(project, file).session
    }

    fun isApiValid(host: String?, key: String?) {
        val apiFacade = ApiFacade()
        apiFacade.reload(host, key)
        apiFacade.session
    }

    fun reloadProjects(servers: Collection<GitlabServer>) {
        projects = ArrayList()
        for (server in servers) {
            reloadProjects(server)
        }
    }

    fun loadMapOfServersAndProjects(servers: Collection<GitlabServer>): Map<GitlabServer, Collection<ProjectDto>> {
        val map: MutableMap<GitlabServer, Collection<ProjectDto>> = HashMap()
        for (server in servers) {
            val projects: Collection<ProjectDto> = loadProjects(server)
            map[server] = projects
        }
        return map
    }

    fun reloadProjects(server: GitlabServer) {
        this.projects = loadProjects(server)
    }

    fun loadProjects(server: GitlabServer): MutableCollection<ProjectDto> {
        val apiFacade = api(server)

        val projects = projects

        for (gitlabProject in apiFacade.projects) {
            val projectDto = ProjectDto()
            projectDto.name = gitlabProject.name
            projectDto.namespace = gitlabProject.namespace.name
            projectDto.httpUrl = gitlabProject.httpUrl
            projectDto.sshUrl = gitlabProject.sshUrl
            projects.add(projectDto)
        }
        this.projects = projects
        return projects
    }

    fun api(project: Project, file: VirtualFile?): ApiFacade {
        return api(currentGitlabServer(project, file))
    }

    fun api(gitRepository: GitRepository): ApiFacade {
        return api(currentGitlabServer(gitRepository))
    }

    fun api(serverDto: GitlabServer): ApiFacade {
        return ApiFacade(serverDto.apiUrl, serverDto.apiToken)
    }

    fun addServer(server: GitlabServer) {
        if (gitlabServers.stream().noneMatch { server1: GitlabServer ->
                server.apiUrl == server1.apiUrl
            }) {
            gitlabServers.add(server)
        } else {
            gitlabServers.stream()
                .filter { server1: GitlabServer -> server.apiUrl == server1.apiUrl }
                .forEach { changedServer: GitlabServer ->
                    changedServer.apiUrl = server.apiUrl
                    changedServer.repositoryUrl = server.repositoryUrl
                    changedServer.apiToken = server.apiToken
                    changedServer.preferredConnection = server.preferredConnection
                    changedServer.removeSourceBranch = server.removeSourceBranch
                }
        }
    }

    fun deleteServer(server: GitlabServer) {
        gitlabServers.stream()
            .filter { server1: GitlabServer -> server.apiUrl == server1.apiUrl }
            .forEach { removedServer: GitlabServer -> gitlabServers.remove(removedServer) }
    }

    private fun currentGitlabServer(project: Project, file: VirtualFile?): GitlabServer {
        val gitRepository = GitLabUtil.getGitRepository(project, file)
        return currentGitlabServer(gitRepository)
    }

    fun currentGitlabServer(gitRepository: GitRepository?): GitlabServer {
        for (gitRemote in gitRepository!!.remotes) {
            for (remoteUrl in gitRemote.urls) {
                for (server in gitlabServers) {
                    if (remoteUrl.contains(server.repositoryUrl)) return server
                }
            }
        }
        throw NullPointerException()
    }

    val isEnabled: Boolean
        get() = gitlabServers.isNotEmpty() //endregion

    companion object {
        val instance: SettingsState
            get() = ApplicationManager.getApplication().getService(SettingsState::class.java)
    }
}
