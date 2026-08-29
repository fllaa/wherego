package app.wherego.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wherego.core.database.UserProfileStore
import app.wherego.core.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    store: UserProfileStore,
) : ViewModel() {
    val profile: StateFlow<UserProfile?> = store.profile.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )
}
