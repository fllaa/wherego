package com.flla.wherego.core.database

import com.flla.wherego.core.common.UlidGenerator
import com.flla.wherego.core.model.UserProfile
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
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

    suspend fun completeOnboarding(
        baseCurrency: String,
        startingBalanceMinor: Long,
        displayName: String?,
    ) {
        val existing = dao.get() ?: return
        val zone = ZoneId.of(existing.timeZoneId)
        val today = LocalDate.now(clock.withZone(zone)).toString()
        dao.update(
            existing.copy(
                baseCurrency = baseCurrency,
                startingBalanceMinor = startingBalanceMinor,
                startingBalanceOn = if (startingBalanceMinor != 0L) today else existing.startingBalanceOn,
                displayName = displayName?.trim()?.ifBlank { null } ?: existing.displayName,
                onboardingDone = true,
                updatedAt = clock.millis(),
            ),
        )
    }

    suspend fun updateDisplayName(name: String) {
        val existing = dao.get() ?: return
        dao.update(
            existing.copy(
                displayName = name.trim().ifBlank { null },
                updatedAt = clock.millis(),
            ),
        )
    }

    suspend fun linkGoogle(
        firebaseUid: String,
        googleSub: String?,
        email: String?,
        displayName: String?,
        photoUrl: String?,
    ) {
        val existing = dao.get() ?: return
        val nextSub = googleSub ?: existing.googleSub
        val nextEmail = email ?: existing.email
        val nextName = displayName?.trim()?.ifBlank { null } ?: existing.displayName
        val nextPhoto = photoUrl ?: existing.photoUrl
        if (
            existing.firebaseUid == firebaseUid &&
            existing.googleSub == nextSub &&
            existing.email == nextEmail &&
            existing.displayName == nextName &&
            existing.photoUrl == nextPhoto
        ) {
            return
        }
        dao.update(
            existing.copy(
                firebaseUid = firebaseUid,
                googleSub = nextSub,
                email = nextEmail,
                displayName = nextName,
                photoUrl = nextPhoto,
                updatedAt = clock.millis(),
            ),
        )
    }
}
