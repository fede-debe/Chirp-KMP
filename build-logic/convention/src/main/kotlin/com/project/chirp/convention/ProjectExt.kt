package com.project.chirp.convention

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Provides an extension property to consistently access the Gradle Version Catalog (`libs`) within convention plugins.
 *
 * ## Strategy / Decisions
 * - **Restoring Type-Safe Access:** Pre-compiled convention plugins do not automatically inherit the generated `libs` accessor that standard build scripts receive. This utility dynamically resolves the catalog by type to restore seamless access to dependency notations and version variables.
 *
 * ## How It Works
 * 1. Queries the Project's `extensions` block for the `VersionCatalogsExtension`.
 * 2. Retrieves the specific catalog instance by naming it "libs".
 *
 * ## Technical Details
 * - Must be placed in the `convention` utility package (within the Kotlin source set) so it is properly resolved by the actual plugin classes.
 */
val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
