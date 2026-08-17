package nl.sdthrussell.jane.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID
import nl.sdthrussell.jane.model.*
import nl.sdthrussell.jane.vision.JaneOcr

class JaneRepository(
    private val context: Context,
    private val store: LocalJaneStore,
    private val journal: ExperienceJournal = ExperienceJournal(context)
) {
    suspend fun initialSnapshot() = store.loadSnapshot()
    suspend fun endpoint() = store.loadEndpoint()
    suspend fun updateEndpoint(value: String) = store.saveEndpoint(value)
    suspend fun pendingExperiences() = journal.pending()
    suspend fun bearerToken() = store.loadBearerToken()
    suspend fun updateBearerToken(value: String) = store.saveBearerToken(value)

    private suspend fun api() = JaneApi(endpoint(), store.loadBearerToken())

    suspend fun refresh(): Result<JaneSnapshot> = runCatching {
        val api = api()
        val health = api.health()
        val projects = api.projects()
        val current = store.loadSnapshot()
        // Jane Alert is an optional capability so Mobile remains compatible with
        // Jane Agent builds that pre-date the public /alerts endpoint.
        val alerts = runCatching { api.alerts() }.getOrElse { current.alerts }
        current.copy(
            projects = projects,
            alerts = alerts,
            lastHealth = health,
            lastUpdated = System.currentTimeMillis()
        ).also(store::saveSnapshot)
    }

    suspend fun chat(text: String, current: JaneSnapshot): Result<Pair<String, JaneSnapshot>> = runCatching {
        val now = System.currentTimeMillis()
        journal.append(ExperienceRecord(UUID.randomUUID().toString(), "chat", text, timestamp = now))
        val response = api().chat(text).response
        val updated = current.copy(
            messages = current.messages + ChatMessage("u-$now", "user", text, now) +
                ChatMessage("j-$now", "jane", response, now + 1),
            lastUpdated = now
        )
        store.saveSnapshot(updated)
        response to updated
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
        journal.append(ExperienceRecord(UUID.randomUUID().toString(), "image", prompt, uri.toString(), System.currentTimeMillis()))
        api().analyzeVision(prompt, byteSize = file.length().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) to text
    }

    suspend fun offlineChat(text: String, current: JaneSnapshot): Pair<String, JaneSnapshot> {
        val now = System.currentTimeMillis()
        journal.append(ExperienceRecord(UUID.randomUUID().toString(), "offline_chat", text, timestamp = now))
        val reply = "I saved this locally. I will process it when Jane Agent is reachable again."
        val updated = current.copy(
            messages = current.messages + ChatMessage("u-$now", "user", text, now) +
                ChatMessage("j-$now", "jane", reply, now + 1),
            lastUpdated = now
        )
        store.saveSnapshot(updated)
        return reply to updated
    }
}
