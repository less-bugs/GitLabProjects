package com.ppolivka.gitlabprojects.component

/**
 * Interface for searching actions
 *
 * @author ppolivka
 * @since 1.4.0
 */
interface Searchable<R, T> {
    /**
     * Returns collections of R objects based on T
     * @param toSearch
     * @return
     */
    fun search(toSearch: T): Collection<R>?
}
