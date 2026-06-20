package com.project.core.data.util

/**
 * Utility enum and property to determine the current desktop operating system.
 *
 * ## Strategy / Decisions
 * To persist data correctly across different desktop environments, we must determine the host OS.
 * Windows, macOS, and Linux all dictate different standard locations for application data. By
 * resolving this once via a system property, we can dynamically route file paths for both our
 * Room Database and DataStore preferences without duplicating OS-check logic.
 *
 * ## How It Works
 * 1. Reads the JVM system property `os.name`.
 * 2. Converts the name to lowercase for safe matching.
 * 3. Uses a `when` expression to check if the string contains "win" (Windows) or "mac" (macOS).
 * 4. Falls back to Linux for any other desktop environment.
 *
 * Technical Details:
 * - Relies on standard JVM `System.getProperty("os.name")`.
 */
enum class DesktopOs {
    WINDOWS,
    MACOS,
    LINUX,
}

val currentOs: DesktopOs
    get() {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("win") -> DesktopOs.WINDOWS
            osName.contains("mac") -> DesktopOs.MACOS
            else -> DesktopOs.LINUX
        }
    }
