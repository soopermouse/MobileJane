package nl.sdthrussell.jane.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String = "unknown",
    val application: String = "Jane Agent"
)


@Serializable
data class PairRequest(val code: String, @SerialName("device_name") val deviceName: String = "J Mobile")

@Serializable
data class PairResponse(
    @SerialName("device_id") val deviceId: String,
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("agent_id") val agentId: String = "j-agent",
    @SerialName("api_base") val apiBase: String = "/v1"
)

@Serializable
data class IntakeRequest(
    val kind: String,
    val text: String,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("media_uri") val mediaUri: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class IntakeResponse(
    val experience: kotlinx.serialization.json.JsonObject? = null,
    val interpretation: kotlinx.serialization.json.JsonObject? = null,
    val integration: kotlinx.serialization.json.JsonObject? = null
)

@Serializable
data class ChatRequest(val message: String, val importance: Double = 0.72)

@Serializable
data class ChatResponse(val response: String)

@Serializable
data class TextSkillRequest(
    val text: String,
    @SerialName("translate_to") val translateTo: String? = "English",
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("byte_size") val byteSize: Int? = null
)

@Serializable
data class ProjectRequest(
    val name: String,
    val description: String = "",
    val status: String = "active"
)

@Serializable
data class ProjectSummary(
    val id: String = "",
    val name: String,
    val description: String = "",
    val status: String = "active",
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class DocumentSkillResult(
    val original: String = "",
    val summary: String = "",
    val translation: String? = null,
    val explanation: String? = null,
    val deadlines: List<String> = emptyList(),
    val amounts: List<String> = emptyList(),
    @SerialName("actions_required") val actionsRequired: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    @SerialName("reply_draft") val replyDraft: String? = null,
    @SerialName("detected_language") val detectedLanguage: String? = null,
    val confidence: Double = 0.0
)

@Serializable
data class VisionSkillResult(
    val interpretation: String = "",
    val observations: List<String> = emptyList(),
    val confidence: Double = 0.0
)

@Serializable
data class AudioSkillResult(
    val interpretation: String = "",
    val transcript: String = "",
    val confidence: Double = 0.0
)

@Serializable
data class ChatMessage(
    val id: String,
    val role: String,
    val text: String,
    val timestamp: Long
)

@Serializable
data class GoalRecord(
    val id: String,
    val title: String,
    val completed: Boolean = false,
    val createdAt: Long
)

@Serializable
data class InvestigationRecord(
    val id: String,
    val question: String,
    val status: String = "open",
    val createdAt: Long
)

@Serializable
data class CaptureRecord(
    val id: String,
    val kind: String,
    val text: String,
    val createdAt: Long,
    val synchronized: Boolean = false
)


@Serializable
data class JaneAlertSummary(
    val id: String = "",
    @SerialName("project_id") val projectId: String? = null,
    val title: String,
    val message: String = "",
    val severity: String = "info",
    val status: String = "open",
    @SerialName("created_at") val createdAt: Long = 0
)

@Serializable
data class JaneSnapshot(
    val messages: List<ChatMessage> = emptyList(),
    val projects: List<ProjectSummary> = emptyList(),
    val goals: List<GoalRecord> = emptyList(),
    val investigations: List<InvestigationRecord> = emptyList(),
    val captures: List<CaptureRecord> = emptyList(),
    val alerts: List<JaneAlertSummary> = emptyList(),
    val lastHealth: HealthResponse = HealthResponse(),
    val lastUpdated: Long = 0
)

@Serializable
data class ExperienceRecord(
    val id: String,
    val kind: String,
    val text: String,
    val localUri: String? = null,
    val timestamp: Long,
    val synchronized: Boolean = false
)

@Serializable
data class ScannedPage(
    val id: String,
    val imageUri: String,
    val extractedText: String,
    val pageNumber: Int,
    val confidence: Double = 0.0
)
