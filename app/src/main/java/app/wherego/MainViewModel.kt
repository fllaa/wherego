package app.wherego

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.wherego.core.database.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userProfileStore: UserProfileStore,
) : ViewModel() {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        viewModelScope.launch {
            userProfileStore.ensureGuest()
            _ready.value = true
        }
    }
}
