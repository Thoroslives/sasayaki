package com.sasayaki.ui.settings

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.sasayaki.data.repository.ConnectionResult
import com.sasayaki.data.repository.ProviderRepository
import com.sasayaki.di.ApplicationScope
import com.sasayaki.domain.provider.ModelInfo
import com.sasayaki.domain.provider.ModelKind
import com.sasayaki.domain.provider.Provider
import com.sasayaki.domain.provider.serverAddressError
import com.sasayaki.domain.provider.suggestProviderName
import com.sasayaki.ui.navigation.ProviderEditorDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ProviderEditorUiState(
    val loaded: Boolean = false,
    val testing: Boolean = false,
    val loadingModels: Boolean = false,
    val result: ConnectionResult? = null,
    val models: List<ModelInfo> = emptyList(),
    val urlError: String? = null,
    val finished: Boolean = false
)

/** Edits one provider. Existing providers save as they change; new ones on [add]. */
@OptIn(FlowPreview::class)
@HiltViewModel
class ProviderEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val providerRepository: ProviderRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {
    private val providerId: Long = savedStateHandle.toRoute<ProviderEditorDestination>().id
    val isNew: Boolean = providerId == 0L

    // Field drafts live in snapshot state so typing never round-trips through a flow.
    var name by mutableStateOf("")
        private set
    var url by mutableStateOf("")
        private set
    var apiKey by mutableStateOf("")
        private set
    var speechModel by mutableStateOf<String?>(null)
        private set
    var cleanupModel by mutableStateOf<String?>(null)
        private set
    private var nameEdited = false

    private val _uiState = MutableStateFlow(ProviderEditorUiState(loaded = isNew))
    val uiState: StateFlow<ProviderEditorUiState> = _uiState.asStateFlow()

    init {
        if (!isNew) {
            viewModelScope.launch {
                val provider = providerRepository.get(providerId)
                if (provider == null) {
                    _uiState.update { it.copy(finished = true) }
                    return@launch
                }
                name = provider.name
                nameEdited = true
                url = provider.baseUrl
                apiKey = provider.apiKey
                speechModel = provider.speechModel
                cleanupModel = provider.cleanupModel
                _uiState.update {
                    it.copy(loaded = true, loadingModels = true, models = providerRepository.models.value[providerId].orEmpty())
                }
                launch { autosave() }
                val models = providerRepository.refreshModels(providerId)
                _uiState.update { it.copy(loadingModels = false, models = models) }
            }
        }
    }

    fun onNameChange(value: String) {
        name = value
        nameEdited = value.isNotBlank()
    }

    fun onUrlChange(value: String) {
        url = value
        if (!nameEdited) name = suggestProviderName(value).orEmpty()
        _uiState.update { it.copy(result = null, urlError = null) }
    }

    fun onApiKeyChange(value: String) {
        apiKey = value
        _uiState.update { it.copy(result = null) }
    }

    fun onSpeechModel(model: String?) {
        speechModel = model
    }

    fun onCleanupModel(model: String?) {
        cleanupModel = model
    }

    fun test() {
        if (!validateUrl()) return
        viewModelScope.launch {
            _uiState.update { it.copy(testing = true, result = null) }
            val result = providerRepository.test(url, apiKey, speechModel, cleanupModel, providerId.takeUnless { isNew })
            val models = (result as? ConnectionResult.Connected)?.models.orEmpty()
            if (speechModel == null) speechModel = suggestSpeechModel(models)
            _uiState.update {
                it.copy(testing = false, result = result, models = models.ifEmpty { it.models })
            }
        }
    }

    fun add() {
        if (!validateUrl()) return
        viewModelScope.launch {
            providerRepository.save(draft())
            _uiState.update { it.copy(finished = true) }
        }
    }

    fun delete() {
        viewModelScope.launch {
            providerRepository.delete(providerId)
            _uiState.update { it.copy(finished = true) }
        }
    }

    override fun onCleared() {
        // The debounced save may not have run before the editor closed.
        val state = _uiState.value
        if (!isNew && state.loaded && !state.finished && serverAddressError(url) == null && url.isNotBlank()) {
            val pending = draft()
            applicationScope.launch { providerRepository.save(pending) }
        }
        super.onCleared()
    }

    private suspend fun autosave() {
        snapshotFlow { draft() }
            .drop(1)
            .debounce(SAVE_DELAY_MS)
            .collect { provider ->
                val error = serverAddressError(provider.baseUrl)
                _uiState.update { it.copy(urlError = error) }
                if (error == null && provider.baseUrl.isNotBlank() && !_uiState.value.finished) providerRepository.save(provider)
            }
    }

    private fun validateUrl(): Boolean {
        val error = if (url.isBlank()) "Enter the server address." else serverAddressError(url)
        _uiState.update { it.copy(urlError = error) }
        return error == null
    }

    private fun draft() = Provider(
        id = providerId,
        name = name.trim().ifEmpty { suggestProviderName(url) ?: "Server" },
        baseUrl = url.trim(),
        apiKey = apiKey.trim(),
        speechModel = speechModel,
        cleanupModel = cleanupModel
    )

    private companion object {
        const val SAVE_DELAY_MS = 600L
        val preferredSpeechModels = listOf("large-v3-turbo", "large-v3", "whisper-1", "gpt-4o-mini-transcribe")

        fun suggestSpeechModel(models: List<ModelInfo>): String? {
            val speech = models.filter { it.kind == ModelKind.SPEECH }.map(ModelInfo::id)
            return preferredSpeechModels.firstNotNullOfOrNull { hint -> speech.firstOrNull { hint in it.lowercase() } }
                ?: speech.firstOrNull()
        }
    }
}
