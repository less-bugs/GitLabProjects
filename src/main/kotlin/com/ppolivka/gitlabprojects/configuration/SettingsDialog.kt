package com.ppolivka.gitlabprojects.configuration

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

/**
 * Wrapper around settings view
 *
 * @author ppolivka
 * @since 17.11.2015
 */
class SettingsDialog(project: Project?) : DialogWrapper(project) {
    private val settingsView = SettingsView()

    init {
        init()
    }

    override fun init() {
        super.init()
        title = SettingsView.DIALOG_TITLE
        settingsView.setup()
    }

    override fun createCenterPanel(): JComponent? {
        return settingsView.createComponent()
    }

    val isModified: Boolean
        get() = settingsView.isModified

    @Throws(ConfigurationException::class)
    fun apply() {
        settingsView.apply()
    }
}
