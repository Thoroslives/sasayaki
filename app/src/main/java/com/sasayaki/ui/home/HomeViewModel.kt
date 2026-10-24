package com.sasayaki.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasayaki.data.db.dao.DictationDao
import com.sasayaki.data.db.dao.LifetimeStatsDao
import com.sasayaki.data.db.entity.DictationStats
import com.sasayaki.data.db.entity.DictationSummary
import com.sasayaki.data.preferences.PreferencesDataStore
import com.sasayaki.data.repository.HistoryRepository
import com.sasayaki.data.repository.ProfileRepository
import com.sasayaki.data.repository.ProviderRepository
import com.sasayaki.domain.model.Profile
import com.sasayaki.domain.provider.Purpose
import com.sasayaki.service.BubbleServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val RECENT_LIMIT = 3
private val EMPTY_STATS = DictationStats(count = 0, wordCount = 0, durationMs = 0L)

@Immutable
data class HomeUiState(
    val loaded: Boolean = false,
    val serviceRunning: Boolean = false,
    val speechProviderConfigured: Boolean = false,
    val historyEnabled: Boolean = true,
    val profiles: List<Profile> = emptyList(),
    val today: DictationStats = EMPTY_STATS,
    val lifetime: DictationStats = EMPTY_STATS,
    val recent: List<DictationSummary> = emptyList()
) {
    val activeProfile: Profile? get() = profiles.firstOrNull { it.isActive }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    dictationDao: DictationDao,
    lifetimeStatsDao: LifetimeStatsDao,
    preferencesDataStore: PreferencesDataStore,
    historyRepository: HistoryRepository,
    private val profileRepository: ProfileRepository,
    providerRepository: ProviderRepository,
    private val serviceController: BubbleServiceController
) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val startOfToday = MutableStateFlow(startOfDay())

    private val todayStats: Flow<DictationStats> = startOfToday.flatMapLatest(dictationDao::getTodayStats)

    // Standalone counters rather than a sum over rows, so pruning or deleting history
    // never walks the lifetime totals backwards.
    private val lifetimeStats: Flow<DictationStats> = lifetimeStatsDao.observe().map { stats ->
        stats?.let { DictationStats(it.dictationCount, it.wordCount, it.durationMs) } ?: EMPTY_STATS
    }

    private val stats = combine(todayStats, lifetimeStats, historyRepository.recent(RECENT_LIMIT)) { today, lifetime, recent ->
        Triple(today, lifetime, recent)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        preferencesDataStore.preferences,
        serviceController.running,
        profileRepository.profiles,
        providerRepository.providers,
        stats
    ) { prefs, running, profiles, providers, (today, lifetime, recent) ->
        HomeUiState(
            loaded = true,
            serviceRunning = running,
            speechProviderConfigured = providers.any { it.defaultModel(Purpose.SPEECH) != null },
            historyEnabled = prefs.historyEnabled,
            profiles = profiles,
            today = today,
            lifetime = lifetime,
            recent = recent
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(millisUntilTomorrow())
                startOfToday.value = startOfDay()
            }
        }
    }

    fun startService() = serviceController.start()

    fun stopService() = serviceController.stop()

    fun activateProfile(id: Long) {
        viewModelScope.launch { profileRepository.activate(id) }
    }

    private fun startOfDay(): Long = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun millisUntilTomorrow(): Long {
        val tomorrow = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return (tomorrow - System.currentTimeMillis()).coerceAtLeast(1000L)
    }
}
