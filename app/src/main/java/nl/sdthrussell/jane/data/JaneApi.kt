package nl.sdthrussell.jane.data

import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.sdthrussell.jane.model.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class JaneApi(
    private val baseUrl: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json".toMediaType()

    suspend fun status(): JaneSnapshot = withContext(Dispatchers.IO) {
        execute(
            Request.Builder()
                .url("${baseUrl.trimEnd('/')}/v1/jane/snapshot")
                .get()
                .build()
        )
    }

    suspend fun observe(text: String): ObserveResponse =
        withContext(Dispatchers.IO) {
            val body = json.encodeToString(ObserveRequest(text))
                .toRequestBody(mediaType)
            execute(
                Request.Builder()
                    .url("${baseUrl.trimEnd('/')}/v1/jane/observe")
                    .post(body)
                    .build()
            )
        }

    suspend fun analyzeImage(
        file: File,
        extractedText: String?
    ): VisionResponse = withContext(Dispatchers.IO) {
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                file.name,
                file.asRequestBody("image/jpeg".toMediaType())
            )
            .apply {
                if (!extractedText.isNullOrBlank()) {
                    addFormDataPart("extracted_text", extractedText)
                }
            }
            .build()

        execute(
            Request.Builder()
                .url("${baseUrl.trimEnd('/')}/v1/jane/vision")
                .post(multipart)
                .build()
        )
    }

    suspend fun syncExperiences(
        records: List<ExperienceRecord>
    ): JaneSnapshot = withContext(Dispatchers.IO) {
        val body = json.encodeToString(records).toRequestBody(mediaType)
        execute(
            Request.Builder()
                .url("${baseUrl.trimEnd('/')}/v1/jane/experiences/sync")
                .post(body)
                .build()
        )
    }

    suspend fun analyzeDocument(requestData: DocumentAnalysisRequest): DocumentAnalysisResult = withContext(Dispatchers.IO) {
        val body = json.encodeToString(requestData).toRequestBody(mediaType)
        execute(Request.Builder().url("${baseUrl.trimEnd('/')}/v1/jane/documents/analyze").post(body).build())
    }

    private inline fun <reified T> execute(request: Request): T {
        client.newCall(request).execute().use { response ->
            require(response.isSuccessful) {
                "Jane bridge returned ${response.code}"
            }
            return json.decodeFromString(response.body.string())
        }
    }
}
