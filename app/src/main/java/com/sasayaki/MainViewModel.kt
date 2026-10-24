package com.sasayaki

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasayaki.data.preferences.PreferencesDataStore
import com.sasayaki.data.preferences.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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
    preferencesDataStore: PreferencesDataStore
) : ViewModel() {
    val appearance: StateFlow<Appearance> = preferencesDataStore.preferences
        .map { Appearance(it.themeMode, it.wallpaperColors) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Appearance())
}
