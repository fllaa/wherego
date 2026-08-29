package app.wherego

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wherego.core.database.LedgerStore
import app.wherego.core.database.UserProfileStore
import app.wherego.core.datastore.ThemePreferences
import app.wherego.core.model.ThemeMode
import app.wherego.core.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userProfileStore: UserProfileStore,
    private val ledgerStore: LedgerStore,
    themePreferences: ThemePreferences,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    val themeMode: StateFlow<String> = themePreferences.mode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ThemeMode.SYSTEM,
    )

    val onboardingDone: StateFlow<Boolean> = userProfileStore.profile
        .map { it?.onboardingDone == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            userProfileStore.ensureGuest()
            ledgerStore.seedCategoriesIfEmpty()
            _ready.value = true
            syncScheduler.enqueuePeriodic()
            syncScheduler.enqueueNow()
        }
    }
}
