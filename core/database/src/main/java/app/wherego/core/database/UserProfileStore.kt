package app.wherego.core.database

import app.wherego.core.common.UlidGenerator
import app.wherego.core.model.UserProfile
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class UserProfileStore @Inject constructor(
    private val dao: UserProfileDao,
    private val ulid: UlidGenerator,
    private val clock: Clock,
) {
    val profile: Flow<UserProfile?> = dao.observe().map { it?.toModel() }

    suspend fun ensureGuest(): UserProfile {
        dao.get()?.toModel()?.let { return it }
        val created = UserProfile.guest(id = ulid.next(), nowMillis = clock.millis())
        dao.insert(UserProfileEntity.from(created))
        return created
    }
}
