package com.ppolivka.gitlabprojects.exception

import org.jetbrains.annotations.NonNls


class GitLabException : RuntimeException {
    constructor()

    constructor(message: @NonNls String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(cause: Throwable?) : super(cause)

    constructor(
        message: String?,
        cause: Throwable?,
        enableSuppression: Boolean,
        writableStackTrace: Boolean
    ) : super(message, cause, enableSuppression, writableStackTrace)
}
