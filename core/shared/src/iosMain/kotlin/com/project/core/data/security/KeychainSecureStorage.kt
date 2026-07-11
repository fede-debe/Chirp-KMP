@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.project.core.data.security

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

/**
 * iOS [SecureStorage] backed by **Keychain Services**.
 *
 * ## Strategy / Decisions
 * - **OS-managed encryption-at-rest:** Values are stored as `kSecClassGenericPassword` items. The
 *   Keychain itself encrypts entries using hardware-backed keys, so there's no need to hand-roll AES on
 *   iOS — storing the blob in the Keychain *is* the encryption.
 * - **Accessibility:** `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` keeps the session usable for
 *   background work after the first unlock while preventing the item from migrating to other devices via
 *   backups.
 * - **Bridging:** Kotlin `String`s are passed straight to `CFBridgingRetain` (which bridges them to
 *   `CFString`); payloads cross the boundary as `NSData` via the [toNSData]/[toByteArray] helpers, avoiding
 *   the invalid `String as NSString` cast.
 *
 * ## How It Works
 * Each operation builds a CoreFoundation query dictionary keyed by a fixed service + the caller's key
 * (account). `putString` deletes any existing item then adds the new one; `getString` copies the data
 * back out; `remove` deletes it. Bridged CoreFoundation values are released after each call.
 */
class KeychainSecureStorage : SecureStorage {

    override fun putString(key: String, value: String) {
        // Keychain "add" fails if the item already exists, so replace.
        remove(key)

        val cfService = CFBridgingRetain(SERVICE)
        val cfAccount = CFBridgingRetain(key)
        val cfData = CFBridgingRetain(value.encodeToByteArray().toNSData())

        val query = CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            0.convert(),
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, cfService)
        CFDictionaryAddValue(query, kSecAttrAccount, cfAccount)
        CFDictionaryAddValue(query, kSecValueData, cfData)
        CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)

        SecItemAdd(query, null)

        CFRelease(query)
        CFRelease(cfService)
        CFRelease(cfAccount)
        CFRelease(cfData)
    }

    override fun getString(key: String): String? {
        val cfService = CFBridgingRetain(SERVICE)
        val cfAccount = CFBridgingRetain(key)

        val query = CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            0.convert(),
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, cfService)
        CFDictionaryAddValue(query, kSecAttrAccount, cfAccount)
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        val data = memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            if (status == errSecSuccess) {
                CFBridgingRelease(result.value) as? NSData
            } else {
                null
            }
        }

        CFRelease(query)
        CFRelease(cfService)
        CFRelease(cfAccount)

        return data?.toByteArray()?.decodeToString()
    }

    override fun remove(key: String) {
        val cfService = CFBridgingRetain(SERVICE)
        val cfAccount = CFBridgingRetain(key)

        val query = CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            0.convert(),
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, cfService)
        CFDictionaryAddValue(query, kSecAttrAccount, cfAccount)

        SecItemDelete(query)

        CFRelease(query)
        CFRelease(cfService)
        CFRelease(cfAccount)
    }

    private fun ByteArray.toNSData(): NSData {
        if (isEmpty()) return NSData()
        return usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.convert())
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        val length = this.length.toInt()
        if (length == 0) return ByteArray(0)
        val result = ByteArray(length)
        result.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, this.length)
        }
        return result
    }

    private companion object {
        const val SERVICE = "com.project.chirp.secure"
    }
}
