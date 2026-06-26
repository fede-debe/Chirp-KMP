package com.project.auth.presentation.social

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Logcat tag for the debug-only ID-token claim dump. Filter Logcat by this to read `aud`/`iss`/`nonce`. */
private const val GOOGLE_AUTH_DEBUG_TAG = "ChirpGoogleAuth"

/**
 * Android Google sign-in via Credential Manager. The ID token is requested with
 * `serverClientId = web client id`, so its `aud` matches the backend allowlist; `setNonce` carries the
 * hashed nonce into the token's `nonce` claim.
 *
 * `LocalContext` resolves to the hosting Activity (the same mechanism the media-picker launchers use),
 * which Credential Manager needs to present its bottom sheet.
 */
@Composable
actual fun rememberGoogleSignInLauncher(
    onResult: (SocialSignInResult) -> Unit,
): SocialSignInLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    return remember(context, credentialManager, onResult) {
        SocialSignInLauncher { hashedNonce ->
            scope.launch {
                try {
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setServerClientId(SocialAuthConfig.GOOGLE_WEB_CLIENT_ID)
                        .setNonce(hashedNonce)
                        // Don't restrict to previously-authorized accounts — allow first-time sign-in.
                        .setFilterByAuthorizedAccounts(false)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val response = credentialManager.getCredential(
                        context = context,
                        request = request,
                    )

                    val credential = response.credential
                    if (credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        logIdTokenClaimsForDebug(
                            context = context,
                            idToken = googleIdTokenCredential.idToken,
                            requestedHashedNonce = hashedNonce,
                        )
                        onResult(SocialSignInResult.Success(token = googleIdTokenCredential.idToken))
                    } else {
                        onResult(SocialSignInResult.Failure("Unexpected credential type"))
                    }
                } catch (e: GetCredentialCancellationException) {
                    onResult(SocialSignInResult.Cancelled)
                } catch (e: GoogleIdTokenParsingException) {
                    onResult(SocialSignInResult.Failure(e.message))
                } catch (e: GetCredentialException) {
                    // Includes NoCredentialException (no Google account on device / SHA-1 not registered).
                    onResult(SocialSignInResult.Failure(e.message))
                }
            }
        }
    }
}

/**
 * DEBUG-ONLY diagnostic. Decodes the just-minted Google ID token and logs only its `aud`, `iss`, and
 * `nonce` claims (never the token itself) to Logcat under [GOOGLE_AUTH_DEBUG_TAG], so the audience the
 * backend will see can be read directly instead of pasting the token into jwt.io.
 *
 * The token logged here is byte-for-byte the one handed to [SocialSignInResult.Success] and POSTed to
 * `/auth/google`, so reading it at mint time is equivalent to reading it "right before the network call".
 *
 * Gated on a debuggable build ([ApplicationInfo.FLAG_DEBUGGABLE]) → a no-op in any release build, so it
 * can never leak token claims in production. Remove once social sign-in is confirmed working.
 */
private fun logIdTokenClaimsForDebug(
    context: Context,
    idToken: String,
    requestedHashedNonce: String,
) {
    val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    if (!isDebuggable) return

    try {
        // A JWT is header.payload.signature — the middle segment is base64url-encoded JSON claims.
        val payloadSegment = idToken.split(".").getOrNull(1) ?: return
        val decoded = Base64.decode(
            payloadSegment,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        ).decodeToString()
        val claims = JSONObject(decoded)

        val aud = claims.optString("aud")
        val iss = claims.optString("iss")
        val nonce = claims.optString("nonce")

        Log.d(
            GOOGLE_AUTH_DEBUG_TAG,
            "Google ID token claims → " +
                "aud=$aud | " +
                "audIsWebClientId=${aud == SocialAuthConfig.GOOGLE_WEB_CLIENT_ID} | " +
                "iss=$iss | " +
                "nonce=$nonce | " +
                "nonceMatchesRequest=${nonce == requestedHashedNonce}",
        )
    } catch (e: Exception) {
        // Never let a debug aid affect the real flow.
        Log.w(GOOGLE_AUTH_DEBUG_TAG, "Could not decode ID token for debug logging", e)
    }
}
