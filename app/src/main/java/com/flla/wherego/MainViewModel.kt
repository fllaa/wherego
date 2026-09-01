package com.flla.wherego

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flla.wherego.core.database.LedgerStore
import com.flla.wherego.core.database.UserProfileStore
import com.flla.wherego.core.datastore.AppLockController
import com.flla.wherego.core.datastore.ThemePreferences
import com.flla.wherego.core.model.ThemeMode
import com.flla.wherego.core.model.AppLanguage
import com.flla.wherego.core.sync.FxCacheScheduler
import com.flla.wherego.core.sync.SyncScheduler
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
    private val themePreferences: ThemePreferences,
    private val appLockController: AppLockController,
    private val syncScheduler: SyncScheduler,
    private val fxCache: FxCacheScheduler,
) : ViewModel() {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    val themeMode: StateFlow<String> = themePreferences.mode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ThemeMode.SYSTEM,
    )

    val amountsHidden: StateFlow<Boolean> = themePreferences.amountsHidden
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * `null` until the first read lands, so the first-run Sign In screen
     * (`pencil-new.pen` → `Sign In`) never flashes for a returning user.
     */
    val welcomeSeen: StateFlow<Boolean?> = themePreferences.welcomeSeen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val onboardingDone: StateFlow<Boolean> = userProfileStore.profile
        .map { it?.onboardingDone == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Whether the launch gate is up. Owned by [AppLockController]; unlocking flips it there. */
    val locked: StateFlow<Boolean> = appLockController.locked

    val language: StateFlow<String> = userProfileStore.profile
        .map { AppLanguage.parse(it?.localeTag) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.SYSTEM)

    fun setWelcomeSeen(seen: Boolean) {
        viewModelScope.launch { themePreferences.setWelcomeSeen(seen) }
    }

    init {
        viewModelScope.launch {
            // Before `_ready`, so an unlocked frame of the ledger can never slip out ahead of the
            // gate on a cold start.
            appLockController.bind()
            userProfileStore.ensureGuest()
            ledgerStore.seedCategoriesIfEmpty()
            _ready.value = true
            syncScheduler.enqueuePeriodic()
            syncScheduler.enqueueNow()
            fxCache.enqueueWeekly()
        }
    }
}
