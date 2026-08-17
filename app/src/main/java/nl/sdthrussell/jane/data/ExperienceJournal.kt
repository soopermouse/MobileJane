package nl.sdthrussell.jane.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.sdthrussell.jane.model.ExperienceRecord

private val Context.experienceStore by preferencesDataStore(name = "jane_experiences")

class ExperienceJournal(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val crypto = JaneCrypto()
    private val encryptedKey = stringPreferencesKey("records_enc_v1")
    private val legacyKey = stringPreferencesKey("records")

    suspend fun list(): List<ExperienceRecord> {
        val prefs = context.experienceStore.data.first()
        prefs[encryptedKey]?.let { encrypted ->
            return runCatching { json.decodeFromString<List<ExperienceRecord>>(crypto.decryptString(encrypted)) }
                .getOrDefault(emptyList())
        }
        val legacy = prefs[legacyKey] ?: return emptyList()
        val records = runCatching { json.decodeFromString<List<ExperienceRecord>>(legacy) }.getOrDefault(emptyList())
        save(records)
        context.experienceStore.edit { it.remove(legacyKey) }
        return records
    }

    suspend fun append(record: ExperienceRecord) = save(list() + record)
    suspend fun pending(): List<ExperienceRecord> = list().filterNot { it.synchronized }

    suspend fun markSynchronized(ids: Set<String>) {
        save(list().map { if (it.id in ids) it.copy(synchronized = true) else it })
    }

    private suspend fun save(records: List<ExperienceRecord>) {
        val encrypted = crypto.encryptString(json.encodeToString(records))
        context.experienceStore.edit { prefs ->
            prefs[encryptedKey] = encrypted
            prefs.remove(legacyKey)
        }
    }
}
