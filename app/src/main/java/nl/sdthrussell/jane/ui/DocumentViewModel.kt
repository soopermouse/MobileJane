package nl.sdthrussell.jane.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.sdthrussell.jane.data.JaneRepository
import nl.sdthrussell.jane.data.LocalJaneStore
import nl.sdthrussell.jane.model.DocumentSkillResult
import nl.sdthrussell.jane.model.ScannedPage
import nl.sdthrussell.jane.vision.JaneOcr

data class DocumentUiState(
    val title: String = "Scanned document",
    val targetLanguage: String = "English",
    val pages: List<ScannedPage> = emptyList(),
    val analyzing: Boolean = false,
    val result: DocumentSkillResult? = null,
    val error: String? = null
)

class DocumentViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalJaneStore(app)
    private val ocr = JaneOcr(app)
    private val repository = JaneRepository(app, store)
    private val _state = MutableStateFlow(DocumentUiState())
    val state = _state.asStateFlow()

    fun setTitle(value: String) { _state.value = _state.value.copy(title = value) }
    fun setTargetLanguage(value: String) { _state.value = _state.value.copy(targetLanguage = value) }

    fun addPage(uri: Uri) = viewModelScope.launch {
        _state.value = _state.value.copy(analyzing = true, error = null)
        runCatching { ocr.extract(uri) }.onSuccess { text ->
            _state.value = _state.value.copy(
                pages = _state.value.pages + ScannedPage(
                    UUID.randomUUID().toString(), uri.toString(), text,
                    _state.value.pages.size + 1, if (text.isBlank()) 0.1 else 0.8
                ),
                analyzing = false
            )
        }.onFailure { _state.value = _state.value.copy(analyzing = false, error = it.message) }
    }

    fun updatePageText(id: String, text: String) {
        _state.value = _state.value.copy(pages = _state.value.pages.map { if (it.id == id) it.copy(extractedText = text) else it })
    }

    fun removePage(id: String) {
        _state.value = _state.value.copy(
            pages = _state.value.pages.filterNot { it.id == id }.mapIndexed { index, page -> page.copy(pageNumber = index + 1) }
        )
    }

    fun analyze() = viewModelScope.launch {
        val current = _state.value
        val text = current.pages.joinToString("\n\n") { "Page ${it.pageNumber}:\n${it.extractedText}" }.trim()
        if (text.isBlank()) {
            _state.value = current.copy(error = "Scan or enter text for at least one page")
            return@launch
        }
        _state.value = current.copy(analyzing = true, error = null)
        repository.analyzeDocument(text, current.targetLanguage, current.pages.firstOrNull()?.imageUri)
            .onSuccess { _state.value = _state.value.copy(analyzing = false, result = it) }
            .onFailure { _state.value = _state.value.copy(analyzing = false, error = it.message) }
    }

    fun reset() { _state.value = DocumentUiState() }
}
