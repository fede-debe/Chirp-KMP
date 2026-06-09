package com.project.chirp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.project.chirp.navigation.ExternalUriHandler

/**
 * Main entry point of the Android application, responsible for intercepting and routing deep links from FCM notifications.
 *
 * ## Strategy / Decisions
 * When an FCM notification is tapped, Android launches the application natively. We must intercept the intent data
 * containing the `chatId` and convert it into a standardized internal URI to feed into the Compose Navigation host.
 *
 * ## How It Works
 * 1. `handleChatMessageDeepLink` extracts the `chatId` from the intent's string extras (key: "chatId").
 * 2. Constructs a custom internal URI: `chirp://chat_detail/{chatId}`.
 * 3. Forwards this URI to the application's external URI handler.
 *
 * ## Technical Details
 * This logic must be placed in **both** `onCreate` and `onNewIntent`.
 * - `onCreate` handles cold starts (app was completely dead).
 * - `onNewIntent` handles warm starts (app was already alive in the background).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        var shouldShowSplashScreen = true

        installSplashScreen().setKeepOnScreenCondition {
            shouldShowSplashScreen
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleChatMessageDeeplink(intent)

        setContent {
            App(
                onAuthenticationChecked = {
                    shouldShowSplashScreen = false
                },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleChatMessageDeeplink(intent)
    }

    private fun handleChatMessageDeeplink(intent: Intent) {
        val chatId = intent.getStringExtra("chatId")
            ?: intent.extras?.getString("chatId")

        if (chatId != null) {
            val deepLinkUrl = "chirp://chat_detail/$chatId"
            ExternalUriHandler.onNewUri(deepLinkUrl)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
