package com.ppolivka.gitlabprojects.configuration

import lombok.SneakyThrows
import java.net.URI

object ApiToRepoUrlConverter {
    @SneakyThrows
    fun convertApiUrlToRepoUrl(apiUrl: String?): String {
        val uri = URI(apiUrl)
        val domain = uri.host
        return if (domain.startsWith("www.")) domain.substring(4) else domain
    }
}
