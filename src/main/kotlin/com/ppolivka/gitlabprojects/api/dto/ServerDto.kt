package com.ppolivka.gitlabprojects.api.dto

import java.util.*

class ServerDto {
    var host: String? = null
    var token: String? = null
    var isDefaultRemoveBranch: Boolean = false

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val serverDto = o as ServerDto
        return isDefaultRemoveBranch == serverDto.isDefaultRemoveBranch && host == serverDto.host && token == serverDto.token
    }

    override fun hashCode(): Int {
        return Objects.hash(host, token, isDefaultRemoveBranch)
    }

    override fun toString(): String {
        return host!!
    }
}
