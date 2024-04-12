package com.ppolivka.gitlabprojects.request

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.SortedComboBoxModel
import com.ppolivka.gitlabprojects.component.SearchBoxModel
import com.ppolivka.gitlabprojects.configuration.ProjectState
import com.ppolivka.gitlabprojects.configuration.ProjectState.Companion.getInstance
import com.ppolivka.gitlabprojects.merge.info.BranchInfo
import com.ppolivka.gitlabprojects.merge.request.GitLabCreateMergeRequestWorker
import com.ppolivka.gitlabprojects.merge.request.SearchableUser
import org.apache.commons.lang3.StringUtils
import org.gitlab.api.models.GitlabUser
import java.awt.event.ActionEvent
import javax.swing.*

/**
 * Dialog fore creating merge requests
 *
 * @author ppolivka
 * @since 30.10.2015
 */
class CreateMergeRequestDialog(
    project: Project,
    private val mergeRequestWorker: GitLabCreateMergeRequestWorker
) : DialogWrapper(project) {
    private lateinit var mainView: JPanel
    private lateinit var targetBranch: JComboBox<BranchInfo>
    private lateinit var currentBranch: JLabel
    private lateinit var mergeTitle: JTextField
    private lateinit var mergeDescription: JTextArea
    private lateinit var diffButton: JButton
    private lateinit var assigneeBox: JComboBox<Any?>
    private lateinit var removeSourceBranch: JCheckBox
    private lateinit var wip: JCheckBox

    private lateinit var myBranchModel: SortedComboBoxModel<BranchInfo>
    private var lastSelectedBranch: BranchInfo? = null

    val projectState: ProjectState = getInstance(project)

    init {
        init()
    }

    override fun init() {
        super.init()
        title = "Create Merge Request"
        verticalStretch = 2f

        val searchBoxModel = SearchBoxModel(assigneeBox, mergeRequestWorker.searchableUsers!!)
        assigneeBox.setModel(searchBoxModel)
        assigneeBox.isEditable = true
        assigneeBox.addItemListener(searchBoxModel)
        assigneeBox.setBounds(140, 170, 180, 20)

        currentBranch.text = mergeRequestWorker.gitLocalBranch!!.name

        myBranchModel = SortedComboBoxModel { o1: BranchInfo, o2: BranchInfo ->
            StringUtil.naturalCompare(
                o1.name,
                o2.name
            )
        }
        myBranchModel.setAll(mergeRequestWorker.branches)
        targetBranch.setModel(myBranchModel)
        targetBranch.selectedIndex = 0
        if (mergeRequestWorker.lastUsedBranch != null) {
            targetBranch.selectedItem = mergeRequestWorker.lastUsedBranch
        }
        lastSelectedBranch = selectedBranch

        targetBranch.addActionListener {
            prepareTitle()
            lastSelectedBranch = selectedBranch
            projectState.lastMergedBranch = selectedBranch.name
            mergeRequestWorker.diffViewWorker!!.launchLoadDiffInfo(
                mergeRequestWorker.localBranchInfo!!,
                selectedBranch
            )
        }

        prepareTitle()

        val deleteMergedBranch = projectState.deleteMergedBranch
        if (deleteMergedBranch != null && deleteMergedBranch) {
            removeSourceBranch.isSelected = true
        }

        val mergeAsWorkInProgress = projectState.mergeAsWorkInProgress
        if (mergeAsWorkInProgress != null && mergeAsWorkInProgress) {
            wip.isSelected = true
        }

        diffButton.addActionListener { e: ActionEvent? ->
            mergeRequestWorker.diffViewWorker!!.showDiffDialog(
                mergeRequestWorker.localBranchInfo!!, selectedBranch
            )
        }
    }

    override fun doOKAction() {
        val branch = selectedBranch
        if (mergeRequestWorker.checkAction(branch)) {
            var title = mergeTitle.text
            if (wip.isSelected) {
                title = "WIP:$title"
            }
            mergeRequestWorker.createMergeRequest(
                branch,
                assignee,
                title,
                mergeDescription.text,
                removeSourceBranch.isSelected
            )
            super.doOKAction()
        }
    }

    override fun doValidate(): ValidationInfo? {
        if (StringUtils.isBlank(mergeTitle.text)) {
            return ValidationInfo("Merge title cannot be empty", mergeTitle)
        }
        if (selectedBranch.name == currentBranch.text) {
            return ValidationInfo(
                "Target branch must be different from current branch.",
                targetBranch
            )
        }
        return null
    }

    private val selectedBranch: BranchInfo
        get() = targetBranch.selectedItem as BranchInfo

    private val assignee: GitlabUser
        get() {
            val searchableUser = assigneeBox.selectedItem as SearchableUser
            return searchableUser.gitLabUser
        }

    private fun prepareTitle() {
        if (StringUtils.isBlank(mergeTitle.text) || mergeTitleGenerator(
                lastSelectedBranch
            ) == mergeTitle.text
        ) {
            mergeTitle.text = mergeTitleGenerator(selectedBranch)
        }
    }

    private fun mergeTitleGenerator(branchInfo: BranchInfo?): String {
        return "Merge of " + currentBranch.text + " to " + branchInfo
    }

    override fun createCenterPanel(): JComponent {
        return mainView
    }
}
