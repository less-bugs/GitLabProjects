package com.ppolivka.gitlabprojects.configuration

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider

/**
 * Provider of SettingsConfigurable
 *
 * @author ppolivka
 * @since 9.10.2015
 */
class SettingsConfigurableProvider : ConfigurableProvider() {
    override fun createConfigurable(): Configurable? {
        val settingsView = SettingsView()
        settingsView.setup()
        return settingsView
    }
}
