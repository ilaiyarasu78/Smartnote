package com.ilaiyarasu.smartnote.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionHelper {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val KEY_LENGTH_BYTE = 32

    private fun getOrCreateKey(context: Context): SecretKeySpec {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        val sharedPrefs = EncryptedSharedPreferences.create(
            context,
            "secret_shared_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        var keyString = sharedPrefs.getString("encryption_key", null)
        if (keyString == null) {
            val key = ByteArray(KEY_LENGTH_BYTE)
            SecureRandom().nextBytes(key)
            keyString = android.util.Base64.encodeToString(key, android.util.Base64.DEFAULT)
            sharedPrefs.edit().putString("encryption_key", keyString).commit()
        }

        val keyBytes = android.util.Base64.decode(keyString, android.util.Base64.DEFAULT)
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(context: Context, data: String): String {
        val key = getOrCreateKey(context)
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)
        val cipherText = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    fun decrypt(context: Context, encryptedData: String): String {
        val key = getOrCreateKey(context)
        val combined = Base64.decode(encryptedData, Base64.DEFAULT)
        val iv = ByteArray(IV_LENGTH_BYTE)
        System.arraycopy(combined, 0, iv, 0, iv.size)
        val cipherText = ByteArray(combined.size - iv.size)
        System.arraycopy(combined, iv.size, cipherText, 0, cipherText.size)
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)
        val decryptedText = cipher.doFinal(cipherText)
        
        return String(decryptedText, StandardCharsets.UTF_8)
    }
}
