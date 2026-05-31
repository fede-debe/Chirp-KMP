package com.project.chirp

import android.app.Application
import com.project.chirp.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

/**
 * Main Android Application class.
 *
 * ## Strategy / Decisions
 * * **Context Injection:** The Android Context is required for various SDK components
 *   (file system, sensors). By calling `androidContext(this)`, the context becomes
 *   available for injection anywhere in the dependency graph.
 * * **Lifecycle:** Initializing in `onCreate` ensures Koin is set up exactly once
 *   per process, preventing re-initialization during Activity rotations.
 *
 * ## Technical Details
 * * **Permissions:** Requires `android.permission.INTERNET` defined in the manifest
 *   to allow Ktor to make network calls.
 */
class ChirpApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@ChirpApplication)
            androidLogger()
        }
    }
}
