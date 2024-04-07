package com.ppolivka.gitlabprojects.util

import com.intellij.concurrency.JobScheduler
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ThrowableConvertor
import com.intellij.util.containers.Convertor
import com.ppolivka.gitlabprojects.configuration.SettingsState
import git4idea.GitUtil
import git4idea.commands.GitCommand
import git4idea.commands.GitSimpleHandler
import git4idea.config.GitExecutableManager
import git4idea.config.GitVersion
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import org.apache.commons.lang3.StringUtils
import java.io.IOException
import java.net.URI
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * GitLab specific untils
 *
 * @author ppolivka
 * @since 28.10.2015
 */
object GitLabUtil {

    fun getGitRepository(project: Project, file: VirtualFile?): GitRepository? {
        val manager = GitUtil.getRepositoryManager(project)
        val repositories = manager.repositories
        if (repositories.size == 0) {
            return null
        }
        if (repositories.size == 1) {
            return repositories[0]
        }
        if (file != null) {
            val repository = manager.getRepositoryForFile(file)
            if (repository != null) {
                return repository
            }
        }
        return manager.getRepositoryForFile(project.baseDir)
    }

    fun findGitLabRemoteUrl(repository: GitRepository): String? {
        val remote = findGitLabRemote(repository) ?: return null
        return remote.getSecond()
    }

    fun findGitLabRemote(repository: GitRepository): Pair<GitRemote, String>? {
        for (gitRemote in repository.remotes) {
            for (remoteUrl in gitRemote.urls) {
                if (remoteUrl.contains(
                        SettingsState.instance.currentGitlabServer(repository).repositoryUrl
                    )
                ) {
                    return Pair.create(gitRemote, gitRemote.name)
                }
            }
        }
        return null
    }


    fun isGitLabUrl(testUrl: String?, url: String?): Boolean {
        try {
            val fromSettings = URI(testUrl)
            val fromSettingsHost = fromSettings.host

            val patternString =
                "(\\w+://)(.+@)*([\\w\\d\\.\\-]+)(:[\\d]+){0,1}/*(.*)|(.+@)*([\\w\\d\\.\\-]+):(.*)"
            val pattern = Pattern.compile(patternString)
            val matcher = pattern.matcher(url)
            var fromUrlHost = ""
            if (matcher.matches()) {
                val group3 = matcher.group(3)
                val group7 = matcher.group(7)
                if (StringUtils.isNotEmpty(group3)) {
                    fromUrlHost = group3
                } else if (StringUtils.isNotEmpty(group7)) {
                    fromUrlHost = group7
                }
            }
            return fromSettingsHost != null && removeNotAlpha(fromSettingsHost) == removeNotAlpha(
                fromUrlHost
            )
        } catch (e: Exception) {
            return false
        }
    }

    fun removeNotAlpha(input: String): String {
        var input = input
        input = input.replace("[^a-zA-Z0-9]".toRegex(), "")
        input = input.lowercase(Locale.getDefault())
        return input
    }

    fun addGitLabRemote(
        project: Project,
        repository: GitRepository,
        remote: String,
        url: String
    ): Boolean {
        val handler = GitSimpleHandler(project, repository.root, GitCommand.REMOTE)
        handler.setSilent(true)

        try {
            handler.addParameters("add", remote, url)
            handler.run()
            if (handler.exitCode != 0) {
                MessageUtil.showErrorDialog(
                    project,
                    "New remote origin cannot be added to this project.",
                    "Cannot Add New Remote"
                )
                return false
            }
            // catch newly added remote
            repository.update()
            return true
        } catch (e: VcsException) {
            MessageUtil.showErrorDialog(
                project,
                "New remote origin cannot be added to this project.",
                "Cannot Add New Remote"
            )
            return false
        }
    }

    fun testGitExecutable(project: Project): Boolean {
        val manager = GitExecutableManager.getInstance()
        val executable = manager.pathToGit
        val version: GitVersion
        try {
            version = manager.identifyVersion(executable)
        } catch (e: Exception) {
            MessageUtil.showErrorDialog(project, "Cannot find git executable.", "Cannot Find Git")
            return false
        }

        if (!version.isSupported) {
            MessageUtil.showErrorDialog(
                project,
                "Your version of git is not supported.",
                "Cannot Find Git"
            )
            return false
        }
        return true
    }

    @Throws(IOException::class)
    fun <T> computeValueInModal(
        project: Project,
        caption: String,
        task: ThrowableConvertor<ProgressIndicator?, T, IOException?>
    ): T {
        val dataRef = Ref<T>()
        val exceptionRef = Ref<Throwable>()
        ProgressManager.getInstance().run(object : Task.Modal(project, caption, true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    dataRef.set(task.convert(indicator))
                } catch (e: Throwable) {
                    exceptionRef.set(e)
                }
            }
        })
        if (!exceptionRef.isNull) {
            val e = exceptionRef.get()
            if (e is IOException) {
                throw (e)
            }
            if (e is RuntimeException) {
                throw (e)
            }
            if (e is Error) {
                throw (e)
            }
            throw RuntimeException(e)
        }
        return dataRef.get()
    }

    fun <T> computeValueInModal(
        project: Project,
        caption: String,
        task: Convertor<ProgressIndicator?, T>
    ): T {
        return computeValueInModal(project, caption, true, task)
    }

    fun <T> computeValueInModal(
        project: Project,
        caption: String,
        canBeCancelled: Boolean,
        task: Convertor<ProgressIndicator?, T>
    ): T {
        val dataRef = Ref<T>()
        val exceptionRef = Ref<Throwable>()
        ProgressManager.getInstance().run(object : Task.Modal(project, caption, canBeCancelled) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    dataRef.set(task.convert(indicator))
                } catch (e: Throwable) {
                    exceptionRef.set(e)
                }
            }
        })
        if (!exceptionRef.isNull) {
            val e = exceptionRef.get()
            if (e is RuntimeException) {
                throw (e)
            }
            if (e is Error) {
                throw (e)
            }
            throw RuntimeException(e)
        }
        return dataRef.get()
    }

    @Throws(IOException::class)
    fun <T> runInterruptable(
        indicator: ProgressIndicator,
        task: ThrowableComputable<T, IOException?>
    ): T {
        var future: ScheduledFuture<*>? = null
        try {
            val thread = Thread.currentThread()
            future = addCancellationListener(indicator, thread)

            return task.compute()
        } finally {
            future?.cancel(true)
            Thread.interrupted()
        }
    }

    private fun addCancellationListener(
        indicator: ProgressIndicator,
        thread: Thread
    ): ScheduledFuture<*> {
        return addCancellationListener {
            if (indicator.isCanceled) {
                thread.interrupt()
            }
        }
    }

    private fun addCancellationListener(run: Runnable): ScheduledFuture<*> {
        return JobScheduler.getScheduler()
            .scheduleWithFixedDelay(run, 1000, 300, TimeUnit.MILLISECONDS)
    }

    @Messages.YesNoResult
    fun showYesNoDialog(project: Project?, title: String, message: String): Boolean {
        return Messages.YES == Messages.showYesNoDialog(
            project,
            message,
            title,
            Messages.getQuestionIcon()
        )
    }
}
