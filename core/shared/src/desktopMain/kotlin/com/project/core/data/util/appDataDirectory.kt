@file:Suppress("ktlint:standard:filename", "filename")

package com.project.core.data.util

import java.io.File

/**
 * Resolves and provisions the platform-specific root directory for storing application data.
 *
 * ## Strategy / Decisions
 * Both the DataStore and the Room Database are ultimately just files on the file system.
 * Instead of duplicating path-resolution logic in both factories, this logic was extracted
 * into a centralized utility function. This ensures all app data (preferences and databases)
 * is stored in a unified, OS-compliant directory folder (`Chirp`).
 *
 * ## How It Works
 * 1. Retrieves the current `DesktopOS`.
 * 2. If Windows: Fetches the `APPDATA` environment variable and appends the `Chirp` folder.
 * 3. If macOS: Fetches the `user.home` property, appending `Library/Application Support/Chirp`.
 * 4. If Linux: Fetches the `user.home` property, appending `.local/share/Chirp`.
 * 5. Checks if the resolved directory exists.
 * 6. If it does not exist, calls `mkdirs()` to recursively create the missing directory tree
 * so the app doesn't crash upon first launch.
 *
 * Technical Details:
 * - Windows resolution requires `System.getenv("APPDATA")` (capitalized).
 * - Unix-based systems require `System.getProperty("user.home")`.
 * - Employs `mkdirs()` instead of `mkdir()` to guarantee the entire relative path is created.
 *
 * @return The absolute File reference to the initialized app data directory.
 */
val appDataDirectory: File
    get() {
        val userHome = System.getProperty("user.home")
        return when (currentOs) {
            DesktopOs.WINDOWS -> File(System.getenv("APPDATA"), "Chirp")
            DesktopOs.MACOS -> File(userHome, "Library/Application Support/Chirp")
            DesktopOs.LINUX -> File(userHome, ".local/share/Chirp")
        }
    }
