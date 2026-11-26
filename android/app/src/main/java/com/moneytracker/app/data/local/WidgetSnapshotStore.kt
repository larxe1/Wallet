package com.moneytracker.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.moneytracker.app.data.model.WidgetSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.widgetSnapshotDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "money_tracker_widget_snapshot"
)

class WidgetSnapshotStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    val snapshotFlow: Flow<WidgetSnapshot> = context.applicationContext.widgetSnapshotDataStore.data
        .map { prefs -> decodeOrDefault(prefs[KEY_SNAPSHOT]) }

    suspend fun readSnapshot(): WidgetSnapshot {
        val prefs = context.applicationContext.widgetSnapshotDataStore.data.first()
        return decodeOrDefault(prefs[KEY_SNAPSHOT])
    }

    suspend fun saveSnapshot(snapshot: WidgetSnapshot) {
        val encoded = json.encodeToString(snapshot)
        context.applicationContext.widgetSnapshotDataStore.edit { prefs ->
            prefs[KEY_SNAPSHOT] = encoded
        }
    }

    suspend fun updateSnapshot(transform: (WidgetSnapshot) -> WidgetSnapshot): WidgetSnapshot {
        var updated = WidgetSnapshot(isPrivacyMasked = true)
        context.applicationContext.widgetSnapshotDataStore.edit { prefs ->
            val current = decodeOrDefault(prefs[KEY_SNAPSHOT])
            updated = transform(current)
            prefs[KEY_SNAPSHOT] = json.encodeToString(updated)
        }
        return updated
    }

    private fun decodeOrDefault(raw: String?): WidgetSnapshot {
        if (raw.isNullOrBlank()) return WidgetSnapshot(isPrivacyMasked = true)
        return runCatching { json.decodeFromString<WidgetSnapshot>(raw) }
            .getOrDefault(WidgetSnapshot(isPrivacyMasked = true))
    }

    companion object {
        private val KEY_SNAPSHOT = stringPreferencesKey("widget_snapshot_json")
    }
}
