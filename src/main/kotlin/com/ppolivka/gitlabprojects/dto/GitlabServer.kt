package com.ppolivka.gitlabprojects.dto

class GitlabServer {
    var apiUrl = ""
    var apiToken = ""
    var repositoryUrl = ""
    var preferredConnection = CheckoutType.SSH
    var removeSourceBranch = true

    override fun toString(): String {
        return apiUrl
    }

    enum class CheckoutType {
        SSH,
        HTTPS
    }

    companion object{
        val EMPTY = GitlabServer()
    }
}
