package com.sasayaki

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasayaki.data.StartupMigrations
import com.sasayaki.data.preferences.PreferencesDataStore
import com.sasayaki.data.preferences.ThemeMode
import com.sasayaki.data.repository.ProviderRepository
import com.sasayaki.ui.navigation.HomeDestination
import com.sasayaki.ui.navigation.OnboardingDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Immutable
data class Appearance(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val wallpaperColors: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesDataStore: PreferencesDataStore,
    providerRepository: ProviderRepository,
    startupMigrations: StartupMigrations
) : ViewModel() {
    /**
     * startDestination is decided once per launch, so finishing onboarding does not swap
     * the graph under the user. Installs that already had a server configured skip it.
     */
    val startDestination: StateFlow<Any?> = flow {
        startupMigrations.await()
        val prefs = preferencesDataStore.preferences.first()
        val hasProvider = providerRepository.getAll().isNotEmpty()
        emit(if (prefs.onboardingCompleted || hasProvider) HomeDestination else OnboardingDestination)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val appearance: StateFlow<Appearance> = preferencesDataStore.preferences
        .map { Appearance(it.themeMode, it.wallpaperColors) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Appearance())
}
