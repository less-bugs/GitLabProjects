package com.ppolivka.gitlabprojects.configuration

/**
 * Enum of all errors that can be thrown during settings
 *
 * @author ppolivka
 * @since 27.10.2015
 */
enum class SettingError(val message: String) {
    NOT_A_URL("Provided server url is not valid (must be in format http(s)://server.com)"),
    SERVER_CANNOT_BE_REACHED("Provided server cannot be reached"),
    INVALID_API_TOKEN("Provided API Token is not valid"),
    GENERAL_ERROR("Error during connection to server");

    fun message(): String {
        return message
    }
}
