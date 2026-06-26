package com.project.auth.presentation.social

/**
 * Static, non-secret OAuth client configuration for social sign-in.
 *
 * These are public client identifiers (they ship inside every app binary and are visible in the
 * provider tokens), not credentials — so unlike the API key they live in source rather than
 * `local.properties`/BuildKonfig.
 */
object SocialAuthConfig {

    /**
     * Google WEB / server OAuth client id. Passed as Credential Manager's `serverClientId` on Android
     * so the minted ID token's `aud` equals this value — which the backend allowlists. (The Android
     * OAuth client tied to the package + SHA-1 only authorizes the device; the token audience is the
     * web client id.)
     */
    const val GOOGLE_WEB_CLIENT_ID =
        "900654509612-leqk4b7jhhnj1dn5guctgkp7s60ecvs1.apps.googleusercontent.com"
}
