package com.ppolivka.gitlabprojects.comment

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.Convertor
import com.ppolivka.gitlabprojects.configuration.SettingsState
import com.ppolivka.gitlabprojects.util.GitLabUtil
import com.ppolivka.gitlabprojects.util.MessageUtil
import org.apache.commons.lang3.StringUtils
import org.gitlab.api.models.GitlabMergeRequest
import java.io.IOException
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea

/**
 * Dialog for adding comments
 *
 * @author ppolivka
 * @since 1.3.2
 */
class AddCommentDialog(
    private val project: Project?,
    private val mergeRequest: GitlabMergeRequest,
    file: VirtualFile?
) : DialogWrapper(
    project
) {
    private var panel: JPanel? = null
    private var commentText: JTextArea? = null

    private var file: VirtualFile? = null

    init {
        init()
    }

    override fun init() {
        super.init()
        title = "Add Comment"
        setOKButtonText("Add")
    }

    override fun doOKAction() {
        super.doOKAction()
        GitLabUtil.computeValueInModal(
            project!!,
            "Adding comment...",
            Convertor<ProgressIndicator?, Void?> { indicator: ProgressIndicator? ->
                val comment = commentText!!.text
                if (StringUtils.isNotBlank(comment)) {
                    try {
                        SettingsState.instance.api(project, file).addComment(mergeRequest, comment)
                    } catch (e: IOException) {
                        MessageUtil.showErrorDialog(
                            project,
                            "Cannot add comment.",
                            "Cannot Add Comment"
                        )
                    }
                }
                null
            })
    }

    override fun doValidate(): ValidationInfo? {
        if (StringUtils.isBlank(commentText!!.text)) {
            return ValidationInfo("Comment text cannot be empty.", commentText)
        }
        return super.doValidate()
    }

    override fun createCenterPanel(): JComponent? {
        return panel
    }

}
