package com.ppolivka.gitlabprojects.exception

/**
 * Exception for actions related to merge requests
 *
 * @author ppolivka
 * @since 31.10.2015
 */
class MergeRequestException : GitLabException {
    constructor() : super("", null)

    constructor(cause: Throwable?) : super("MergeRequest", cause)
}
