package com.sasayaki.ui.profiles

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.sasayaki.data.repository.ProcessingRepository
import com.sasayaki.data.repository.ProfileRepository
import com.sasayaki.data.repository.ProviderRepository
import com.sasayaki.di.ApplicationScope
import com.sasayaki.domain.model.PostProcessingPrompt
import com.sasayaki.domain.model.Profile
import com.sasayaki.domain.model.TextReplacementRule
import com.sasayaki.domain.provider.ModelInfo
import com.sasayaki.domain.provider.Provider
import com.sasayaki.ui.navigation.ProfileEditorDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ProfileEditorUiState(
    val providers: List<Provider> = emptyList(),
    val rules: List<TextReplacementRule> = emptyList(),
    val customPrompts: List<PostProcessingPrompt> = emptyList(),
    val profileCount: Int = 0,
    val active: Boolean = false,
    val models: Map<Long, List<ModelInfo>> = emptyMap(),
    val loadingModels: Set<Long> = emptySet()
)

/** ProfileEditorViewModel saves each change to its profile shortly after it is made. */
@OptIn(FlowPreview::class)
@HiltViewModel
class ProfileEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
    private val providerRepository: ProviderRepository,
    processingRepository: ProcessingRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {
    private val profileId: Long = savedStateHandle.toRoute<ProfileEditorDestination>().id

    // The draft is snapshot state so text fields bound to it never lag a flow.
    var profile by mutableStateOf<Profile?>(null)
        private set

    // synced is the last version known to match the database; the draft differs from it
    // only while the user has unsaved edits.
    private var synced: Profile? = null

    private val loadingModels = MutableStateFlow<Set<Long>>(emptySet())
    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished

    val uiState: StateFlow<ProfileEditorUiState> = combine(
        combine(providerRepository.providers, providerRepository.models, loadingModels, ::Triple),
        processingRepository.rules,
        processingRepository.prompts,
        profileRepository.profiles
    ) { (providers, models, loading), rules, prompts, profiles ->
        ProfileEditorUiState(
            providers = providers,
            rules = rules,
            customPrompts = prompts.filterNot(PostProcessingPrompt::builtIn),
            profileCount = profiles.size,
            active = profiles.firstOrNull { it.id == profileId }?.isActive == true,
            models = models,
            loadingModels = loading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileEditorUiState())

    init {
        viewModelScope.launch {
            val loaded = profileRepository.getProfile(profileId) ?: run {
                _finished.value = true
                return@launch
            }
            synced = loaded
            profile = loaded
            launch {
                // Changes made elsewhere (a deleted provider, an edit from another screen)
                // replace the draft when it has nothing unsaved.
                profileRepository.profiles
                    .map { list -> list.firstOrNull { it.id == profileId } }
                    .filterNotNull()
                    .collect { stored ->
                        if (profile == synced) {
                            synced = stored
                            profile = stored
                        }
                    }
            }
            snapshotFlow { profile }
                .filterNotNull()
                .filter { it != synced }
                .debounce(SAVE_DELAY_MS)
                .collect { draft ->
                    profileRepository.save(draft)
                    synced = draft
                }
        }
    }

    fun update(transform: (Profile) -> Profile) {
        profile = profile?.let(transform)
    }

    fun loadModels(providerId: Long) {
        if (providerId in loadingModels.value) return
        viewModelScope.launch {
            loadingModels.update { it + providerId }
            providerRepository.refreshModels(providerId)
            loadingModels.update { it - providerId }
        }
    }

    fun activate() {
        viewModelScope.launch { profileRepository.activate(profileId) }
    }

    fun duplicate(onCreated: (Long) -> Unit) {
        val current = profile ?: return
        viewModelScope.launch {
            profileRepository.save(current)
            onCreated(profileRepository.duplicate(current))
        }
    }

    fun delete() {
        viewModelScope.launch {
            profileRepository.delete(profileId)
            _finished.value = true
        }
    }

    override fun onCleared() {
        // Flush an edit made within the debounce window before the editor closed.
        val pending = profile
        if (pending != null && pending != synced && !_finished.value) applicationScope.launch { profileRepository.save(pending) }
    }

    private companion object {
        const val SAVE_DELAY_MS = 500L
    }
}

