package com.sasayaki.ui.settings

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.sasayaki.data.repository.ConnectionResult
import com.sasayaki.data.repository.ProviderRepository
import com.sasayaki.di.ApplicationScope
import com.sasayaki.domain.provider.ModelInfo
import com.sasayaki.domain.provider.serverAddressError
import com.sasayaki.ui.navigation.ProviderEditorDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
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
    val draft = ProviderDraft(providerRepository, viewModelScope, savedStateHandle.toRoute<ProviderEditorDestination>().id)
    val isNew: Boolean = draft.id == 0L
    val uiState: StateFlow<ProviderEditorUiState> = draft.state

    init {
        if (!isNew) {
            viewModelScope.launch {
                if (!draft.load()) {
                    draft.markFinished()
                    return@launch
                }
                autosave()
            }
        }
    }

    fun add() {
        if (!draft.validateUrl()) return
        viewModelScope.launch {
            providerRepository.save(draft.toProvider())
            draft.markFinished()
        }
    }

    fun delete() {
        viewModelScope.launch {
            providerRepository.delete(draft.id)
            draft.markFinished()
        }
    }

    override fun onCleared() {
        // The debounced save may not have run before the editor closed.
        val state = uiState.value
        if (!isNew && state.loaded && !state.finished && draft.url.isNotBlank() && serverAddressError(draft.url) == null) {
            val pending = draft.toProvider()
            applicationScope.launch { providerRepository.save(pending) }
        }
        super.onCleared()
    }

    private suspend fun autosave() {
        snapshotFlow { draft.toProvider() }
            .drop(1)
            .debounce(SAVE_DELAY_MS)
            .collect { provider ->
                val error = serverAddressError(provider.baseUrl)
                draft.showUrlError(error)
                if (error == null && provider.baseUrl.isNotBlank() && !uiState.value.finished) providerRepository.save(provider)
            }
    }

    private companion object {
        const val SAVE_DELAY_MS = 600L
    }
}
