package com.project.auth.presentation.social

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.AuthenticationServices.ASPresentationAnchor
import platform.Foundation.NSError
import platform.darwin.NSObject

// ASAuthorizationError.canceled — the user dismissed the Apple sheet.
private const val APPLE_ERROR_CANCELED = 1001L

/**
 * Native "Sign in with Apple" via `ASAuthorizationController`. Requests fullName + email scopes and
 * stamps the request with the hashed nonce, so Apple's identity token carries `nonce = SHA256(rawNonce)`.
 *
 * Requires the "Sign in with Apple" capability + `com.apple.developer.applesignin` entitlement on the
 * bundle id (`com.project.chirp`). The identity token's `aud` is the bundle id, which the backend allowlists.
 */
@Composable
actual fun rememberAppleSignInLauncher(
    onResult: (SocialSignInResult) -> Unit,
): SocialSignInLauncher {
    // ASAuthorizationController keeps its delegate weakly, so we hold a strong ref here until the
    // flow completes (then release it) to keep the coordinator alive for the duration of the sheet.
    val holder = remember { CoordinatorHolder() }

    return remember(onResult) {
        SocialSignInLauncher { hashedNonce ->
            val coordinator = AppleSignInCoordinator(
                hashedNonce = hashedNonce,
                onResult = { result ->
                    holder.coordinator = null
                    onResult(result)
                },
            )
            holder.coordinator = coordinator
            coordinator.start()
        }
    }
}

private class CoordinatorHolder {
    var coordinator: AppleSignInCoordinator? = null
}

private class AppleSignInCoordinator(
    private val hashedNonce: String,
    private val onResult: (SocialSignInResult) -> Unit,
) : NSObject(),
    ASAuthorizationControllerDelegateProtocol,
    ASAuthorizationControllerPresentationContextProvidingProtocol {

    fun start() {
        val request = ASAuthorizationAppleIDProvider().createRequest().apply {
            requestedScopes = listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)
            nonce = hashedNonce
        }
        val controller = ASAuthorizationController(authorizationRequests = listOf(request))
        controller.delegate = this
        controller.presentationContextProvider = this
        controller.performRequests()
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization,
    ) {
        val credential = didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
        if (credential == null) {
            onResult(SocialSignInResult.Failure("No Apple credential returned"))
            return
        }

        val identityToken = credential.identityToken?.toByteArray()?.decodeToString()
        if (identityToken.isNullOrBlank()) {
            onResult(SocialSignInResult.Failure("Missing Apple identity token"))
            return
        }

        // fullName is only populated on the user's first authorization.
        val fullName = credential.fullName?.let { components ->
            listOfNotNull(components.givenName, components.familyName)
                .joinToString(" ")
                .ifBlank { null }
        }

        onResult(SocialSignInResult.Success(token = identityToken, fullName = fullName))
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError,
    ) {
        if (didCompleteWithError.code == APPLE_ERROR_CANCELED) {
            onResult(SocialSignInResult.Cancelled)
        } else {
            onResult(SocialSignInResult.Failure(didCompleteWithError.localizedDescription))
        }
    }

    override fun presentationAnchorForAuthorizationController(
        controller: ASAuthorizationController,
    ): ASPresentationAnchor = presentationAnchor()
}
