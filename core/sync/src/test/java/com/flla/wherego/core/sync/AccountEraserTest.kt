package com.flla.wherego.core.sync

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.flla.wherego.core.database.LocalDataEraser
import com.flla.wherego.core.datastore.ThemePreferences
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.UserProfile
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountEraserTest {
    private lateinit var context: Context
    private lateinit var preferences: ThemePreferences
    private lateinit var syncScheduler: SyncScheduler
    private lateinit var fxCache: FxCacheScheduler
    private lateinit var activity: Activity

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        preferences = ThemePreferences(context)
        syncScheduler = SyncScheduler(context)
        fxCache = FxCacheScheduler(context)
        activity = Robolectric.buildActivity(Activity::class.java).get()
    }

    @Test
    fun eraseHappyPathPurgesCloudThenDevice() = runBlocking {
        val log = mutableListOf<String>()
        val auth = RecordingAuth(log, uid = "uid-1")
        val cloud = RecordingCloud(log, failDelete = false)
        val local = RecordingLocal(log)
        val eraser = AccountEraser(
            auth = auth,
            cloud = cloud,
            receipts = NoopReceipts(),
            local = local,
            preferences = preferences,
            syncScheduler = syncScheduler,
            fxCache = fxCache,
        )

        preferences.setWelcomeSeen(true)
        assertTrue(preferences.welcomeSeen.first())

        val result = eraser.erase(activity)
        assertTrue(result.isSuccess)
        assertEquals(listOf("cloud", "authDelete", "signOut", "local"), log)
        assertFalse(preferences.welcomeSeen.first())
    }

    @Test
    fun eraseCloudFailureAbortsWithoutTouchingDevice() = runBlocking {
        val log = mutableListOf<String>()
        val auth = RecordingAuth(log, uid = "uid-1")
        val cloud = RecordingCloud(log, failDelete = true)
        val local = RecordingLocal(log)
        val eraser = AccountEraser(
            auth = auth,
            cloud = cloud,
            receipts = NoopReceipts(),
            local = local,
            preferences = preferences,
            syncScheduler = syncScheduler,
            fxCache = fxCache,
        )

        preferences.setWelcomeSeen(true)
        assertTrue(preferences.welcomeSeen.first())

        val result = eraser.erase(activity)
        assertTrue(result.isFailure)
        assertFalse(log.contains("local"))
        assertFalse(log.contains("authDelete"))
        assertTrue(preferences.welcomeSeen.first())
    }

    @Test
    fun eraseGuestPathWipesDeviceOnly() = runBlocking {
        val log = mutableListOf<String>()
        val auth = RecordingAuth(log, uid = null)
        val cloud = RecordingCloud(log, failDelete = false)
        val local = RecordingLocal(log)
        val eraser = AccountEraser(
            auth = auth,
            cloud = cloud,
            receipts = NoopReceipts(),
            local = local,
            preferences = preferences,
            syncScheduler = syncScheduler,
            fxCache = fxCache,
        )

        preferences.setWelcomeSeen(true)

        val result = eraser.erase(activity)
        assertTrue(result.isSuccess)
        assertEquals(listOf("signOut", "local"), log)
        assertFalse(preferences.welcomeSeen.first())
    }
}

private class RecordingLocal(private val log: MutableList<String>) : LocalDataEraser {
    override suspend fun resetToGuest() {
        log.add("local")
    }
}

private class RecordingAuth(
    private val log: MutableList<String>,
    private val uid: String? = "uid-1",
) : AuthRepository {
    private val snap = AuthState(firebaseUid = uid, signedIn = uid != null)
    override val state: Flow<AuthState> = MutableStateFlow(snap)
    override suspend fun current(): AuthState = snap
    override suspend fun signIn(activity: Activity): Result<AuthState> = Result.success(snap)
    override suspend fun signOut() {
        log.add("signOut")
    }
    override suspend fun deleteAccount(activity: Activity): Result<Unit> {
        log.add("authDelete")
        return Result.success(Unit)
    }
}

private class RecordingCloud(
    private val log: MutableList<String>,
    var failDelete: Boolean = false,
) : CloudDataSource {
    override val available: Boolean = true
    override suspend fun pushTransactions(uid: String, rows: List<Transaction>) = Unit
    override suspend fun pullTransactions(uid: String, sinceEpoch: Long): List<Transaction> = emptyList()
    override suspend fun pushCategories(uid: String, rows: List<Category>) = Unit
    override suspend fun pullCategories(uid: String, sinceEpoch: Long): List<Category> = emptyList()
    override suspend fun pushProfile(uid: String, profile: UserProfile) = Unit
    override suspend fun pullProfile(uid: String, sinceEpoch: Long): UserProfile? = null
    override suspend fun deleteAll(uid: String) {
        if (failDelete) throw RuntimeException("Cloud delete failed")
        log.add("cloud")
    }
}

private class NoopReceipts : ReceiptUploader {
    override suspend fun upload(uid: String, receiptId: String, file: File): String? = null
    override suspend fun deleteAll(uid: String) = Unit
}
