package com.ppolivka.gitlabprojects.api

import com.ppolivka.gitlabprojects.api.dto.NamespaceDto
import org.gitlab.api.AuthMethod
import org.gitlab.api.GitlabAPI
import org.gitlab.api.TokenType
import org.gitlab.api.models.*
import java.io.IOException
import java.net.URLEncoder
import java.util.*
import java.util.stream.Collectors

/**
 * Facade aroud GitLab REST API
 *
 * @author ppolivka
 * @since 9.10.2015
 */
class ApiFacade {
    private lateinit var api: GitlabAPI

    constructor()

    constructor(host: String?, key: String?) {
        reload(host, key)
    }

    fun reload(host: String?, key: String?): Boolean {
        if (host != null && key != null && host.isNotEmpty() && key.isNotEmpty()) {
            api = GitlabAPI.connect(host, key, TokenType.PRIVATE_TOKEN, AuthMethod.URL_PARAMETER)!!
            api.ignoreCertificateErrors(true)
            return true
        }
        return false
    }

    @get:Throws(IOException::class)
    val session: GitlabSession
        get() = api.currentSession

    @Throws(IOException::class)
    private fun checkApi() {
//        if (api == null) {
//            throw IOException("please, configure plugin settings")
//        }
    }

    @get:Throws(IOException::class)
    val namespaces: List<NamespaceDto>
        get() = api.retrieve().getAll("/namespaces", Array<NamespaceDto>::class.java)

    @Throws(IOException::class)
    fun getMergeRequests(project: GitlabProject?): List<GitlabMergeRequest> {
        return api.getOpenMergeRequests(project)
    }

    @Throws(IOException::class)
    fun getMergeRequestComments(mergeRequest: GitlabMergeRequest?): List<GitlabNote> {
        return api.getNotes(mergeRequest)
    }

    @Throws(IOException::class)
    fun addComment(mergeRequest: GitlabMergeRequest?, body: String?) {
        api.createNote(mergeRequest, body)
    }

    @Throws(IOException::class)
    fun createMergeRequest(
        project: GitlabProject,
        assignee: GitlabUser?,
        from: String?,
        to: String?,
        title: String?,
        description: String?,
        removeSourceBranch: Boolean
    ): GitlabMergeRequest {
        val tailUrl = "/projects/" + project.id + "/merge_requests"
        val requestor = api.dispatch()
            .with("source_branch", from)
            .with("target_branch", to)
            .with("title", title)
            .with("description", description)
        if (removeSourceBranch) {
            requestor.with("remove_source_branch", true)
        }
        if (assignee != null) {
            requestor.with("assignee_id", assignee.id)
        }

        return requestor.to(tailUrl, GitlabMergeRequest::class.java)
    }

    @Throws(IOException::class)
    fun acceptMergeRequest(project: GitlabProject?, mergeRequest: GitlabMergeRequest) {
        api.acceptMergeRequest(project, mergeRequest.iid, null)
    }

    @Throws(IOException::class)
    fun changeAssignee(project: GitlabProject, mergeRequest: GitlabMergeRequest, user: GitlabUser) {
        api.updateMergeRequest(
            project.id,
            mergeRequest.iid,
            null,
            user.id,
            null,
            null,
            null,
            null
        )
    }

    @Throws(IOException::class)
    fun createProject(
        name: String?,
        visibilityLevel: String?,
        isPublic: Boolean,
        namespace: NamespaceDto?,
        description: String?
    ): GitlabProject {
        return api.createProject(
            name,
            if (namespace != null && namespace.id != 0) namespace.id else null,
            description,
            null,
            null,
            null,
            null,
            null,
            isPublic,
            visibilityLevel,
            null
        )
    }

    @Throws(IOException::class)
    fun getProject(id: Int?): GitlabProject {
        return api.getProject(id)
    }

    @Throws(IOException::class)
    fun loadProjectBranches(gitlabProject: GitlabProject?): List<GitlabBranch> {
        return api.getBranches(gitlabProject)
    }

    @get:Throws(Throwable::class)
    val projects: Collection<GitlabProject>
        get() {
            checkApi()

            val result: SortedSet<GitlabProject> = TreeSet { o1, o2 ->
                val namespace1 = o1.namespace
                val n1 = namespace1?.name?.lowercase(Locale.getDefault()) ?: "Default"
                val namespace2 = o2.namespace
                val n2 = namespace2?.name?.lowercase(Locale.getDefault()) ?: "Default"

                val compareNamespace = n1.compareTo(n2)
                if (compareNamespace != 0) compareNamespace else o1.name.lowercase(Locale.getDefault())
                    .compareTo(
                        o2.name.lowercase(
                            Locale.getDefault()
                        )
                    )
            }
            var projects = try {
                api.membershipProjects
            } catch (e: Throwable) {
                emptyList()
            }
            projects = projects.stream()
                .filter { project: GitlabProject -> java.lang.Boolean.TRUE != project.isArchived }
                .collect(Collectors.toList())
            result.addAll(projects)

            return result
        }

    @Throws(IOException::class)
    fun searchUsers(project: GitlabProject, text: String?): Collection<GitlabUser?> {
        checkApi()
        var users: List<GitlabUser?> = ArrayList()
        if (text != null) {
            val tailUrl =
                GitlabProject.URL + "/" + project.id + "/users" + "?search=" + URLEncoder.encode(
                    text,
                    "UTF-8"
                )
            val response = api.retrieve().to(tailUrl, Array<GitlabUser>::class.java)
            users = Arrays.asList(*response)
        }
        return users
    }

    @get:Throws(IOException::class)
    val currentUser: GitlabUser
        get() {
            checkApi()
            return api.user
        }
}
