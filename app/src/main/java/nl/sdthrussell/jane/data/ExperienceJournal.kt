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
    private val key = stringPreferencesKey("records")

    suspend fun list(): List<ExperienceRecord> {
        val raw = context.experienceStore.data.first()[key] ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<ExperienceRecord>>(raw)
        }.getOrDefault(emptyList())
    }

    suspend fun append(record: ExperienceRecord) {
        context.experienceStore.edit {
            it[key] = json.encodeToString(list() + record)
        }
    }

    suspend fun pending(): List<ExperienceRecord> = list().filterNot { it.synchronized }

    suspend fun markSynchronized(ids: Set<String>) {
        val updated = list().map {
            if (it.id in ids) it.copy(synchronized = true) else it
        }
        context.experienceStore.edit { it[key] = json.encodeToString(updated) }
    }
}
