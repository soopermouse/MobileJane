package nl.sdthrussell.jane.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.sdthrussell.jane.model.JaneSnapshot

private val Context.dataStore by preferencesDataStore(name = "jane_state")

class LocalJaneStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val crypto = JaneCrypto()
    private val snapshotKey = stringPreferencesKey("snapshot_enc_v1")
    private val legacySnapshotKey = stringPreferencesKey("snapshot")
    private val endpointKey = stringPreferencesKey("endpoint")
    private val tokenKey = stringPreferencesKey("bearer_token_enc_v1")

    suspend fun loadSnapshot(): JaneSnapshot {
        val prefs = context.dataStore.data.first()
        prefs[snapshotKey]?.let { encrypted ->
            return runCatching { json.decodeFromString<JaneSnapshot>(crypto.decryptString(encrypted)) }
                .getOrDefault(JaneSnapshot())
        }
        // One-time migration from the pre-1.6 plaintext snapshot.
        val legacy = prefs[legacySnapshotKey] ?: return JaneSnapshot()
        val snapshot = runCatching { json.decodeFromString<JaneSnapshot>(legacy) }.getOrDefault(JaneSnapshot())
        saveSnapshot(snapshot)
        context.dataStore.edit { it.remove(legacySnapshotKey) }
        return snapshot
    }

    suspend fun saveSnapshot(snapshot: JaneSnapshot) {
        val encrypted = crypto.encryptString(json.encodeToString(snapshot))
        context.dataStore.edit { prefs ->
            prefs[snapshotKey] = encrypted
            prefs.remove(legacySnapshotKey)
        }
    }

    suspend fun loadEndpoint(): String =
        context.dataStore.data.first()[endpointKey] ?: "http://10.0.2.2:8000"

    suspend fun saveEndpoint(endpoint: String) {
        context.dataStore.edit { it[endpointKey] = endpoint.trim() }
    }

    suspend fun loadBearerToken(): String {
        val encrypted = context.dataStore.data.first()[tokenKey] ?: return ""
        return runCatching { crypto.decryptString(encrypted) }.getOrDefault("")
    }

    suspend fun saveBearerToken(token: String) {
        context.dataStore.edit { prefs ->
            val cleaned = token.trim()
            if (cleaned.isBlank()) prefs.remove(tokenKey)
            else prefs[tokenKey] = crypto.encryptString(cleaned)
        }
    }
}
