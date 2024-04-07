package com.ppolivka.gitlabprojects.merge.request

import com.intellij.notification.NotificationListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsNotifier
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ThrowableConvertor
import com.intellij.util.containers.Convertor
import com.ppolivka.gitlabprojects.configuration.ProjectState
import com.ppolivka.gitlabprojects.configuration.SettingsState
import com.ppolivka.gitlabprojects.dto.GitlabServer
import com.ppolivka.gitlabprojects.exception.MergeRequestException
import com.ppolivka.gitlabprojects.merge.GitLabDiffViewWorker
import com.ppolivka.gitlabprojects.merge.GitLabMergeRequestWorker
import com.ppolivka.gitlabprojects.merge.info.BranchInfo
import com.ppolivka.gitlabprojects.merge.info.DiffInfo
import com.ppolivka.gitlabprojects.util.GitLabUtil
import com.ppolivka.gitlabprojects.util.MessageUtil
import git4idea.GitLocalBranch
import git4idea.commands.Git
import git4idea.repo.GitRepository
import org.gitlab.api.models.GitlabMergeRequest
import org.gitlab.api.models.GitlabProject
import org.gitlab.api.models.GitlabUser
import java.io.IOException

/**
 * GitLab Create Merge request worker
 *
 * @author ppolivka
 * @since 30.10.2015
 */
class GitLabCreateMergeRequestWorker : GitLabMergeRequestWorker {
    override var git: Git? = null
    override var project: Project? = null
    override   var projectState: ProjectState? =null
    override var gitRepository: GitRepository? = null
    override var remoteUrl: String? = null
    override var gitlabProject: GitlabProject? = null
    override var remoteProjectName: String? = null
    override var diffViewWorker: GitLabDiffViewWorker? = null

    @JvmField
    var gitLocalBranch: GitLocalBranch? = null
    @JvmField
    var localBranchInfo: BranchInfo? = null
    @JvmField
    var branches: List<BranchInfo>? = null
    @JvmField
    var lastUsedBranch: BranchInfo? = null

    //endregion
    @JvmField
    var searchableUsers: SearchableUsers? = null

    fun createMergeRequest(
        branch: BranchInfo,
        assignee: GitlabUser?,
        title: String,
        description: String?,
        removeSourceBranch: Boolean
    ) {
        object : Task.Backgroundable(project, "Creating merge request...") {
            override fun run(indicator: ProgressIndicator) {
                if (title.startsWith("WIP:")) {
                    projectState!!.mergeAsWorkInProgress = true
                } else {
                    projectState!!.mergeAsWorkInProgress = false
                }

                projectState!!.deleteMergedBranch = removeSourceBranch

                indicator.text = "Pushing current branch..."
                val result = git!!.push(
                    gitRepository!!,
                    branch.remoteName,
                    remoteUrl,
                    gitLocalBranch!!.name,
                    true
                )
                if (!result.success()) {
                    MessageUtil.showErrorDialog(
                        project,
                        "Push failed:<br/>" + result.errorOutputAsHtmlString,
                        GitLabMergeRequestWorker.CANNOT_CREATE_MERGE_REQUEST
                    )
                    return
                }

                indicator.text = "Creating merge request..."
                val mergeRequest: GitlabMergeRequest
                try {
                    mergeRequest = SettingsState.instance.api(gitRepository!!).createMergeRequest(
                        gitlabProject!!,
                        assignee,
                        gitLocalBranch!!.name,
                        branch.name,
                        title,
                        description,
                        removeSourceBranch
                    )
                } catch (e: IOException) {
                    MessageUtil.showErrorDialog(
                        project,
                        "Cannot create Merge Request via GitLab REST API",
                        GitLabMergeRequestWorker.CANNOT_CREATE_MERGE_REQUEST
                    )
                    return
                }
                VcsNotifier.getInstance(project)
                    .notifyImportantInfo(
                        title,
                        "<a href='" + generateMergeRequestUrl(
                            SettingsState.instance.currentGitlabServer(gitRepository), mergeRequest
                        ) + "'>Merge request '" + title + "' created</a>",
                        NotificationListener.URL_OPENING_LISTENER
                    )
            }
        }.queue()
    }

    private fun generateMergeRequestUrl(
        server: GitlabServer,
        mergeRequest: GitlabMergeRequest
    ): String {
        val hostText = server.apiUrl
        val helpUrl = StringBuilder()
        helpUrl.append(hostText)
        if (!hostText.endsWith("/")) {
            helpUrl.append("/")
        }
        helpUrl.append(gitlabProject!!.pathWithNamespace)
        helpUrl.append("/merge_requests/")
        helpUrl.append(mergeRequest.iid)
        return helpUrl.toString()
    }

    fun checkAction(branch: BranchInfo?): Boolean {
        if (branch == null) {
            MessageUtil.showWarningDialog(
                project!!,
                "Target branch is not selected",
                GitLabMergeRequestWorker.CANNOT_CREATE_MERGE_REQUEST
            )
            return false
        }

        val info: DiffInfo?
        try {
            info = GitLabUtil
                .computeValueInModal(
                    project!!,
                    "Collecting diff data...",
                    ThrowableConvertor { indicator ->
                        GitLabUtil.runInterruptable(
                            indicator!!
                        ) { diffViewWorker!!.getDiffInfo(localBranchInfo!!, branch) }
                    })
        } catch (e: IOException) {
            MessageUtil.showErrorDialog(
                project!!,
                "Can't collect diff data",
                GitLabMergeRequestWorker.CANNOT_CREATE_MERGE_REQUEST
            )
            return true
        }
        if (info == null) {
            return true
        }

        val localBranchName = "'" + gitLocalBranch!!.name + "'"
        val targetBranchName = "'" + branch.remoteName + "/" + branch.name + "'"
        if (info.info.getBranchToHeadCommits(gitRepository!!).isEmpty()) {
            return GitLabUtil
                .showYesNoDialog(
                    project, "Empty Pull Request",
                    """The branch $localBranchName is fully merged to the branch $targetBranchName
Do you want to proceed anyway?"""
                )
        }
        if (!info.info.getHeadToBranchCommits(gitRepository!!).isEmpty()) {
            return GitLabUtil
                .showYesNoDialog(
                    project, "Target Branch Is Not Fully Merged",
                    """The branch $targetBranchName is not fully merged to the branch $localBranchName
Do you want to proceed anyway?"""
                )
        }

        return true
    }

    companion object {
        private const val CANNOT_SHOW_DIFF_INFO = "Cannot Show Diff Info"

        fun create(project: Project, file: VirtualFile?): GitLabCreateMergeRequestWorker? {
            return GitLabUtil.computeValueInModal(project, "Loading data...", Convertor {
                val mergeRequestWorker = GitLabCreateMergeRequestWorker()
                try {
                    GitLabMergeRequestWorker.Util.fillRequiredInfo(
                        mergeRequestWorker,
                        project,
                        file
                    )
                } catch (e: MergeRequestException) {
                    return@Convertor null
                }

                //region Additional fields
                val currentBranch = mergeRequestWorker.gitRepository!!.currentBranch
                if (currentBranch == null) {
                    MessageUtil.showErrorDialog(
                        project,
                        "No current branch",
                        GitLabMergeRequestWorker.CANNOT_CREATE_MERGE_REQUEST
                    )
                    return@Convertor null
                }
                mergeRequestWorker.gitLocalBranch = currentBranch

                val lastMergedBranch = mergeRequestWorker.projectState!!.lastMergedBranch

                try {
                    val branches = SettingsState.instance
                        .api(project, file)
                        .loadProjectBranches(mergeRequestWorker.gitlabProject!!)
                    val branchInfos: MutableList<BranchInfo> = ArrayList()
                    for (branch in branches) {
                        val branchInfo =
                            BranchInfo(branch.name, mergeRequestWorker.remoteProjectName!!)
                        if (branch.name == lastMergedBranch) {
                            mergeRequestWorker.lastUsedBranch = branchInfo
                        }
                        branchInfos.add(branchInfo)
                    }
                    mergeRequestWorker.branches = branchInfos
                } catch (e: Exception) {
                    MessageUtil.showErrorDialog(
                        project,
                        "Cannot list GitLab branches",
                        GitLabMergeRequestWorker.CANNOT_CREATE_MERGE_REQUEST
                    )
                    return@Convertor null
                }

                mergeRequestWorker.localBranchInfo = BranchInfo(
                    mergeRequestWorker.gitLocalBranch!!.name,
                    mergeRequestWorker.remoteProjectName!!,
                    false
                )
                mergeRequestWorker.searchableUsers =
                    SearchableUsers(project, file!!, mergeRequestWorker.gitlabProject!!)

                //endregion
                mergeRequestWorker
            })
        }
    }
}
