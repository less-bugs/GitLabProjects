package com.ppolivka.gitlabprojects.merge

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Couple
import com.intellij.openapi.vcs.VcsException
import com.intellij.util.ThrowableConvertor
import com.ppolivka.gitlabprojects.merge.info.BranchInfo
import com.ppolivka.gitlabprojects.merge.info.DiffInfo
import com.ppolivka.gitlabprojects.util.GitLabUtil
import com.ppolivka.gitlabprojects.util.MessageUtil
import git4idea.changes.GitChangeUtils
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository
import git4idea.ui.branch.GitCompareBranchesDialog
import git4idea.update.GitFetcher
import git4idea.util.GitCommitCompareInfo
import org.jetbrains.ide.PooledThreadExecutor
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

/**
 * Worker class that helps to calculate diff between two branches
 *
 * @author ppolivka
 * @since 31.10.2015
 */
class GitLabDiffViewWorker internal constructor(
    private val project: Project,
    private val gitRepository: GitRepository
) {
    fun showDiffDialog(from: BranchInfo, branch: BranchInfo) {
        val info: DiffInfo?
        try {
            info = GitLabUtil
                .computeValueInModal(
                    project,
                    "Collecting diff data...",
                    ThrowableConvertor { indicator ->
                        GitLabUtil.runInterruptable(
                            indicator!!
                        ) { getDiffInfo(from, branch) }
                    })
        } catch (e: IOException) {
            MessageUtil.showErrorDialog(project, "Can't collect diff data", CANNOT_SHOW_DIFF_INFO)
            return
        }
        if (info == null) {
            MessageUtil.showErrorDialog(project, "Can't collect diff data", CANNOT_SHOW_DIFF_INFO)
            return
        }

        val dialog =
            GitCompareBranchesDialog(project, info.to, info.from, info.info, gitRepository, true)
        dialog.show()
    }

    @Throws(IOException::class)
    fun getDiffInfo(from: BranchInfo, branch: BranchInfo): DiffInfo? {
        if (branch.name == null) {
            return null
        }

        try {
            return launchLoadDiffInfo(from, branch).get()
        } catch (e: InterruptedException) {
            throw IOException(e)
        } catch (e: ExecutionException) {
            val wrapEx = e.cause
            if (wrapEx!!.cause is VcsException) {
                throw IOException(wrapEx.cause)
            }
            return null
        }
    }

    fun launchLoadDiffInfo(from: BranchInfo, branch: BranchInfo): CompletableFuture<DiffInfo?> {
        if (branch.name == null) {
            return CompletableFuture.completedFuture(null)
        }

        val fetchFuture = launchFetchRemote(branch)
        return fetchFuture.thenApply { t: Boolean? ->
            try {
                return@thenApply doLoadDiffInfo(from, branch)
            } catch (e: VcsException) {
                throw RuntimeException(e)
            }
        }
    }

    private fun launchFetchRemote(branch: BranchInfo): CompletableFuture<Boolean> {
        if (branch.name == null) {
            return CompletableFuture.completedFuture(false)
        }

        return CompletableFuture.supplyAsync(
            { doFetchRemote(branch) },
            PooledThreadExecutor.INSTANCE
        )
    }

    private fun doFetchRemote(branch: BranchInfo): Boolean {
        if (branch.name == null) {
            return false
        }

        val result =
            GitFetcher(project, EmptyProgressIndicator(), false).fetch(
                gitRepository.root, branch.remoteName, null
            )
        if (!result.isSuccess) {
            GitFetcher.displayFetchResult(project, result, null, result.errors)
            return false
        }
        return true
    }

    @Throws(VcsException::class)
    private fun doLoadDiffInfo(from: BranchInfo, to: BranchInfo): DiffInfo {
        val currentBranch = from.fullName
        val targetBranch = to.fullRemoteName

        val commits1 = GitHistoryUtils.history(project, gitRepository.root, "..$targetBranch")
        val commits2 = GitHistoryUtils.history(project, gitRepository.root, "$targetBranch..")
        val diff = GitChangeUtils.getDiff(
            project, gitRepository.root, targetBranch, currentBranch, null
        )
        val info = GitCommitCompareInfo(GitCommitCompareInfo.InfoType.BRANCH_TO_HEAD)
        info.put(gitRepository, diff)
        info.put(gitRepository, Couple.of(commits1, commits2))

        return DiffInfo(info, currentBranch, targetBranch)
    }

    companion object {
        private const val CANNOT_SHOW_DIFF_INFO = "Cannot Show Diff Info"
    }
}
