package com.osscabinet.ossWallet.domain.formatter

/**
 * Pure domain helper for theme detection
 * 
 * Note: This helper determines theme mode from the theme string setting.
 * For "System" theme, the UI layer should still use isSystemInDarkTheme(),
 * but this helper can be used when the theme is explicitly set.
 */
object ThemeHelper {
    /**
     * Determines if the theme is dark based on the theme string
     * 
     * @param theme The theme string ("Light", "Dark", "Amoled", "System")
     * @param isSystemDark The system dark mode state (required when theme is "System")
     * @return Pair<isDarkTheme, isAmoled>
     */
    fun getThemeMode(theme: String, isSystemDark: Boolean = false): ThemeMode {
        return when (theme) {
            "Light" -> ThemeMode(isDarkTheme = false, isAmoled = false)
            "Dark" -> ThemeMode(isDarkTheme = true, isAmoled = false)
            "Amoled" -> ThemeMode(isDarkTheme = true, isAmoled = true)
            "System" -> ThemeMode(isDarkTheme = isSystemDark, isAmoled = false)
            else -> ThemeMode(isDarkTheme = isSystemDark, isAmoled = false)
        }
    }
}

/**
 * Data class representing theme mode
 */
data class ThemeMode(
    val isDarkTheme: Boolean,
    val isAmoled: Boolean
)
