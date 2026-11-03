package com.example.varsivo

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Password hashing for the copy of the account kept on the phone (used for offline login).
 *
 *  - A new random 16-byte salt per password (SecureRandom), so equal passwords get different hashes.
 *  - PBKDF2 with HMAC-SHA256, 120 000 iterations, 256-bit output: slow on purpose, so a stolen hash
 *    is expensive to brute-force.
 *  - Hashes are compared with MessageDigest.isEqual, which takes the same time for every input.
 *  - The plain password is never written to disk.
 *
 * Reference: OWASP (2024) *Password Storage Cheat Sheet*
 * https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
 */
object SecurityUtils {

    fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt.toHex()
    }

    fun hashPassword(password: String, salt: String): String {
        val saltBytes = salt.hexToByteArray()

        val spec = PBEKeySpec(
            password.toCharArray(),
            saltBytes,
            120_000,
            256
        )

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded

        return hash.toHex()
    }

    fun verifyPassword(
        password: String,
        storedHash: String,
        salt: String
    ): Boolean {
        val newHash = hashPassword(password, salt)

        return MessageDigest.isEqual(
            newHash.toByteArray(),
            storedHash.toByteArray()
        )
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    private fun String.hexToByteArray(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}