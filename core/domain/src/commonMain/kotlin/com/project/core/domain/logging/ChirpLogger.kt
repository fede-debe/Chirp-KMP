package com.project.core.domain.logging

/**
 * Domain-level abstraction defining the required capabilities for any logging mechanism used within the application.
 *
 * ## Strategy / Decisions
 * - **Dependency Inversion / Clean Architecture:** By placing this interface in the Domain layer and its implementation in the Data layer, the entire project depends strictly on this abstraction rather than a specific third-party library.
 * - **Future-Proofing:** If the underlying logging library (currently Touchlab Kermit) needs to be replaced in the future, only the concrete implementation in the Data layer will need to change. The rest of the codebase will remain untouched.
 *
 * ## How It Works
 * Exposes standard logging severities (Info, Warn, Error, and Debug). The Error log specifically accommodates a `Throwable` to capture stack traces.
 *
 * @param message The string message to be logged.
 * @param throwable An optional exception or throwable to be recorded alongside error logs.
 */
interface ChirpLogger {
    fun debug(message: String)
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String, throwable: Throwable? = null)
}
