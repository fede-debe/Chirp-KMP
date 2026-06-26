package com.project.auth.presentation.social

/**
 * Outcome of a native provider sign-in sheet, handed back from the platform launcher to the screen.
 *
 * Only the provider [token] (Google ID token / Apple identity token) and — first time only — the
 * user's [fullName] cross this boundary. The provider token is sent to our backend exactly once to
 * authenticate and is never stored.
 */
sealed interface SocialSignInResult {

    data class Success(
        val token: String,
        val fullName: String? = null,
    ) : SocialSignInResult

    /** User dismissed/backed out of the provider sheet — not an error; just re-enable the button. */
    data object Cancelled : SocialSignInResult

    /** The provider flow failed (no account, misconfiguration, network, decode error, …). */
    data class Failure(val message: String? = null) : SocialSignInResult
}
