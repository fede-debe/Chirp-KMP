package com.project.core.data.util

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Produces the single-use nonce pair for a social sign-in attempt.
 *
 * Each attempt MUST use a fresh [newRawNonce]. The raw nonce is what we send to our backend; the
 * [hashedNonce] (lowercase-hex SHA-256) is what we hand to the provider (Credential Manager / Apple),
 * so the provider's token carries `nonce = SHA256_hex(rawNonce)`. The backend re-derives and compares.
 *
 * Injected (rather than a top-level function) so it can be swapped for a deterministic fake in tests.
 */
class NonceFactory {

    /** 256 bits of randomness rendered as 64 lowercase hex chars. Generate a new one per attempt. */
    @OptIn(ExperimentalUuidApi::class)
    fun newRawNonce(): String =
        (Uuid.random().toHexString() + Uuid.random().toHexString())

    fun hashedNonce(rawNonce: String): String = Sha256.hexUtf8(rawNonce)
}
