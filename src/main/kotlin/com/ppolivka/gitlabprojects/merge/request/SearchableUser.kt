package com.ppolivka.gitlabprojects.merge.request

import org.gitlab.api.models.GitlabUser

/**
 * One user returned from Search, used by combo box model
 *
 * @author ppolivka
 * @since 1.4.0
 */
open class SearchableUser(val gitLabUser: GitlabUser) {
    override fun toString(): String {
        return gitLabUser.name
    }
}
