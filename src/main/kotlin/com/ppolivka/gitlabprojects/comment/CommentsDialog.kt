package com.ppolivka.gitlabprojects.comment

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.ppolivka.gitlabprojects.common.ReadOnlyTableModel
import git4idea.DialogManager
import org.gitlab.api.models.GitlabNote
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.table.TableModel

/**
 * Dialog for listing comments
 *
 * @author ppolivka
 * @since 1.3.2
 */
class CommentsDialog(
    private val project: Project?,
    private var worker: GitLabCommentsListWorker,
    private val file: VirtualFile
) : DialogWrapper(
    project
) {
    private lateinit var panel: JPanel
    private lateinit var comments: JTable
    private lateinit var addCommentButton: JButton

    init {
        init()
    }

    override fun init() {
        super.init()

        title = "Comments"

        reloadModel()

        comments.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(me: MouseEvent) {
                if (me.clickCount == 2) {
                    val name = comments.getValueAt(comments.selectedRow, 0) as String
                    val date = comments.getValueAt(comments.selectedRow, 1) as Date
                    val body = comments.getValueAt(comments.selectedRow, 2) as String
                    DialogManager.show(CommentDetail(project, name, date, body))
                }
            }
        })

        addCommentButton.addActionListener {
            AddCommentDialog(project, worker.mergeRequest, file).show()
            this.worker = GitLabCommentsListWorker.create(project!!, worker.mergeRequest, file)
            reloadModel()
            comments.repaint()
        }
    }

    private fun reloadModel() {
        comments.model = commentsModel(worker.comments)
        comments.columnModel.getColumn(0).preferredWidth = 100
        comments.columnModel.getColumn(1).preferredWidth = 150
        comments.columnModel.getColumn(2).preferredWidth = 400
        comments.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    }

    override fun createCenterPanel(): JComponent? {
        return panel
    }

    private fun commentsModel(notes: List<GitlabNote>?): TableModel {
        val columnNames = arrayOf<Any>("Author", "Date", "Text")
        val data = Array(
            notes!!.size
        ) { arrayOfNulls<Any>(columnNames.size) }
        var i = 0
        notes.sortedWith(Comparator { o1: GitlabNote, o2: GitlabNote -> o2.createdAt.compareTo(o1.createdAt) })
        for (mergeRequest in notes) {
            val row = arrayOfNulls<Any>(columnNames.size)
            row[0] = mergeRequest!!.author.name
            row[1] = mergeRequest.createdAt
            row[2] = mergeRequest.body
            data[i] = row
            i++
        }
        return ReadOnlyTableModel(data, columnNames)
    }
}
