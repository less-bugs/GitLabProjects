package com.ppolivka.gitlabprojects.merge.list

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.Convertor
import com.ppolivka.gitlabprojects.comment.CommentsDialog
import com.ppolivka.gitlabprojects.comment.GitLabCommentsListWorker.Companion.create
import com.ppolivka.gitlabprojects.configuration.SettingsState.Companion.instance
import com.ppolivka.gitlabprojects.merge.info.BranchInfo
import com.ppolivka.gitlabprojects.util.GitLabUtil.computeValueInModal
import com.ppolivka.gitlabprojects.util.GitLabUtil.showYesNoDialog
import org.gitlab.api.models.GitlabMergeRequest
import java.awt.event.ActionEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Dialog to accept merge request
 *
 * @author ppolivka
 * @since 22.12.2015
 */
class CodeReviewDialog(
    private val project: Project,
    private val mergeRequest: GitlabMergeRequest,
    private val mergeRequestWorker: GitLabMergeRequestListWorker
) : DialogWrapper(project) {
    private val virtualFile: VirtualFile? = null
    private var sourceBranch: BranchInfo? = null
    private var targetBranch: BranchInfo? = null

    private lateinit var panel: JPanel
    private lateinit var diffButton: JButton
    private lateinit var sourceName: JLabel
    private lateinit var targetName: JLabel
    private lateinit var requestName: JLabel
    private lateinit var commentsButton: JButton
    private lateinit var assigneeName: JLabel
    private lateinit var assignMe: JButton

    private var diffClicked = false

    init {
        init()
    }

    override fun init() {
        super.init()
        title = "Code Review"
        setOKButtonText("Merge")

        sourceName.text = mergeRequest.sourceBranch
        sourceBranch = createBranchInfo(mergeRequest.sourceBranch)

        targetName.text = mergeRequest.targetBranch
        targetBranch = createBranchInfo(mergeRequest.targetBranch)

        requestName.text = mergeRequest.title

        var assignee: String? = ""
        if (mergeRequest.assignee != null) {
            assignee = mergeRequest.assignee.name
        }
        assigneeName.text = assignee

        diffButton.addActionListener { e: ActionEvent? ->
            diffClicked = true
            mergeRequestWorker.diffViewWorker!!.showDiffDialog(sourceBranch!!, targetBranch!!)
        }

        commentsButton.addActionListener { e: ActionEvent? ->
            val commentsListWorker = create(
                project, mergeRequest, virtualFile
            )
            val commentsDialog = CommentsDialog(project, commentsListWorker, virtualFile!!)
            commentsDialog.show()
        }

        assignMe.addActionListener {
            computeValueInModal(
                project,
                "Changing assignee...",
                Convertor<ProgressIndicator?, Void> {
                    try {
                        val settingsState = instance
                        val currentUser =
                            settingsState.api(mergeRequestWorker.gitRepository!!).currentUser
                        settingsState.api(mergeRequestWorker.gitRepository!!).changeAssignee(
                            mergeRequestWorker.gitlabProject!!,
                            mergeRequest,
                            currentUser
                        )
                        assigneeName.text = currentUser.name
                    } catch (e: Exception) {
                        Messages.showErrorDialog(
                            project,
                            "Cannot change assignee of this merge request.",
                            "Cannot Change Assignee"
                        )
                    }
                    null
                })
        }
    }


    private fun createBranchInfo(name: String): BranchInfo {
        return BranchInfo(name, mergeRequestWorker.remoteProjectName!!, true)
    }

    override fun doOKAction() {
        var canContinue = diffClicked
        if (!diffClicked) {
            canContinue = showYesNoDialog(
                project,
                "Merging Without Review",
                "You are about to merge this merge request without looking at code differences. Are you sure?"
            )
        }
        if (canContinue) {
            mergeRequestWorker.mergeBranches(project, mergeRequest)
            super.doOKAction()
        }
    }

    override fun createCenterPanel(): JComponent? {
        return panel
    }
}
