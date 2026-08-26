package nl.sdthrussell.jane.data

import java.io.IOException
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

class JaneAuthenticationException(message: String = "Reconnect to J Agent") : IOException(message)

/** Authenticated HTTP client for J Agent. Pairing exchange is intentionally unauthenticated. */
class JaneApi(
    private val baseUrl: String,
    bearerToken: String = "",
    clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mediaType = "application/json".toMediaType()
    private val token = bearerToken.trim()
    private val root = baseUrl.trim().trimEnd('/').also(::validateEndpoint)
    private val client = clientBuilder
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun health(): HealthResponse = get("/health", authenticated = false)
    suspend fun pair(code: String, deviceName: String = "J Mobile"): PairResponse =
        post("/v1/os/pair", PairRequest(code.trim().uppercase(), deviceName), authenticated = false)
    suspend fun chat(message: String, importance: Double = 0.72): ChatResponse = post("/chat", ChatRequest(message, importance))
    suspend fun projects(): List<ProjectSummary> = get("/projects")
    suspend fun saveProject(request: ProjectRequest): ProjectSummary = post("/projects", request)
    suspend fun intake(request: IntakeRequest): IntakeResponse = post("/intake", request)

    private fun validateEndpoint(value: String) {
        val url = value.toHttpUrl()
        if (url.isHttps) return
        val emulatorDevHost = BuildConfig.DEBUG && url.scheme == "http" && url.host == "10.0.2.2"
        require(emulatorDevHost) { "J Agent must use HTTPS. Cleartext HTTP is allowed only for 10.0.2.2 in debug builds." }
    }

    private fun request(path: String, authenticated: Boolean): Request.Builder {
        val builder = Request.Builder().url("$root$path").header("Accept", "application/json")
        if (authenticated) {
            check(token.isNotBlank()) { "J Agent is not paired. Reconnect this device." }
            builder.header("Authorization", "Bearer $token")
        }
        return builder
    }

    private suspend inline fun <reified T> get(path: String, authenticated: Boolean = true): T = withContext(Dispatchers.IO) {
        execute(request(path, authenticated).get().build())
    }

    private suspend inline fun <reified Req, reified Res> post(path: String, value: Req, authenticated: Boolean = true): Res = withContext(Dispatchers.IO) {
        val body = json.encodeToString(value).toRequestBody(mediaType)
        execute(request(path, authenticated).post(body).build())
    }

    private inline fun <reified T> execute(request: Request): T {
        client.newCall(request).execute().use { response ->
            val raw = response.body.string()
            if (response.code == 401) throw JaneAuthenticationException()
            check(response.isSuccessful) { "J Agent returned HTTP ${response.code}: ${raw.take(300)}" }
            return json.decodeFromString(raw)
        }
    }
}
