package com.project.core.data.util

/**
 * Pure-Kotlin SHA-256 (FIPS 180-4) living in `commonMain` so Android and iOS hash byte-for-byte
 * identically — there is no room for platform divergence (JCA vs CryptoKit) in the social sign-in
 * nonce handshake.
 *
 * ## Why this exists
 * Social sign-in providers embed a `nonce` claim in the ID token they mint. Our backend rejects the
 * token unless `nonce == lowercase-hex SHA-256(rawNonce)`. The client therefore hashes the raw nonce
 * the exact same way on every platform; a single shared implementation guarantees that.
 *
 * ## Scope / Decisions
 * - **Hashing only.** No secret material is stored or transmitted. The raw nonce is single-use and
 *   thrown away after one sign-in attempt, so a dependency-free hash (rather than Keystore/CryptoKit)
 *   is the right tool — it keeps both platforms provably identical and adds no new crypto dependency.
 * - Also reused for the iOS PKCE `code_challenge` (`base64url(SHA-256(code_verifier))`), via [hash].
 */
object Sha256 {

    // Round constants (first 32 bits of the fractional parts of the cube roots of the first 64 primes).
    // Declared as Long literals + truncated to Int to avoid hand-writing two's-complement hex.
    private val K: IntArray = longArrayOf(
        0x428a2f98L, 0x71374491L, 0xb5c0fbcfL, 0xe9b5dba5L,
        0x3956c25bL, 0x59f111f1L, 0x923f82a4L, 0xab1c5ed5L,
        0xd807aa98L, 0x12835b01L, 0x243185beL, 0x550c7dc3L,
        0x72be5d74L, 0x80deb1feL, 0x9bdc06a7L, 0xc19bf174L,
        0xe49b69c1L, 0xefbe4786L, 0x0fc19dc6L, 0x240ca1ccL,
        0x2de92c6fL, 0x4a7484aaL, 0x5cb0a9dcL, 0x76f988daL,
        0x983e5152L, 0xa831c66dL, 0xb00327c8L, 0xbf597fc7L,
        0xc6e00bf3L, 0xd5a79147L, 0x06ca6351L, 0x14292967L,
        0x27b70a85L, 0x2e1b2138L, 0x4d2c6dfcL, 0x53380d13L,
        0x650a7354L, 0x766a0abbL, 0x81c2c92eL, 0x92722c85L,
        0xa2bfe8a1L, 0xa81a664bL, 0xc24b8b70L, 0xc76c51a3L,
        0xd192e819L, 0xd6990624L, 0xf40e3585L, 0x106aa070L,
        0x19a4c116L, 0x1e376c08L, 0x2748774cL, 0x34b0bcb5L,
        0x391c0cb3L, 0x4ed8aa4aL, 0x5b9cca4fL, 0x682e6ff3L,
        0x748f82eeL, 0x78a5636fL, 0x84c87814L, 0x8cc70208L,
        0x90befffaL, 0xa4506cebL, 0xbef9a3f7L, 0xc67178f2L,
    ).map { it.toInt() }.toIntArray()

    // Initial hash values (first 32 bits of the fractional parts of the square roots of the first 8 primes).
    private val H0: IntArray = longArrayOf(
        0x6a09e667L,
        0xbb67ae85L,
        0x3c6ef372L,
        0xa54ff53aL,
        0x510e527fL,
        0x9b05688cL,
        0x1f83d9abL,
        0x5be0cd19L,
    ).map { it.toInt() }.toIntArray()

    fun hash(input: String): ByteArray = hash(input.encodeToByteArray())

    /** Lowercase hex SHA-256 of the UTF-8 bytes of [input] — the format the backend compares against. */
    fun hexUtf8(input: String): String = hash(input).toLowercaseHex()

    fun hash(message: ByteArray): ByteArray {
        val h = H0.copyOf()

        // Pre-processing: append 0x80, pad with zeros until length ≡ 56 (mod 64), append 64-bit big-endian bit length.
        val bitLength = message.size.toLong() * 8
        var paddedLength = message.size + 1
        while (paddedLength % 64 != 56) {
            paddedLength++
        }
        paddedLength += 8

        val padded = ByteArray(paddedLength)
        message.copyInto(padded)
        padded[message.size] = 0x80.toByte()
        for (i in 0 until 8) {
            padded[paddedLength - 1 - i] = (bitLength ushr (8 * i) and 0xffL).toByte()
        }

        val w = IntArray(64)
        var chunk = 0
        while (chunk < paddedLength) {
            for (i in 0 until 16) {
                val j = chunk + i * 4
                w[i] = ((padded[j].toInt() and 0xff) shl 24) or
                    ((padded[j + 1].toInt() and 0xff) shl 16) or
                    ((padded[j + 2].toInt() and 0xff) shl 8) or
                    (padded[j + 3].toInt() and 0xff)
            }
            for (i in 16 until 64) {
                val s0 = ror(w[i - 15], 7) xor ror(w[i - 15], 18) xor (w[i - 15] ushr 3)
                val s1 = ror(w[i - 2], 17) xor ror(w[i - 2], 19) xor (w[i - 2] ushr 10)
                w[i] = w[i - 16] + s0 + w[i - 7] + s1
            }

            var a = h[0]
            var b = h[1]
            var c = h[2]
            var d = h[3]
            var e = h[4]
            var f = h[5]
            var g = h[6]
            var hh = h[7]

            for (i in 0 until 64) {
                val bigS1 = ror(e, 6) xor ror(e, 11) xor ror(e, 25)
                val choose = (e and f) xor (e.inv() and g)
                val temp1 = hh + bigS1 + choose + K[i] + w[i]
                val bigS0 = ror(a, 2) xor ror(a, 13) xor ror(a, 22)
                val majority = (a and b) xor (a and c) xor (b and c)
                val temp2 = bigS0 + majority

                hh = g
                g = f
                f = e
                e = d + temp1
                d = c
                c = b
                b = a
                a = temp1 + temp2
            }

            h[0] += a
            h[1] += b
            h[2] += c
            h[3] += d
            h[4] += e
            h[5] += f
            h[6] += g
            h[7] += hh

            chunk += 64
        }

        val out = ByteArray(32)
        for (i in 0 until 8) {
            out[i * 4] = (h[i] ushr 24 and 0xff).toByte()
            out[i * 4 + 1] = (h[i] ushr 16 and 0xff).toByte()
            out[i * 4 + 2] = (h[i] ushr 8 and 0xff).toByte()
            out[i * 4 + 3] = (h[i] and 0xff).toByte()
        }
        return out
    }

    private fun ror(value: Int, bits: Int): Int = (value ushr bits) or (value shl (32 - bits))

    private fun ByteArray.toLowercaseHex(): String {
        val digits = "0123456789abcdef"
        val sb = StringBuilder(size * 2)
        for (b in this) {
            val v = b.toInt() and 0xff
            sb.append(digits[v ushr 4])
            sb.append(digits[v and 0x0f])
        }
        return sb.toString()
    }
}
