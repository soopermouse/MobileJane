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
    private val snapshotKey = stringPreferencesKey("snapshot")
    private val endpointKey = stringPreferencesKey("endpoint")

    suspend fun loadSnapshot(): JaneSnapshot {
        val raw = context.dataStore.data.first()[snapshotKey] ?: return JaneSnapshot()
        return runCatching { json.decodeFromString<JaneSnapshot>(raw) }.getOrDefault(JaneSnapshot())
    }

    suspend fun saveSnapshot(snapshot: JaneSnapshot) {
        context.dataStore.edit { it[snapshotKey] = json.encodeToString(snapshot) }
    }

    suspend fun loadEndpoint(): String =
        context.dataStore.data.first()[endpointKey] ?: "http://10.0.2.2:8787"

    suspend fun saveEndpoint(endpoint: String) {
        context.dataStore.edit { it[endpointKey] = endpoint.trim() }
    }
}
