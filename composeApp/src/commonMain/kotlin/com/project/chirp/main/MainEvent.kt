package com.project.chirp.main

/**
 * Defines one-time events dispatched from the MainViewModel to the root UI layer.
 * * ## Strategy / Decisions
 * Implemented as a `sealed interface` rather than a simple primitive or Unit type to maintain
 * strict architectural consistency with other screens across the app, ensuring uniformity in
 * event handling.
 * * ## Alternatives / Why Not
 * - **Channel of type `Unit`:** The instructor originally considered using a `Channel<Unit>` since
 * there is currently only a single event (session expiration). This was rejected in favor of the
 * sealed interface approach to "stay consistent" with how events are modeled elsewhere in the project.
 */
sealed interface MainEvent {
    data object OnSessionExpired : MainEvent
}
