package com.project.core.data.logging

import co.touchlab.kermit.Logger
import com.project.core.domain.logging.ChirpLogger

/**
 * Concrete implementation of the [ChirpLogger] abstraction utilizing the Touchlab Kermit logging library.
 *
 * ## Strategy / Decisions
 * - **Singleton Object:** Implemented as a Kotlin `object` rather than a `class` because the logger has no internal state and requires no constructor parameters.
 * This saves memory and prevents multiple instances of the logger wrapper.
 *
 * ## How It Works
 * Intercepts calls from the domain-level `ChirpLogger` interface and routes them directly to Touchlab Kermit's static logging methods (`Logger.i`, `Logger.w`, `Logger.e`, `Logger.d`), forwarding messages and throwables appropriately.
 */
object KermitLogger : ChirpLogger {

    override fun debug(message: String) {
        Logger.d(message)
    }

    override fun info(message: String) {
        Logger.i(message)
    }

    override fun warn(message: String) {
        Logger.w(message)
    }

    override fun error(message: String, throwable: Throwable?) {
        Logger.e(message, throwable)
    }
}
