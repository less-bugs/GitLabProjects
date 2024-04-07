package com.ppolivka.gitlabprojects.merge.request

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.ppolivka.gitlabprojects.component.Searchable
import com.ppolivka.gitlabprojects.configuration.SettingsState
import com.ppolivka.gitlabprojects.util.MessageUtil
import org.gitlab.api.models.GitlabProject
import org.gitlab.api.models.GitlabUser
import java.io.IOException
import java.util.stream.Collectors

/**
 * Searchable users model
 *
 * @author ppolivka
 * @since 1.4.0
 */
class SearchableUsers(
    private val project: Project,
    private val file: VirtualFile,
    private val gitlabProject: GitlabProject
) : Searchable<SearchableUser?, String?> {
    @JvmField
    var initialModel: List<SearchableUser>

    init {
        this.initialModel = search("")
    }

    override fun search(toSearch: String?): List<SearchableUser> {
        try {
            val users = SettingsState.instance.api(
                project, file
            ).searchUsers(gitlabProject, toSearch).stream().map { gitLabUser: GitlabUser? ->
                SearchableUser(
                    gitLabUser!!
                )
            }.collect(Collectors.toList())
            val resultingUsers: MutableList<SearchableUser> = ArrayList()
            resultingUsers.addAll(users)
            return resultingUsers
        } catch (e: IOException) {
            MessageUtil.showErrorDialog(
                project,
                "New remote origin cannot be added to this project.",
                "Cannot Add New Remote"
            )
        }
        return emptyList()
    }

}
