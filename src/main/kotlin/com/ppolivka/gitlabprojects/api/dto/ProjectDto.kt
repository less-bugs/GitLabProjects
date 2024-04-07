package com.ppolivka.gitlabprojects.api.dto

import java.io.Serializable

/**
 * DTO Class representing one GitLab Project
 *
 * @author ppolivka
 * @since 10.10.2015
 */
class ProjectDto : Serializable {
    lateinit var name: String
    lateinit var namespace: String
    lateinit var sshUrl: String
    lateinit var httpUrl: String
}
