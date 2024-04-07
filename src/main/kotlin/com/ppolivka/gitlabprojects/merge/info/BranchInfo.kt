package com.ppolivka.gitlabprojects.merge.info

import java.util.*

/**
 * Class containing info about branch
 *
 * @author ppolivka
 * @since 31.10.2015
 */
class BranchInfo @JvmOverloads constructor(
    var name: String,
    var remoteName: String,
    remoteOnly: Boolean = false
) {
    private var remoteOnly = false

    init {
        this.remoteOnly = remoteOnly
    }

    val fullName: String
        get() = if (remoteOnly) fullRemoteName else name

    val fullRemoteName: String
        get() = this.remoteName + "/" + this.name

    override fun toString(): String {
        return name
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is BranchInfo) {
            return false
        }
        return name == o.name
    }

    override fun hashCode(): Int {
        return Objects.hash(name)
    }
}
