@file:Suppress("ktlint:standard:filename", "filename")

package com.project.chirp

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.application
import com.project.chirp.deeplink.DesktopDeepLinkHandler
import com.project.chirp.di.desktopModule
import com.project.chirp.di.initKoin
import com.project.chirp.navigation.ExternalUriHandler
import com.project.chirp.theme.rememberAppTheme
import com.project.chirp.windows.ChirpWindow
import org.koin.compose.koinInject

/**
 * The process entry point for the Compose Desktop JVM application.
 *
 * ## Strategy / Decisions
 * Desktop applications require a standard `main` function to launch, acting as the root process. We utilize the `application` scope provided by Compose Multiplatform to bridge our shared cross-platform Compose UI into a desktop Window environment.
 * We ignore standard command-line arguments because this application is designed as a GUI launched via a desktop icon.
 *
 * ## How It Works
 * 1. The `main()` function is invoked by the JVM upon process launch.
 * 2. The Compose `application { ... }` block initializes the desktop rendering lifecycle.
 * 3. A desktop `Window` is instantiated to host the composable tree.
 * 4. The shared `App()` composable (containing cross-platform navigation and logic) is executed inside the window.
 * 5. When the user clicks the close icon, the `onCloseRequest` lambda is triggered, invoking `exitApplication()` to kill the JVM process.
 *
 * ## Alternatives / Why Not
 * - **Command Line Execution:** We could technically parse the `Array<String>` args, but rejected this because the target UX is double-clicking an application icon, rendering CLI args unnecessary.
 * - **Direct AWT/Swing Usage:** Swing is a mature framework built on AWT, but we use Compose Multiplatform to abstract away Swing/AWT boilerplate and keep our UI declarative and cross-platform.
 *
 * Technical Details
 * - Execution is bound to the JVM target.
 * - `exitApplication()` terminates the underlying application process.
 */
fun main(args: Array<String>) {
    initKoin {
        modules(desktopModule)
    }

    DesktopDeepLinkHandler.setup()

    val initialDeepLink = args.firstOrNull {
        val cleanedDeepLink = it.trim('"')

        DesktopDeepLinkHandler.supportedUriPatterns.any { it.matches(cleanedDeepLink) }
    }?.trim('"')

    application {
        val applicationStateHolder = koinInject<ApplicationStateHolder>()
        val applicationState by applicationStateHolder.state.collectAsState()
        val windows = applicationState.windows

        var canReceiveDeepLink by remember {
            mutableStateOf(false)
        }

        LaunchedEffect(canReceiveDeepLink) {
            if (canReceiveDeepLink && initialDeepLink != null) {
                ExternalUriHandler.onNewUri(initialDeepLink)
            }
        }

        LaunchedEffect(windows) {
            if (windows.isEmpty()) {
                exitApplication()
            }
        }

        val appTheme = rememberAppTheme(applicationState.themePreference)

        for (window in windows) {
            key(window.id) {
                ChirpWindow(
                    appTheme = appTheme,
                    onCloseRequest = {
                        applicationStateHolder.onWindowCloseRequest(window.id)
                    },
                    onAddWindowClick = applicationStateHolder::onAddWindowClick,
                    onFocusChanged = { focused ->
                        applicationStateHolder.onWindowFocusChanged(window.id, focused)
                    },
                    onDeepLinkListenerSetup = {
                        canReceiveDeepLink = true
                    },
                )
            }
        }

        ChirpTrayMenu(
            state = applicationState.trayState,
            themePreferenceFromAppSettings = applicationState.themePreference,
            onThemePreferenceClick = applicationStateHolder::onThemePreferenceClick,
        )
    }
}
