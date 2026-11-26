package com.moneytracker.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.moneytracker.app.data.model.AuthSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SessionStore(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val prefs: SharedPreferences = createSecurePrefs(context.applicationContext)

    private val _sessionFlow = MutableStateFlow<AuthSession?>(readSessionInternal())
    val sessionFlow: StateFlow<AuthSession?> = _sessionFlow.asStateFlow()

    fun getSession(): AuthSession? = _sessionFlow.value ?: readSessionInternal()

    fun saveSession(session: AuthSession) {
        val encoded = json.encodeToString(session)
        prefs.edit().putString(KEY_SESSION, encoded).apply()
        _sessionFlow.value = session
    }

    fun clearSession() {
        prefs.edit().remove(KEY_SESSION).apply()
        _sessionFlow.value = null
    }

    private fun readSessionInternal(): AuthSession? {
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching { json.decodeFromString<AuthSession>(raw) }.getOrNull()
    }

    private fun createSecurePrefs(appContext: Context): SharedPreferences {
        return runCatching {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                PREFS_NAME_ENCRYPTED,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse {
            appContext.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val PREFS_NAME_ENCRYPTED = "money_tracker_secure_session"
        private const val PREFS_NAME_FALLBACK = "money_tracker_session_fallback"
        private const val KEY_SESSION = "auth_session_json"
    }
}
