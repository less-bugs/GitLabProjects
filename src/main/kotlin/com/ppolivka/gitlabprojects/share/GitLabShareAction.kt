package com.ppolivka.gitlabprojects.share

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.ppolivka.gitlabprojects.api.dto.NamespaceDto
import com.ppolivka.gitlabprojects.common.GitLabIcons
import com.ppolivka.gitlabprojects.common.NoGitLabApiAction
import com.ppolivka.gitlabprojects.configuration.SettingsState
import com.ppolivka.gitlabprojects.dto.GitlabServer
import com.ppolivka.gitlabprojects.util.GitLabUtil
import com.ppolivka.gitlabprojects.util.MessageUtil
import git4idea.GitUtil
import git4idea.actions.GitInit
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.commands.GitSimpleHandler
import git4idea.repo.GitRepository
import git4idea.util.GitFileUtils
import git4idea.util.GitUIUtil
import org.gitlab.api.models.GitlabProject
import java.io.IOException

/**
 * Import to VCS project to gitlab
 *
 * @author ppolivka
 * @since 28.10.2015
 */
class GitLabShareAction : NoGitLabApiAction(
    "Share Project on GitLab...",
    "Easy share on your GitLab server",
    GitLabIcons.gitLabIcon
) {
    override fun apiValidAction(anActionEvent: AnActionEvent) {
        if (project.isDisposed) {
            return
        }

        shareProjectOnGitLab(project, file)
    }

    private fun shareProjectOnGitLab(project: Project, file: VirtualFile?) {
        FileDocumentManager.getInstance().saveAllDocuments()

        // get gitRepository
        val gitRepository = GitLabUtil.getGitRepository(project, file)
        val gitDetected = gitRepository != null
        val root = if (gitDetected) gitRepository!!.root else project.baseDir

        if (gitDetected) {
            val gitLabRemoteUrl = GitLabUtil.findGitLabRemoteUrl(gitRepository!!)
            if (gitLabRemoteUrl != null) {
                MessageUtil.showInfoMessage(
                    project,
                    "This project already has remote to your GitLab Server",
                    "Already Git Lab Project"
                )
            }
        }
        val gitLabShareDialog = GitLabShareDialog(project)
        gitLabShareDialog.show()
        if (!gitLabShareDialog.isOK) {
            return
        }
        val name = gitLabShareDialog.projectName.text
        val commitMessage = gitLabShareDialog.commitMessage.text
        val namespace = gitLabShareDialog.groupList.selectedItem as NamespaceDto
        var visibility_level = "internal"
        var isPublic = false
        if (gitLabShareDialog.isPrivate.isSelected) {
            visibility_level = "private"
        }
        if (gitLabShareDialog.isPublic.isSelected) {
            visibility_level = "public"
            isPublic = true
        }
        val visibility = visibility_level
        val publicity = isPublic

        var isSsh = true
        if (gitLabShareDialog.isHTTPAuth.isSelected) {
            isSsh = false
        }
        val authSsh = isSsh

        ProgressManager.getInstance()
            .run(object : Task.Backgroundable(project, "Sharing to GitLab...") {
                override fun run(indicator: ProgressIndicator) {
                    val gitlabProject: GitlabProject
                    try {
                        indicator.text = "Creating GitLab Repository"
                        gitlabProject = SettingsState.instance
                            .api(gitLabShareDialog.serverList.selectedItem as GitlabServer)
                            .createProject(name, visibility, publicity, namespace, "")
                    } catch (e: IOException) {
                        return
                    }

                    if (!gitDetected) {
                        indicator.text = "Creating empty git repo..."
                        if (!createEmptyGitRepository(project, root)) {
                            return
                        }
                    }

                    val repositoryManager = GitUtil.getRepositoryManager(project)
                    val repository = repositoryManager.getRepositoryForRoot(root)
                    if (repository == null) {
                        MessageUtil.showErrorDialog(
                            project,
                            "Remote server was not found.",
                            "Remote Not Found"
                        )
                        return
                    }

                    val remoteUrl = if (authSsh) gitlabProject.sshUrl else gitlabProject.httpUrl

                    indicator.text = "Adding GitLAb as a remote host..."
                    if (!GitLabUtil.addGitLabRemote(project, repository, name, remoteUrl)) {
                        return
                    }

                    if (!performFirstCommitIfRequired(
                            project,
                            root,
                            repository,
                            indicator,
                            remoteUrl,
                            commitMessage
                        )
                    ) {
                        return
                    }

                    indicator.text = "Pushing to gitlab master..."
                    if (!pushCurrentBranch(project, repository, name, remoteUrl)) {
                        return
                    }

                    MessageUtil.showInfoMessage(
                        project,
                        "Project was shared to your GitLab server",
                        "Project Shared"
                    )
                }
            })
    }

    companion object {

        private fun performFirstCommitIfRequired(
            project: Project,
            root: VirtualFile,
            repository: GitRepository,
            indicator: ProgressIndicator,
            url: String,
            commitMessage: String
        ): Boolean {
            // check if there is no commits
            if (!repository.isFresh) {
                return true
            }

            try {
                indicator.text = "Adding files to git..."

                // ask for files to add
                val trackedFiles = ChangeListManager.getInstance(project).affectedFiles
                val untrackedFiles =
                    filterOutIgnored(
                        project,
                        repository.untrackedFilesHolder.retrieveUntrackedFiles()
                    )
                untrackedFiles.removeAll(trackedFiles)

                GitFileUtils.addFiles(project, root, untrackedFiles)

                indicator.text = "Performing commit..."
                val handler = GitSimpleHandler(project, root, GitCommand.COMMIT)
                handler.setStdoutSuppressed(false)
                handler.addParameters("-m", commitMessage)
                handler.endOptions()
                handler.run()
            } catch (e: VcsException) {
                MessageUtil.showErrorDialog(
                    project,
                    "Project was create on GitLab server, but files cannot be commited to it.",
                    "Initial Commit Failure"
                )
                return false
            }
            return true
        }

        private fun filterOutIgnored(
            project: Project,
            files: Collection<VirtualFile>
        ): MutableCollection<VirtualFile> {
            val changeListManager = ChangeListManager.getInstance(project)
            val vcsManager = ProjectLevelVcsManager.getInstance(project)
            return ContainerUtil.filter(files) { file ->
                !changeListManager.isIgnoredFile(
                    file!!
                ) && !vcsManager.isIgnored(file)
            }
        }

        private fun createEmptyGitRepository(
            project: Project,
            root: VirtualFile
        ): Boolean {
            val h = GitLineHandler(project, root, GitCommand.INIT)
            h.setStdoutSuppressed(false)
            val commandResult = Git.getInstance().runCommand(h)
            if (!commandResult.success()) {
                GitUIUtil.showOperationError(
                    project,
                    "git init",
                    commandResult.outputAsJoinedString
                )
                return false
            }
            GitInit.refreshAndConfigureVcsMappings(project, root, root.path)
            return true
        }

        private fun pushCurrentBranch(
            project: Project,
            repository: GitRepository,
            remoteName: String,
            remoteUrl: String
        ): Boolean {
            val git = Git.getInstance()

            val currentBranch = repository.currentBranch
            if (currentBranch == null) {
                MessageUtil.showErrorDialog(
                    project,
                    "Project was create on GitLAb server, but cannot be pushed.",
                    "Cannot Be Pushed"
                )
                return false
            }
            val result = git.push(repository, remoteName, remoteUrl, currentBranch.name, true)
            if (!result.success()) {
                MessageUtil.showErrorDialog(
                    project,
                    "Project was create on GitLab server, but cannot be pushed.",
                    "Cannot Be Pushed"
                )
                return false
            }
            return true
        }
    }
}
