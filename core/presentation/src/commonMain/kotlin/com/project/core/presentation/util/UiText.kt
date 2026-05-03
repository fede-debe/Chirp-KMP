package com.project.core.presentation.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * A wrapper interface that encapsulates both dynamic raw strings and static string resources,
 * allowing string data to be handled safely outside of the immediate UI layer.
 *
 * ## Strategy / Decisions
 * By wrapping strings into a `UiText` sealed interface, we decouple string resolution from
 * non-UI classes (like ViewModels). A ViewModel can evaluate an API response, determine the appropriate
 * error state, and emit a `UiText` object representing a localized string resource. This sets up the
 * architecture for robust localization while maintaining clean layer separation (presentation logic vs. UI rendering).
 *
 * ## How It Works
 * 1. **DynamicString:** A data class used for raw text strings, such as dynamic error messages fetched directly from a backend API.
 * 2. **Resource:** A standard class wrapping a Compose Multiplatform resource ID. It optionally accepts an array of arguments to populate string placeholders.
 * 3. **asString():** A `@Composable` function that resolves the text. It passes `DynamicString` values through directly, and delegates `Resource` instances to the Compose framework's `stringResource()`, spreading any placeholder arguments.
 * 4. **asStringAsync():** A `suspend` function serving as an alternative unwrapper for asynchronous UI contexts that are not `@Composable`. It utilizes the Compose `getString()` method to fetch the value.
 *
 * ## Alternatives / Why Not
 * - **Evaluating String Resources in the ViewModel:** If string resources were unwrapped directly in the ViewModel, Android system language changes would not automatically reflect in the app. The app would need to be killed and relaunched, or the ViewModel manually recreated. By passing the wrapped `UiText` state to the UI layer and invoking `asString()` directly within a composable, Android's configuration changes trigger an immediate recreation of the Compose tree, automatically displaying the new language.
 *
 * ## Technical Details
 * - Resolution functions (`asString` and `asStringAsync`) must strictly be executed within the UI layer.
 * - Utilizes the spread operator (`*`) to pass vararg elements into Compose string resolvers.
 *
 * @see DynamicString
 * @see Resource
 */
sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    class Resource(
        val id: StringResource,
        val args: Array<Any> = arrayOf(),
    ) : UiText

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is Resource -> stringResource(
                resource = id,
                *args,
            )
        }
    }

    suspend fun asStringAsync(): String {
        return when (this) {
            is DynamicString -> value
            is Resource -> getString(
                resource = id,
                *args,
            )
        }
    }
}
