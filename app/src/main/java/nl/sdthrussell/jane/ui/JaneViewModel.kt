package nl.sdthrussell.jane.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.sdthrussell.jane.data.JaneRepository
import nl.sdthrussell.jane.data.LocalJaneStore
import nl.sdthrussell.jane.model.JaneSnapshot

data class JaneUiState(
    val snapshot: JaneSnapshot = JaneSnapshot(),
    val endpoint: String = "",
    val loading: Boolean = true,
    val connected: Boolean = false,
    val voiceState: String = "Tap the microphone",
    val pendingExperiences: Int = 0,
    val error: String? = null
)

class JaneViewModel(application: Application) : AndroidViewModel(application) {
    private val repository =
        JaneRepository(application, LocalJaneStore(application))

    private val _state = MutableStateFlow(JaneUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = JaneUiState(
                snapshot = repository.initialSnapshot(),
                endpoint = repository.endpoint(),
                loading = false,
                pendingExperiences = repository.pendingExperiences().size
            )
            refresh()
        }
    }

    fun setVoiceState(value: String) {
        _state.value = _state.value.copy(voiceState = value)
    }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        repository.refresh()
            .onSuccess {
                _state.value = _state.value.copy(
                    snapshot = it,
                    loading = false,
                    connected = true,
                    pendingExperiences = repository.pendingExperiences().size
                )
            }
            .onFailure {
                _state.value = _state.value.copy(
                    loading = false,
                    connected = false,
                    error = it.message
                )
            }
    }

    fun send(text: String, onReply: (String) -> Unit = {}) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            repository.observe(text)
                .onSuccess {
                    _state.value = _state.value.copy(
                        snapshot = it.snapshot,
                        loading = false,
                        connected = true,
                        pendingExperiences =
                            repository.pendingExperiences().size
                    )
                    onReply(it.reply)
                }
                .onFailure {
                    val result =
                        repository.offlineObserve(text, _state.value.snapshot)
                    _state.value = _state.value.copy(
                        snapshot = result.snapshot,
                        loading = false,
                        connected = false,
                        pendingExperiences =
                            repository.pendingExperiences().size,
                        error = "Portable mode: ${it.message}"
                    )
                    onReply(result.reply)
                }
        }
    }

    fun analyzeImage(
        uri: Uri,
        file: File,
        onReply: (String) -> Unit = {}
    ) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        repository.captureImage(uri, file)
            .onSuccess {
                val reply = buildString {
                    append(it.description)
                    if (!it.extractedText.isNullOrBlank()) {
                        append("\n\nText found:\n${it.extractedText}")
                    }
                }
                _state.value = _state.value.copy(
                    snapshot = it.snapshot ?: _state.value.snapshot,
                    loading = false,
                    connected = true,
                    pendingExperiences =
                        repository.pendingExperiences().size
                )
                onReply(reply)
            }
            .onFailure {
                _state.value = _state.value.copy(
                    loading = false,
                    connected = false,
                    pendingExperiences =
                        repository.pendingExperiences().size,
                    error = "Image saved locally: ${it.message}"
                )
            }
    }

    fun synchronize() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true)
        repository.synchronize()
            .onSuccess {
                _state.value = _state.value.copy(
                    snapshot = it,
                    loading = false,
                    connected = true,
                    pendingExperiences = 0
                )
            }
            .onFailure {
                _state.value = _state.value.copy(
                    loading = false,
                    connected = false,
                    error = "Synchronization failed: ${it.message}"
                )
            }
    }

    fun saveEndpoint(value: String) = viewModelScope.launch {
        repository.updateEndpoint(value)
        _state.value = _state.value.copy(endpoint = value)
        refresh()
    }
}
