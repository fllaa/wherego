package app.wherego.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.wherego.core.model.UserProfile

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val googleSub: String?,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val baseCurrency: String,
    val localeTag: String,
    val timeZoneId: String,
    val onboardingDone: Boolean,
    val startingBalanceMinor: Long,
    val startingBalanceOn: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toModel(): UserProfile = UserProfile(
        id = id,
        googleSub = googleSub,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl,
        baseCurrency = baseCurrency,
        localeTag = localeTag,
        timeZoneId = timeZoneId,
        onboardingDone = onboardingDone,
        startingBalanceMinor = startingBalanceMinor,
        startingBalanceOn = startingBalanceOn,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun from(model: UserProfile): UserProfileEntity = UserProfileEntity(
            id = model.id,
            googleSub = model.googleSub,
            email = model.email,
            displayName = model.displayName,
            photoUrl = model.photoUrl,
            baseCurrency = model.baseCurrency,
            localeTag = model.localeTag,
            timeZoneId = model.timeZoneId,
            onboardingDone = model.onboardingDone,
            startingBalanceMinor = model.startingBalanceMinor,
            startingBalanceOn = model.startingBalanceOn,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt,
        )
    }
}
