package com.ppolivka.gitlabprojects.configuration

import java.net.URI

object ApiToRepoUrlConverter {
    fun convertApiUrlToRepoUrl(apiUrl: String): String {
        val uri = URI(apiUrl)
        val domain = uri.host
        return if (domain.startsWith("www.")) domain.substring(4) else domain
    }
}
