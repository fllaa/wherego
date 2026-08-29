package app.wherego.core.sync

import app.wherego.core.database.TransactionDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class CloudStatus @Inject constructor(
    auth: AuthRepository,
    transactions: TransactionDao,
    cloud: CloudDataSource,
) {
    val dot: Flow<CloudDot> = combine(
        auth.state,
        transactions.observeDirtyCount(),
    ) { state, dirty ->
        when {
            !state.signedIn -> CloudDot.Offline
            !cloud.available -> CloudDot.Offline
            dirty > 0 -> CloudDot.Pending
            else -> CloudDot.Synced
        }
    }
}
