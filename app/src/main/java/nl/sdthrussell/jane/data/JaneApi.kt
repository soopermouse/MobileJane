package nl.sdthrussell.jane.data

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.sdthrussell.jane.BuildConfig
import nl.sdthrussell.jane.model.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Authenticated HTTP client for Jane Agent. */
class JaneApi(
    private val baseUrl: String,
    bearerToken: String,
    clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mediaType = "application/json".toMediaType()
    private val token = bearerToken.trim()
    private val root = baseUrl.trim().trimEnd('/').also(::validateEndpoint)
    private val client = clientBuilder
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            check(token.isNotBlank()) { "Jane Agent authentication token is required" }
            chain.proceed(
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/json")
                    .build()
            )
        }
        .build()

    suspend fun health(): HealthResponse = get("/health")
    suspend fun chat(message: String, importance: Double = 0.72): ChatResponse = post("/chat", ChatRequest(message, importance))
    suspend fun projects(): List<ProjectSummary> = get("/projects")
    suspend fun alerts(): List<JaneAlertSummary> = get("/alerts")
    suspend fun saveProject(request: ProjectRequest): ProjectSummary = post("/projects", request)
    suspend fun analyzeDocument(text: String, translateTo: String?): DocumentSkillResult = post("/skills/documents", TextSkillRequest(text, translateTo))
    suspend fun analyzeVision(extractedText: String, mediaType: String? = "image/jpeg", byteSize: Int? = null): VisionSkillResult =
        post("/skills/vision", TextSkillRequest(extractedText, null, mediaType, byteSize))
    suspend fun analyzeAudio(transcript: String, mediaType: String? = "audio/wav", byteSize: Int? = null): AudioSkillResult =
        post("/skills/audio", TextSkillRequest(transcript, null, mediaType, byteSize))

    private fun validateEndpoint(value: String) {
        val url = value.toHttpUrl()
        if (url.isHttps) return
        val emulatorDevHost = BuildConfig.DEBUG && url.scheme == "http" && url.host == "10.0.2.2"
        require(emulatorDevHost) { "Jane Agent must use HTTPS. Cleartext HTTP is allowed only for 10.0.2.2 in debug builds." }
    }

    private suspend inline fun <reified T> get(path: String): T = withContext(Dispatchers.IO) {
        execute(Request.Builder().url("$root$path").get().build())
    }

    private suspend inline fun <reified Req, reified Res> post(path: String, value: Req): Res = withContext(Dispatchers.IO) {
        val body = json.encodeToString(value).toRequestBody(mediaType)
        execute(Request.Builder().url("$root$path").post(body).build())
    }

    private inline fun <reified T> execute(request: Request): T {
        client.newCall(request).execute().use { response ->
            val raw = response.body.string()
            check(response.isSuccessful) { "Jane Agent returned HTTP ${response.code}: ${raw.take(300)}" }
            return json.decodeFromString(raw)
        }
    }
}
