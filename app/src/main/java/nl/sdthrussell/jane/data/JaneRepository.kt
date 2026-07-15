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

    suspend fun refresh(): Result<JaneSnapshot> = runCatching {
        JaneApi(endpoint()).status().also(store::saveSnapshot)
    }

    suspend fun observe(text: String): Result<ObserveResponse> = runCatching {
        journal.append(
            ExperienceRecord(
                id = UUID.randomUUID().toString(),
                kind = "speech_or_text",
                text = text,
                timestamp = System.currentTimeMillis()
            )
        )
        JaneApi(endpoint()).observe(text).also {
            store.saveSnapshot(it.snapshot)
        }
    }

    suspend fun captureImage(uri: Uri, file: File): Result<VisionResponse> =
        runCatching {
            val text = JaneOcr(context).extract(uri)
            journal.append(
                ExperienceRecord(
                    id = UUID.randomUUID().toString(),
                    kind = "image",
                    text = text.ifBlank {
                        "Image captured without readable text"
                    },
                    localUri = uri.toString(),
                    timestamp = System.currentTimeMillis()
                )
            )
            JaneApi(endpoint()).analyzeImage(file, text)
        }

    suspend fun synchronize(): Result<JaneSnapshot> = runCatching {
        val pending = journal.pending()
        if (pending.isEmpty()) {
            store.loadSnapshot()
        } else {
            JaneApi(endpoint()).syncExperiences(pending).also {
                journal.markSynchronized(pending.map { item -> item.id }.toSet())
                store.saveSnapshot(it)
            }
        }
    }

    suspend fun offlineObserve(
        text: String,
        current: JaneSnapshot
    ): ObserveResponse {
        val now = System.currentTimeMillis()
        journal.append(
            ExperienceRecord(
                id = UUID.randomUUID().toString(),
                kind = "offline_observation",
                text = text,
                timestamp = now
            )
        )

        val reply =
            "I saved this experience locally. I will integrate it into my main memory when we synchronize."

        val updated = current.copy(
            status = current.status.copy(
                state = "Portable",
                currentFocus = "Preserving today's experiences",
                memoryCount = current.status.memoryCount + 1
            ),
            messages = current.messages + listOf(
                ChatMessage("u-$now", "user", text, now),
                ChatMessage("j-$now", "jane", reply, now + 1)
            ),
            memories = listOf(
                MemoryItem(
                    id = "m-$now",
                    summary = text.take(180),
                    value = 0.55,
                    novelty = 0.65,
                    confidence = 1.0,
                    status = "queued"
                )
            ) + current.memories
        )
        store.saveSnapshot(updated)
        return ObserveResponse(reply, updated)
    }
}
