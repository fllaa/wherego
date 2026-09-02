package com.flla.wherego.core.database

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Only the fail-closed contract is pinned here.
 *
 * The other half of [ReceiptFiles.compressTo] — that `decodeStream` returns null by contract when
 * `inJustDecodeBounds` is set, so the *stream* and not the decode result is what may be null-checked
 * — is deliberately not asserted. Robolectric's `BitmapFactory` shadow ignores `inJustDecodeBounds`
 * and hands back a bitmap regardless, so a test of it passes just as happily against the broken
 * version: verified by reintroducing the bug and watching the suite stay green. A test that cannot
 * fail is worse than none, so that contract is covered on-device instead, by attaching an image and
 * confirming a row lands in `files/receipts` with OCR text in `receipts.ocrRaw`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReceiptFilesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /**
     * `openInputStream` throws `FileNotFoundException` for a path that has gone away rather than
     * returning null, and a shared image lives in `cacheDir`, which the system may evict between
     * the share arriving and the sheet reading it.
     *
     * `ReceiptStore.ingest` has no catch, and its caller runs inside `viewModelScope.launch`, so
     * before this was fail-closed an evicted cache file took the whole app down instead of opening
     * an empty sheet.
     */
    @Test
    fun aSourceThatCannotBeOpenedIsRejectedRatherThanThrown() {
        val missing = Uri.fromFile(File(context.cacheDir, "evicted-before-we-read-it.png"))
        val dest = File(context.cacheDir, "receipt-dest.jpg")

        val copied = ReceiptFiles.compressTo(context, missing, dest)

        assertFalse("an unreadable source must report failure, not raise", copied)
    }
}
