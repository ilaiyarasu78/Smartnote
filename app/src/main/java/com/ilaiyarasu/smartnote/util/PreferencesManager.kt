package com.ilaiyarasu.smartnote.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("smartnote_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN = "app_pin"
        private const val KEY_IS_PIN_ENABLED = "is_pin_enabled"
        private const val KEY_IS_DARK_MODE = "is_dark_mode"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_IS_AUTO_SYNC_ENABLED = "is_auto_sync_enabled"
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val KEY_LAST_SIGNED_IN_EMAIL = "last_signed_in_email"
        private const val KEY_USER_NAME = "user_name"
    }

    var userName: String?
        get() = sharedPreferences.getString(KEY_USER_NAME, null)
        set(value) = sharedPreferences.edit { putString(KEY_USER_NAME, value) }

    var lastSignedInEmail: String?
        get() = sharedPreferences.getString(KEY_LAST_SIGNED_IN_EMAIL, null)
        set(value) = sharedPreferences.edit { putString(KEY_LAST_SIGNED_IN_EMAIL, value) }

    var isAutoSyncEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_AUTO_SYNC_ENABLED, true)
        set(value) = sharedPreferences.edit { putBoolean(KEY_IS_AUTO_SYNC_ENABLED, value) }

    var lastSyncTimestamp: Long
        get() = sharedPreferences.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
        set(value) = sharedPreferences.edit { putLong(KEY_LAST_SYNC_TIMESTAMP, value) }

    var isPinEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_PIN_ENABLED, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_IS_PIN_ENABLED, value) }

    var isDarkMode: Boolean
        get() = sharedPreferences.getBoolean(KEY_IS_DARK_MODE, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_IS_DARK_MODE, value) }

    fun setPin(pin: String) {
        sharedPreferences.edit { putString(KEY_PIN, pin) }
    }

    fun verifyPin(pin: String): Boolean {
        return sharedPreferences.getString(KEY_PIN, null) == pin
    }

    fun getPin(): String? {
        return sharedPreferences.getString(KEY_PIN, null)
    }

    fun setAppTheme(theme: String) {
        sharedPreferences.edit { putString(KEY_APP_THEME, theme) }
    }

    fun getAppTheme(): String {
        return sharedPreferences.getString(KEY_APP_THEME, "System") ?: "System"
    }
    
    fun clearPin() {
        sharedPreferences.edit { remove(KEY_PIN) }
        isPinEnabled = false
    }
}