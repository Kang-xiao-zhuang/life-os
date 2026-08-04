package com.zk.lifeos.model

/**
 * How the app resolves dark mode. Shared model: the UI renders it, the service layer maps it,
 * the repository persists it — so it belongs to none of them exclusively.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        /**
         * 深色模式优先 — dark is the product's default look, not merely an option, so a fresh
         * install opens dark regardless of the system setting. It also matches the dark launch
         * window background, so there is no flash before the first Compose frame.
         */
        val DEFAULT = DARK

        /** Tolerant parse — an unknown or corrupt stored value falls back to [DEFAULT]. */
        fun fromStored(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}
