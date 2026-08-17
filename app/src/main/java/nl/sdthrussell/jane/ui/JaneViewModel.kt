package nl.sdthrussell.jane.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.sdthrussell.jane.data.JaneRepository
import nl.sdthrussell.jane.data.LocalJaneStore
import nl.sdthrussell.jane.model.CaptureRecord
import nl.sdthrussell.jane.model.GoalRecord
import nl.sdthrussell.jane.model.InvestigationRecord
import nl.sdthrussell.jane.model.JaneSnapshot

data class JaneUiState(
    val snapshot: JaneSnapshot = JaneSnapshot(),
    val endpoint: String = "",
    val authConfigured: Boolean = false,
    val loading: Boolean = true,
    val connected: Boolean = false,
    val voiceState: String = "Tap the microphone",
    val pendingExperiences: Int = 0,
    val error: String? = null
)

class JaneViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LocalJaneStore(application)
    private val repository = JaneRepository(application, store)
    private val _state = MutableStateFlow(JaneUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val hasAuth = repository.bearerToken().isNotBlank()
            _state.value = JaneUiState(
                snapshot = repository.initialSnapshot(),
                endpoint = repository.endpoint(),
                authConfigured = hasAuth,
                loading = false,
                pendingExperiences = repository.pendingExperiences().size
            )
            if (hasAuth) refresh()
        }
    }

    fun setVoiceState(value: String) { _state.value = _state.value.copy(voiceState = value) }
    fun clearError() { _state.value = _state.value.copy(error = null) }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        repository.refresh().onSuccess {
            _state.value = _state.value.copy(snapshot = it, loading = false, connected = true)
        }.onFailure {
            _state.value = _state.value.copy(loading = false, connected = false, error = it.message)
        }
    }

    fun send(text: String, onReply: (String) -> Unit = {}) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            repository.chat(text, _state.value.snapshot).onSuccess { (reply, snapshot) ->
                _state.value = _state.value.copy(snapshot = snapshot, loading = false, connected = true)
                onReply(reply)
            }.onFailure { failure ->
                val (reply, snapshot) = repository.offlineChat(text, _state.value.snapshot)
                _state.value = _state.value.copy(snapshot = snapshot, loading = false, connected = false, error = "Portable mode: ${failure.message}")
                onReply(reply)
            }
        }
    }

    fun addProject(name: String, description: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        _state.value = _state.value.copy(loading = true, error = null)
        repository.saveProject(name, description, _state.value.snapshot).onSuccess {
            _state.value = _state.value.copy(snapshot = it, loading = false, connected = true)
        }.onFailure {
            _state.value = _state.value.copy(loading = false, connected = false, error = it.message)
        }
    }

    fun addGoal(title: String) = updateLocal { current ->
        current.copy(goals = current.goals + GoalRecord(UUID.randomUUID().toString(), title.trim(), createdAt = System.currentTimeMillis()))
    }

    fun toggleGoal(id: String) = updateLocal { current ->
        current.copy(goals = current.goals.map { if (it.id == id) it.copy(completed = !it.completed) else it })
    }

    fun addInvestigation(question: String) = updateLocal { current ->
        current.copy(investigations = current.investigations + InvestigationRecord(UUID.randomUUID().toString(), question.trim(), createdAt = System.currentTimeMillis()))
    }

    fun capture(kind: String, text: String, sendToJane: Boolean = true) {
        if (text.isBlank()) return
        updateLocal { current ->
            current.copy(captures = current.captures + CaptureRecord(UUID.randomUUID().toString(), kind, text.trim(), System.currentTimeMillis()))
        }
        if (sendToJane) send("Mobile $kind capture: ${text.trim()}")
    }

    fun saveConnection(endpoint: String, bearerToken: String) = viewModelScope.launch {
        val cleaned = endpoint.trim().trimEnd('/')
        repository.updateEndpoint(cleaned)
        if (bearerToken.isNotBlank()) repository.updateBearerToken(bearerToken)
        _state.value = _state.value.copy(
            endpoint = cleaned,
            authConfigured = repository.bearerToken().isNotBlank()
        )
        refresh()
    }

    private fun updateLocal(transform: (JaneSnapshot) -> JaneSnapshot) = viewModelScope.launch {
        val updated = transform(_state.value.snapshot).copy(lastUpdated = System.currentTimeMillis())
        store.saveSnapshot(updated)
        _state.value = _state.value.copy(snapshot = updated)
    }
}
