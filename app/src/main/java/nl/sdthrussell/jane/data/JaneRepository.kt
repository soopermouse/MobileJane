package nl.sdthrussell.jane.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlinx.serialization.json.Json
import nl.sdthrussell.jane.model.*
import nl.sdthrussell.jane.vision.JaneOcr

class JaneRepository(
    private val context: Context,
    private val store: LocalJaneStore,
    private val journal: ExperienceJournal = ExperienceJournal(context)
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun initialSnapshot() = store.loadSnapshot()
    suspend fun endpoint() = store.loadEndpoint()
    suspend fun updateEndpoint(value: String) = store.saveEndpoint(value)
    suspend fun pendingExperiences() = journal.pending()
    suspend fun bearerToken() = store.loadBearerToken()
    suspend fun clearBearerToken() = store.saveBearerToken("")

    private suspend fun api() = JaneApi(endpoint(), store.loadBearerToken())

    /** Exchanges the human-readable one-time code for a device credential. */
    suspend fun pair(endpoint: String, code: String): Result<Unit> = runCatching {
        val cleanEndpoint = endpoint.trim().trimEnd('/')
        val response = JaneApi(cleanEndpoint).pair(code)
        store.saveEndpoint(cleanEndpoint)
        store.saveBearerToken(response.accessToken)
    }

    /** Drain encrypted offline experiences through the single typed-intake seam. */
    suspend fun synchronizePending(): Result<Int> = runCatching {
        val pending = journal.pending()
        if (pending.isEmpty()) return@runCatching 0
        val client = api()
        val synchronized = linkedSetOf<String>()
        for (record in pending) {
            client.intake(
                IntakeRequest(
                    kind = record.kind,
                    text = record.text,
                    mediaUri = record.localUri,
                    metadata = mapOf("mobile_experience_id" to record.id, "captured_at_ms" to record.timestamp.toString())
                )
            )
            synchronized += record.id
            // Commit progress after each accepted experience so a later network
            // failure does not cause already-imported records to be replayed.
            journal.markSynchronized(setOf(record.id))
        }
        synchronized.size
    }

    suspend fun refresh(): Result<JaneSnapshot> = runCatching {
        val client = api()
        val health = client.health()
        val projects = client.projects()
        synchronizePending().getOrThrow()
        store.loadSnapshot().copy(
            projects = projects,
            // J Alert is a separate product; J Agent does not expose /alerts.
            lastHealth = health,
            lastUpdated = System.currentTimeMillis()
        ).also(store::saveSnapshot)
    }

    suspend fun chat(text: String, current: JaneSnapshot): Result<Pair<String, JaneSnapshot>> {
        val now = System.currentTimeMillis()
        val record = ExperienceRecord(UUID.randomUUID().toString(), "chat", text, timestamp = now)
        journal.append(record)
        return runCatching {
            val response = api().chat(text).response
            journal.markSynchronized(setOf(record.id))
            val updated = current.copy(
                messages = current.messages + ChatMessage("u-$now", "user", text, now) +
                    ChatMessage("j-$now", "jane", response, now + 1),
                lastUpdated = now
            )
            store.saveSnapshot(updated)
            response to updated
        }
    }

    suspend fun saveProject(name: String, description: String, current: JaneSnapshot): Result<JaneSnapshot> = runCatching {
        val saved = api().saveProject(ProjectRequest(name.trim(), description.trim()))
        val projects = current.projects.filterNot { it.id == saved.id || it.name.equals(saved.name, true) } + saved
        current.copy(projects = projects.sortedBy { it.name.lowercase() }, lastUpdated = System.currentTimeMillis())
            .also(store::saveSnapshot)
    }

    suspend fun analyzeImage(uri: Uri, file: File): Result<Pair<VisionSkillResult, String>> = runCatching {
        val text = JaneOcr(context).extract(uri)
        val prompt = text.ifBlank { "A mobile image was captured, but local OCR found no readable text." }
        val record = ExperienceRecord(UUID.randomUUID().toString(), "vision", prompt, uri.toString(), System.currentTimeMillis())
        journal.append(record)
        val response = api().intake(IntakeRequest(
            kind = "vision", text = prompt, mediaUri = uri.toString(),
            metadata = mapOf("media_type" to "image/jpeg", "byte_size" to file.length().toString(), "mobile_experience_id" to record.id)
        ))
        journal.markSynchronized(setOf(record.id))
        val result = response.interpretation?.let { json.decodeFromJsonElement(VisionSkillResult.serializer(), it) } ?: VisionSkillResult()
        result to text
    }

    suspend fun analyzeDocument(text: String, targetLanguage: String, mediaUri: String? = null): Result<DocumentSkillResult> = runCatching {
        val record = ExperienceRecord(UUID.randomUUID().toString(), "document", text, mediaUri, System.currentTimeMillis())
        journal.append(record)
        val response = api().intake(IntakeRequest(
            kind = "document", text = text, mediaUri = mediaUri,
            metadata = mapOf("translate_to" to targetLanguage, "mobile_experience_id" to record.id)
        ))
        journal.markSynchronized(setOf(record.id))
        response.interpretation?.let { json.decodeFromJsonElement(DocumentSkillResult.serializer(), it) } ?: DocumentSkillResult(original = text)
    }

    suspend fun analyzeAudio(transcript: String, mediaUri: String? = null): Result<AudioSkillResult> = runCatching {
        val record = ExperienceRecord(UUID.randomUUID().toString(), "audio", transcript, mediaUri, System.currentTimeMillis())
        journal.append(record)
        val response = api().intake(IntakeRequest(
            kind = "audio", text = transcript, mediaUri = mediaUri,
            metadata = mapOf("media_type" to "audio/wav", "mobile_experience_id" to record.id)
        ))
        journal.markSynchronized(setOf(record.id))
        response.interpretation?.let { json.decodeFromJsonElement(AudioSkillResult.serializer(), it) } ?: AudioSkillResult(transcript = transcript)
    }

    suspend fun offlineChat(text: String, current: JaneSnapshot): Pair<String, JaneSnapshot> {
        val now = System.currentTimeMillis()
        // chat() already journaled the failed turn; do not duplicate it here.
        val reply = "I saved this locally. I will process it when J Agent is reachable again."
        val updated = current.copy(
            messages = current.messages + ChatMessage("u-$now", "user", text, now) +
                ChatMessage("j-$now", "jane", reply, now + 1),
            lastUpdated = now
        )
        store.saveSnapshot(updated)
        return reply to updated
    }
}
