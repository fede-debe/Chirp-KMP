package com.project.core.data.util

/**
 * Multiplatform utility to resolve the underlying Operating System name.
 *
 * ## Strategy / Decisions
 * The backend server relies on the Firebase Admin SDK to dispatch notifications. It needs explicit instruction
 * on whether to route the payload to Apple (APNS) or Google (FCM). This utility bridges that context.
 *
 * ## How It Works
 * Uses Kotlin expect/actual mechanisms to return exact OS strings.
 *
 * ## Technical Details
 * Must return exactly "Android" or "iOS" (capitalized appropriately) as the server expects strict string matching
 * for its data structures.
 */
expect object PlatformUtils {
    fun getOSName(): String
}
