package com.ppolivka.gitlabprojects.comment

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.util.*
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea

/**
 * Dialog listing details of one comment
 *
 * @author ppolivka
 * @since 1.3.2
 */
class CommentDetail(project: Project?, name: String, date: Date, body: String) :
    DialogWrapper(project) {
    private lateinit var panel: JPanel
    private lateinit var authorName: JLabel
    private lateinit var dateText: JLabel
    private lateinit var bodyText: JTextArea

    init {
        init()
        title = "Comment Detail"
        authorName.text = name
        dateText.text = date.toString()
        bodyText.text = body
    }

    override fun createCenterPanel(): JComponent {
        return panel
    }
}
