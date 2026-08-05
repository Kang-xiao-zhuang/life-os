package com.zk.lifeos.model

import java.util.Locale

/**
 * Which language the interface uses. [SYSTEM] follows the phone; the other two pin it, because a
 * phone set to one language and a person who reads another is a normal situation.
 */
enum class AppLanguage {
    SYSTEM,
    CHINESE,
    ENGLISH;

    /** Null means "whatever the system says" — the caller then leaves the configuration alone. */
    fun toLocale(): Locale? = when (this) {
        SYSTEM -> null
        CHINESE -> Locale.SIMPLIFIED_CHINESE
        ENGLISH -> Locale.ENGLISH
    }

    companion object {
        val DEFAULT = SYSTEM

        fun fromStored(value: String?): AppLanguage =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}
