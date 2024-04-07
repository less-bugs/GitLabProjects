package com.ppolivka.gitlabprojects.configuration

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

/**
 * Project specific setting
 *
 * @author ppolivka
 * @since 30.10.2015
 */
@State(
    name = "GitlabProjectsProjectSettings",
    storages = [Storage(file = StoragePathMacros.WORKSPACE_FILE)]
)
class ProjectState : PersistentStateComponent<ProjectState.State?> {
    private var projectState: State? = State()

    override fun getState(): State? {
        return projectState
    }

    override fun loadState(state: State) {
        projectState = state
    }

    class State {
        var lastMergedBranch: String? = null
        var deleteMergedBranch: Boolean? = null
        var mergeAsWorkInProgress: Boolean? = null
        var projectIdMap: MutableMap<Int?, Int?>? = HashMap()
    }

    fun getProjectId(gitRepository: String): Int? {
        if (projectState!!.projectIdMap != null) {
            return projectState!!.projectIdMap!![gitRepository.hashCode()]
        }
        return null
    }

    fun setProjectId(gitRepository: String, projectId: Int?) {
        if (projectState!!.projectIdMap == null) {
            projectState!!.projectIdMap = HashMap()
        }
        projectState!!.projectIdMap!![gitRepository.hashCode()] = projectId
    }

    var lastMergedBranch: String?
        get() = projectState!!.lastMergedBranch
        set(lastMergedBranch) {
            projectState!!.lastMergedBranch = lastMergedBranch
        }

    var deleteMergedBranch: Boolean?
        get() = projectState!!.deleteMergedBranch
        set(deleteMergedBranch) {
            projectState!!.deleteMergedBranch = deleteMergedBranch
        }

    var mergeAsWorkInProgress: Boolean?
        get() = projectState!!.mergeAsWorkInProgress
        set(mergeAsWorkInProgress) {
            projectState!!.mergeAsWorkInProgress = mergeAsWorkInProgress
        }


    companion object {
        fun getInstance(project: Project): ProjectState {
            return ServiceManager.getService(project, ProjectState::class.java)
        }
    }
}
