package nl.sdthrussell.jane.model

import kotlinx.serialization.Serializable

@Serializable
data class JaneStatus(
    val name: String = "Jane",
    val state: String = "Awake",
    val uptimeSeconds: Long = 0,
    val currentFocus: String = "Listening",
    val curiosityLevel: Double = 0.65,
    val attentionBudget: Double = 1.0,
    val activeInvestigations: Int = 0,
    val memoryCount: Int = 0,
    val edgeCount: Int = 0
)

@Serializable
data class ChatMessage(
    val id: String,
    val role: String,
    val text: String,
    val timestamp: Long
)

@Serializable
data class MemoryItem(
    val id: String,
    val summary: String,
    val value: Double,
    val novelty: Double,
    val confidence: Double,
    val status: String
)

@Serializable
data class Investigation(
    val id: String,
    val question: String,
    val status: String,
    val expectedInformationGain: Double,
    val createdAt: Long
)

@Serializable
data class Discovery(
    val id: String,
    val title: String,
    val explanation: String,
    val confidence: Double,
    val timestamp: Long
)

@Serializable
data class JaneSnapshot(
    val status: JaneStatus = JaneStatus(),
    val messages: List<ChatMessage> = emptyList(),
    val memories: List<MemoryItem> = emptyList(),
    val investigations: List<Investigation> = emptyList(),
    val discoveries: List<Discovery> = emptyList()
)

@Serializable
data class ObserveRequest(val text: String)

@Serializable
data class ObserveResponse(
    val reply: String,
    val snapshot: JaneSnapshot
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
data class VisionResponse(
    val description: String,
    val extractedText: String? = null,
    val snapshot: JaneSnapshot? = null
)


@Serializable
data class ScannedPage(val id:String,val imageUri:String,val extractedText:String,val pageNumber:Int,val confidence:Double=0.0)

@Serializable
data class DocumentAnalysisRequest(val documentId:String,val title:String,val sourceLanguage:String?=null,val targetLanguage:String="en",val pages:List<ScannedPage>,val actions:List<String> = listOf("summarize","translate","extract_deadlines","extract_amounts","explain","draft_reply"))

@Serializable
data class DocumentAnalysisResult(val documentId:String,val detectedLanguage:String,val title:String,val fullText:String,val translation:String?=null,val summary:String?=null,val explanation:String?=null,val deadlines:List<String> = emptyList(),val amounts:List<String> = emptyList(),val actionsRequired:List<String> = emptyList(),val risks:List<String> = emptyList(),val replyDraft:String?=null,val confidence:Double=0.0,val snapshot:JaneSnapshot?=null)
