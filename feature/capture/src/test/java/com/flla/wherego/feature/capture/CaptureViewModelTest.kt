package com.flla.wherego.feature.capture

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.flla.wherego.core.common.UlidGenerator
import com.flla.wherego.core.database.DatabaseModule
import com.flla.wherego.core.database.FxRateStore
import com.flla.wherego.core.database.LedgerStore
import com.flla.wherego.core.database.ReceiptStore
import com.flla.wherego.core.database.UserProfileStore
import com.flla.wherego.core.database.WheregoDatabase
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.TransactionKind
import com.flla.wherego.core.sync.ReceiptUploadScheduler
import com.flla.wherego.core.sync.SyncScheduler
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CaptureViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: WheregoDatabase
    private lateinit var ledger: LedgerStore
    private lateinit var profiles: UserProfileStore
    private lateinit var fxRates: FxRateStore
    private lateinit var receipts: ReceiptStore
    private lateinit var ocr: ReceiptOcr
    private lateinit var upload: ReceiptUploadScheduler
    private lateinit var syncScheduler: SyncScheduler
    private lateinit var ulid: UlidGenerator
    private val clock: Clock = Clock.fixed(Instant.parse("2026-08-30T10:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, WheregoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        ulid = UlidGenerator()
        ledger = LedgerStore(db.categoryDao(), db.transactionDao(), ulid, clock)
        profiles = UserProfileStore(db.userProfileDao(), ulid, clock)
        fxRates = FxRateStore(db.fxRateDao(), clock)
        receipts = ReceiptStore(context, db.receiptDao(), ledger, ulid, clock)
        ocr = ReceiptOcr(context)
        upload = ReceiptUploadScheduler(context)
        syncScheduler = SyncScheduler(context)
        runBlocking {
            profiles.ensureGuest()
            ledger.seedCategoriesIfEmpty()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun createViewModel(): CaptureViewModel = CaptureViewModel(
        ledger = ledger,
        profiles = profiles,
        syncScheduler = syncScheduler,
        fxRates = fxRates,
        receipts = receipts,
        ocr = ocr,
        upload = upload,
        ulid = ulid,
    )

    @Test
    fun beginCreateInitializesDraftIdAndEmptyReceipt() = runBlocking {
        val vm = createViewModel()
        vm.beginCreate()
        var attempts = 0
        while (vm.state.value.draftId.isBlank() && attempts < 50) {
            testDispatcher.scheduler.advanceUntilIdle()
            Thread.sleep(10)
            attempts++
        }
        val state = vm.state.value
        assertTrue(state.draftId.isNotBlank())
        assertNull(state.editingId)
        assertNull(state.receiptId)
        assertFalse(state.hasReceipt)
        assertFalse(state.isReadingOcr)
        assertNull(state.ocrSuggestedAmount)
    }

    @Test
    fun ocrAmountSuggestionApplyAndDismiss() = runBlocking {
        val vm = createViewModel()
        vm.beginCreate()
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onDigit("3")
        vm.onDigit("0")
        vm.onDigit("0")
        vm.onDigit("0")
        vm.onDigit("0")
        assertEquals(30_000L, vm.state.value.amountMinor)

        // Dismiss OCR suggestion
        vm.dismissSuggestedOcrAmount()
        assertNull(vm.state.value.ocrSuggestedAmount)

        // Remove receipt
        vm.removeReceipt()
        assertNull(vm.state.value.receiptId)
        assertFalse(vm.state.value.hasReceipt)
    }
    @Test
    fun savePersistsDraftWithReceiptId() = runBlocking {
        val vm = createViewModel()
        vm.beginCreate()
        var initAttempts = 0
        while (vm.state.value.draftId.isBlank() && initAttempts < 50) {
            testDispatcher.scheduler.advanceUntilIdle()
            Thread.sleep(10)
            initAttempts++
        }
        vm.onDigit("4")
        vm.onDigit("5")
        vm.onDigit("0")
        vm.onDigit("0")
        vm.onDigit("0")
        vm.onCategory("cat_food")
        assertTrue(vm.state.value.canSave)
        var savedTx: Transaction? = null
        vm.save { tx -> savedTx = tx }
        var saveAttempts = 0
        while (savedTx == null && saveAttempts < 50) {
            testDispatcher.scheduler.advanceUntilIdle()
            Thread.sleep(10)
            saveAttempts++
        }
        assertNotNull(savedTx)
        assertEquals(45_000L, savedTx?.amountMinor)
        assertEquals("cat_food", savedTx?.categoryId)
        val inDb = ledger.getTransaction(savedTx!!.id)
        assertNotNull(inDb)
        assertEquals(savedTx!!.id, inDb?.id)
    }

    /**
     * The `Balance` tab asserts a total. There is no category to pick, so `canSave` cannot demand
     * one — but a blank buffer must never park an anchor, since an anchor stops every earlier row
     * counting toward the balance.
     */
    @Test
    fun theBalanceTabSavesWithoutACategoryButNotWhileBlank() = runBlocking {
        val vm = createViewModel()
        vm.beginCreate()
        settle { vm.state.value.draftId.isNotBlank() }

        vm.onCategory("cat_food")
        vm.onKind(TransactionKind.RECONCILE)
        settle { vm.state.value.isReconcile }

        assertNull("a category cannot survive onto an assertion", vm.state.value.categoryId)
        assertFalse("a blank balance is not a claim", vm.state.value.canSave)

        vm.onDigit("0")
        assertTrue("an explicit zero is a real claim", vm.state.value.canSave)
    }

    /** Re-dating an assertion is the thing `Adjust balance` cannot do; it always stamps today. */
    @Test
    fun theBalanceTabWritesAnAssertionOnThePickedDay() = runBlocking {
        val vm = createViewModel()
        vm.beginCreate()
        settle { vm.state.value.draftId.isNotBlank() }

        vm.onKind(TransactionKind.RECONCILE)
        settle { vm.state.value.isReconcile }
        vm.onDigit("5")
        vm.onDigit("000")
        vm.onDigit("000")
        vm.onYesterday()

        var saved: Transaction? = null
        vm.save { tx -> saved = tx }
        settle { saved != null }

        assertEquals(TransactionKind.RECONCILE, saved?.kind)
        assertEquals(5_000_000L, saved?.amountMinor)
        assertEquals("2026-08-29", saved?.occurredOn)
        assertEquals("cat_other", saved?.categoryId)
    }

    /** Editing corrects the claim in place instead of adding a competing one. */
    @Test
    fun editingAnAssertionDoesNotMintASecondAnchor() = runBlocking {
        val vm = createViewModel()
        vm.beginCreate()
        settle { vm.state.value.draftId.isNotBlank() }
        vm.onKind(TransactionKind.RECONCILE)
        settle { vm.state.value.isReconcile }
        vm.onDigit("1")
        vm.onDigit("000")
        var first: Transaction? = null
        vm.save { tx -> first = tx }
        settle { first != null }

        vm.beginEdit(first!!)
        settle { vm.state.value.editingId == first!!.id }
        vm.onBackspace()
        vm.onDigit("9")
        var edited: Transaction? = null
        vm.save { tx -> edited = tx }
        settle { edited != null && edited!!.amountMinor != first!!.amountMinor }

        assertEquals(first!!.id, edited?.id)
        assertEquals(1, ledger.observeActive().first().count { it.kind == TransactionKind.RECONCILE })
    }

    /** Drains the test dispatcher until [done], the way the older tests spin by hand. */
    private fun settle(done: () -> Boolean) {
        var attempts = 0
        while (!done() && attempts < 50) {
            testDispatcher.scheduler.advanceUntilIdle()
            Thread.sleep(10)
            attempts++
        }
    }
}
