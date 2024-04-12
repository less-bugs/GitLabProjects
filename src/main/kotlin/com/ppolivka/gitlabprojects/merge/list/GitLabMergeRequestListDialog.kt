package com.ppolivka.gitlabprojects.merge.list

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.ppolivka.gitlabprojects.common.ReadOnlyTableModel
import org.gitlab.api.models.GitlabMergeRequest
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.TableModel

/**
 * Dialog that is listing all active merge request in git lab repo
 *
 * @author ppolivka
 * @since 31.10.2015
 */
class GitLabMergeRequestListDialog(
    private val project: Project,
    private val mergeRequestListWorker: GitLabMergeRequestListWorker
) : DialogWrapper(
    project
) {
    private lateinit var mainView: JPanel
    private lateinit var listOfRequests: JTable

    init {
        init()
    }

    override fun init() {
        super.init()
        title = "List of Merge Requests"

        isOKActionEnabled = false
        setOKButtonText("Code Review")
        horizontalStretch = 2f

        listOfRequests.model = mergeRequestModel(mergeRequestListWorker.mergeRequests)
        listOfRequests.columnModel.getColumn(0).preferredWidth = 200
        listOfRequests.columnModel.getColumn(5).width = 0
        listOfRequests.columnModel.getColumn(5).minWidth = 0
        listOfRequests.columnModel.getColumn(5).maxWidth = 0
        listOfRequests.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

        listOfRequests.selectionModel.addListSelectionListener {
            isOKActionEnabled = true
        }
    }

    override fun doOKAction() {
        val mergeRequest =
            listOfRequests.getValueAt(listOfRequests.selectedRow, 5) as GitlabMergeRequest
        val codeReviewDialog = CodeReviewDialog(project, mergeRequest, mergeRequestListWorker)
        codeReviewDialog.show()
        if (codeReviewDialog.isOK) {
            super.doOKAction()
        }
    }

    private fun mergeRequestModel(mergeRequests: List<GitlabMergeRequest>): TableModel {
        val columnNames = arrayOf("Merge request", "Author", "Source", "Target", "Assignee", "")
        val data = Array(
            mergeRequests.size
        ) { arrayOfNulls<Any>(columnNames.size) }
        for ((i, mergeRequest) in mergeRequests.withIndex()) {
            val row = arrayOfNulls<Any>(columnNames.size)
            row[0] = mergeRequest.title
            row[1] = mergeRequest.author.name
            row[2] = mergeRequest.sourceBranch
            row[3] = mergeRequest.targetBranch
            row[4] = mergeRequest.assignee?.name ?: ""
            row[5] = mergeRequest
            data[i] = row
        }
        return ReadOnlyTableModel(data, columnNames)
    }

    override fun createCenterPanel(): JComponent {
        return mainView
    }
}
