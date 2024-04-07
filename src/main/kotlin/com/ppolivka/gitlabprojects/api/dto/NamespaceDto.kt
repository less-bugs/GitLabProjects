package com.ppolivka.gitlabprojects.api.dto

/**
 * Dto Class Representing one namespace
 *
 * @author ppolivka
 * @since 28.10.2015
 */
open class NamespaceDto {
    var id: Int = 0
    var path: String? = null
    var kind: String? = null

    override fun toString(): String {
        return path!!
    }
}
