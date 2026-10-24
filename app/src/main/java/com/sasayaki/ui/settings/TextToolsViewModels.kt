package com.sasayaki.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasayaki.data.repository.ProcessingRepository
import com.sasayaki.domain.model.PostProcessingPrompt
import com.sasayaki.domain.model.TextReplacementRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReplacementRulesViewModel @Inject constructor(
    private val processingRepository: ProcessingRepository
) : ViewModel() {
    val rules: StateFlow<List<TextReplacementRule>?> = processingRepository.rules
        .map<List<TextReplacementRule>, List<TextReplacementRule>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun save(rule: TextReplacementRule) {
        viewModelScope.launch { processingRepository.saveRule(rule) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { processingRepository.deleteRule(id) }
    }
}

@HiltViewModel
class PromptsViewModel @Inject constructor(
    private val processingRepository: ProcessingRepository
) : ViewModel() {
    val prompts: StateFlow<List<PostProcessingPrompt>?> = processingRepository.prompts
        .map<List<PostProcessingPrompt>, List<PostProcessingPrompt>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun save(prompt: PostProcessingPrompt) {
        viewModelScope.launch { processingRepository.savePrompt(prompt) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { processingRepository.deletePrompt(id) }
    }
}
