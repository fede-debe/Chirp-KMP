package com.project.core.data.security

/**
 * Synchronous, platform-backed secure key-value store.
 *
 * ## Strategy / Decisions
 * - **Hardware-backed secrets:** Implementations persist values in the operating system's secure
 *   store — Android Keystore on Android, Keychain Services on iOS — so sensitive data (JWT tokens,
 *   session data) is never written to disk in plaintext.
 * - **Synchronous by design:** Keystore/Keychain/`SharedPreferences` reads are synchronous, which lets
 *   [com.project.core.data.auth.EncryptedSessionStorage] seed its reactive state at construction with no
 *   async race. A `suspend` API would add ceremony for no benefit here.
 *
 * ## How It Works
 * Implementations encrypt on write and decrypt on read (Android), or delegate encryption-at-rest to the
 * OS (iOS Keychain). Callers deal only in plaintext [String]s and are unaware of the mechanism.
 */
interface SecureStorage {
    fun putString(key: String, value: String)
    fun getString(key: String): String?
    fun remove(key: String)
}
