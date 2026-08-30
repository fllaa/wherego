package com.flla.wherego.core.model

data class UserProfile(
    val id: String,
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
    val firebaseUid: String? = null,
) {
    companion object {
        const val DEFAULT_CURRENCY = "IDR"
        const val DEFAULT_LOCALE = "id-ID"
        const val DEFAULT_ZONE = "Asia/Jakarta"

        fun guest(id: String, nowMillis: Long): UserProfile = UserProfile(
            id = id,
            googleSub = null,
            email = null,
            displayName = null,
            photoUrl = null,
            baseCurrency = DEFAULT_CURRENCY,
            localeTag = DEFAULT_LOCALE,
            timeZoneId = DEFAULT_ZONE,
            onboardingDone = false,
            startingBalanceMinor = 0L,
            startingBalanceOn = null,
            createdAt = nowMillis,
            updatedAt = nowMillis,
            firebaseUid = null,
        )
    }
}
