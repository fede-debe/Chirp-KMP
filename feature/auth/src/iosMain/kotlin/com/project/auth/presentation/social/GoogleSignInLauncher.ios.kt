package com.project.auth.presentation.social

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.project.core.data.util.NonceFactory
import com.project.core.data.util.Sha256
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSDictionary
import platform.Foundation.NSError
import platform.Foundation.NSJSONSerialization
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.dictionaryWithContentsOfFile
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// ASWebAuthenticationSessionErrorCodeCanceledLogin — the user dismissed the web sheet.
private const val WEB_AUTH_CANCELED = 1L

/**
 * iOS Google sign-in via `ASWebAuthenticationSession` (OAuth 2.0 authorization-code + PKCE) — no
 * GoogleSignIn SDK, so it stays in shared Kotlin like the Apple path and needs no Xcode SPM/cinterop.
 * The hashed nonce is passed as the `nonce` auth param so the resulting ID token carries it.
 *
 * PREREQUISITES (currently NOT met — the flow fails cleanly until then):
 *  1. An iOS OAuth client must exist and its `CLIENT_ID` + `REVERSED_CLIENT_ID` must be present in
 *     `GoogleService-Info.plist` (today the plist is push-only and has neither).
 *  2. The `REVERSED_CLIENT_ID` must be registered as a URL scheme in `Info.plist` (callback).
 *  3. The backend must allowlist the iOS client id as a valid `aud` (until then the exchange's token
 *     is rejected with 401 INVALID_TOKEN).
 */
@Composable
actual fun rememberGoogleSignInLauncher(
    onResult: (SocialSignInResult) -> Unit,
): SocialSignInLauncher {
    val holder = remember { GoogleCoordinatorHolder() }

    return remember(onResult) {
        SocialSignInLauncher { hashedNonce ->
            val coordinator = GoogleSignInCoordinator(
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

private class GoogleCoordinatorHolder {
    var coordinator: GoogleSignInCoordinator? = null
}

private class GoogleSignInCoordinator(
    private val hashedNonce: String,
    private val onResult: (SocialSignInResult) -> Unit,
) : NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {

    private var session: ASWebAuthenticationSession? = null

    @OptIn(ExperimentalEncodingApi::class)
    fun start() {
        val config = readGoogleConfig()
        if (config == null) {
            onResult(
                SocialSignInResult.Failure(
                    "Google iOS sign-in is not configured: GoogleService-Info.plist is missing " +
                        "CLIENT_ID / REVERSED_CLIENT_ID (no iOS OAuth client).",
                ),
            )
            return
        }
        val (clientId, reversedClientId) = config
        val redirectUri = "$reversedClientId:/oauth2redirect"

        // PKCE: a 64-char hex verifier (URL-safe) and its base64url-SHA256 challenge.
        val codeVerifier = NonceFactory().newRawNonce()
        val codeChallenge = Base64.UrlSafe
            .withPadding(Base64.PaddingOption.ABSENT)
            .encode(Sha256.hash(codeVerifier))

        val components = NSURLComponents(string = "https://accounts.google.com/o/oauth2/v2/auth")
        components.queryItems = listOf(
            NSURLQueryItem(name = "client_id", value = clientId),
            NSURLQueryItem(name = "redirect_uri", value = redirectUri),
            NSURLQueryItem(name = "response_type", value = "code"),
            NSURLQueryItem(name = "scope", value = "openid email profile"),
            NSURLQueryItem(name = "nonce", value = hashedNonce),
            NSURLQueryItem(name = "code_challenge", value = codeChallenge),
            NSURLQueryItem(name = "code_challenge_method", value = "S256"),
        )
        val authUrl = components.URL
        if (authUrl == null) {
            onResult(SocialSignInResult.Failure("Could not build the Google authorization URL"))
            return
        }

        val webSession = ASWebAuthenticationSession(
            uRL = authUrl,
            callbackURLScheme = reversedClientId,
        ) { callbackUrl, error ->
            when {
                error != null -> {
                    if (error.code == WEB_AUTH_CANCELED) {
                        onResult(SocialSignInResult.Cancelled)
                    } else {
                        onResult(SocialSignInResult.Failure(error.localizedDescription))
                    }
                }
                callbackUrl != null -> {
                    val code = queryParam(callbackUrl, "code")
                    if (code == null) {
                        onResult(SocialSignInResult.Failure("No authorization code in Google callback"))
                    } else {
                        exchangeCodeForIdToken(
                            code = code,
                            clientId = clientId,
                            redirectUri = redirectUri,
                            codeVerifier = codeVerifier,
                            onResult = onResult,
                        )
                    }
                }
                else -> onResult(SocialSignInResult.Failure("Google sign-in returned no result"))
            }
        }
        webSession.presentationContextProvider = this
        webSession.prefersEphemeralWebBrowserSession = false
        session = webSession
        webSession.start()
    }

    override fun presentationAnchorForWebAuthenticationSession(
        session: ASWebAuthenticationSession,
    ): ASPresentationAnchor = presentationAnchor()
}

private fun readGoogleConfig(): Pair<String, String>? {
    val path = NSBundle.mainBundle.pathForResource("GoogleService-Info", "plist") ?: return null
    // Kotlin/Native bridges NSDictionary to a Kotlin Map, so read values via indexing.
    val dict = NSDictionary.dictionaryWithContentsOfFile(path) ?: return null
    val clientId = dict["CLIENT_ID"] as? String ?: return null
    val reversedClientId = dict["REVERSED_CLIENT_ID"] as? String ?: return null
    return clientId to reversedClientId
}

private fun queryParam(url: NSURL, name: String): String? {
    val components = NSURLComponents(uRL = url, resolvingAgainstBaseURL = false)
    val items = components.queryItems ?: return null
    for (item in items) {
        val queryItem = item as? NSURLQueryItem ?: continue
        if (queryItem.name == name) {
            return queryItem.value
        }
    }
    return null
}

@OptIn(ExperimentalForeignApi::class)
private fun exchangeCodeForIdToken(
    code: String,
    clientId: String,
    redirectUri: String,
    codeVerifier: String,
    onResult: (SocialSignInResult) -> Unit,
) {
    val tokenUrl = NSURL(string = "https://oauth2.googleapis.com/token")

    val bodyComponents = NSURLComponents()
    bodyComponents.queryItems = listOf(
        NSURLQueryItem(name = "grant_type", value = "authorization_code"),
        NSURLQueryItem(name = "code", value = code),
        NSURLQueryItem(name = "client_id", value = clientId),
        NSURLQueryItem(name = "redirect_uri", value = redirectUri),
        NSURLQueryItem(name = "code_verifier", value = codeVerifier),
    )
    val body = bodyComponents.percentEncodedQuery ?: ""

    val request = NSMutableURLRequest(uRL = tokenUrl)
    request.setHTTPMethod("POST")
    request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField = "Content-Type")
    request.setHTTPBody(body.encodeToByteArray().toNSData())

    // NSURLSession callbacks run off the main thread; hop back before touching UI/ViewModel state.
    val handler: (NSData?, NSURLResponse?, NSError?) -> Unit = handler@{ data, _, error ->
        onMain {
            when {
                error != null -> onResult(SocialSignInResult.Failure(error.localizedDescription))
                data != null -> {
                    val idToken = parseIdToken(data)
                    if (idToken != null) {
                        onResult(SocialSignInResult.Success(token = idToken))
                    } else {
                        onResult(SocialSignInResult.Failure("Token exchange did not return an id_token"))
                    }
                }
                else -> onResult(SocialSignInResult.Failure("Empty Google token response"))
            }
        }
    }
    val task = NSURLSession.sharedSession.dataTaskWithRequest(request, handler)
    task.resume()
}

@OptIn(ExperimentalForeignApi::class)
private fun parseIdToken(data: NSData): String? {
    val json = NSJSONSerialization.JSONObjectWithData(data, 0uL, null) as? Map<*, *> ?: return null
    return json["id_token"] as? String
}

private fun onMain(block: () -> Unit) {
    dispatch_async(dispatch_get_main_queue()) { block() }
}
