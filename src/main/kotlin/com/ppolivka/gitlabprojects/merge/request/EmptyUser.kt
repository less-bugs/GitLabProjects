package com.ppolivka.gitlabprojects.merge.request

import org.gitlab.api.models.GitlabUser

/**
 * Empty Searchable User implementation
 * User for query implementation and various other pplaceholder
 *
 * @author ppolivka
 * @since 1.4.0
 */
class EmptyUser(private val user: String) : SearchableUser(GitlabUser()) {
    override fun toString(): String {
        return user
    }
}
