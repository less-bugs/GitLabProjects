package com.ppolivka.gitlabprojects.exception

/**
 * Exception for actions related to merge requests
 *
 * @author ppolivka
 * @since 31.10.2015
 */
class MergeRequestException : Throwable {
    constructor()

    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(cause: Throwable?) : super(cause)

    constructor(
        message: String?,
        cause: Throwable?,
        enableSuppression: Boolean,
        writableStackTrace: Boolean
    ) : super(message, cause, enableSuppression, writableStackTrace)
}
