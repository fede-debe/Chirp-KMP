package com.project.chirp.convention

import org.gradle.api.Project
import java.util.Locale

/**
 * **Header:** Provides Project extension functions to dynamically calculate Android namespaces, resource prefixes, and iOS framework names based on a module's Gradle path.
 *
 * ## Strategy / Decisions
 * - **Dynamic Convention over Configuration:** Instead of hardcoding namespaces (e.g., `com.plcoding.chat.data`) or resource prefixes in every single module's `build.gradle.kts`, we use the module's absolute Gradle path (e.g., `:core:data`) to derive these values automatically. This heavily reduces boilerplate and centralizes naming rules across all modules.
 *
 * ## How It Works
 * 1. **pathToPackageName:** Takes the module path, replaces colons with periods, converts to lowercase, and prepends the root project package name (e.g., `:core:data` -> `com.plcoding.core.data`).
 * 2. **pathToResourcePrefix:** Takes the path, replaces colons with underscores, drops the leading underscore, and appends a trailing underscore so it can prefix resources cleanly (e.g., `:core:data` -> `core_data_`).
 * 3. **pathToFrameworkName:** Splits the module path using multiple delimiters (colons, dashes, underscores, spaces). It loops over each segment, capitalizes the first letter (TitleCase), and joins them without separators (e.g., `:core:data` -> `CoreData`).
 *
 * ## Alternatives / Why Not
 * - **Manual hardcoding:** Rejected because it becomes extremely tedious to maintain across dozens of KMP modules. Any structural or target architecture changes would require manually editing every module's build file.
 *
 * Technical Details
 * - Operates strictly on the Gradle `Project.path` string.
 */
fun Project.pathToPackageName(): String {
    val relativePackageName = path
        .replace(':', '.')
        .lowercase()

    return "com.project$relativePackageName"
}

fun Project.pathToResourcePrefix(): String {
    return path
        .replace(':', '_')
        .lowercase()
        .drop(1) + "_"
}

fun Project.pathToFrameworkName(): String {
    val parts = this.path.split(":", "-", "_", " ")
    return parts.joinToString("") { part ->
        part.replaceFirstChar {
            it.titlecase(Locale.ROOT)
        }
    }
}
