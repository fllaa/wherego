package com.flla.wherego.share

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.IntentCompat
import com.flla.wherego.CaptureRequest
import com.flla.wherego.MainActivity
import com.flla.wherego.core.i18n.AppLocale
import com.flla.wherego.core.i18n.R
import java.io.File

/**
 * Where an image handed over by another app lands: a bank's "share receipt" button, or the chip that
 * appears under a fresh screenshot.
 *
 * It exists to spend the grant on the incoming `content://` Uri while that grant is provably alive.
 * The bytes are copied into our own cache here and only a path we own travels on to [MainActivity],
 * which is what lets OCR survive a rotation or a process death mid-read.
 *
 * No window, no history, no recents entry — it is a hallway, not a room.
 */
class ShareReceiptActivity : Activity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.context(newBase, AppLocale.load(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shared = incomingImage()
        val copied = shared?.let(::copyIntoCache)
        if (shared != null && copied == null) {
            Toast.makeText(this, R.string.receipt_err_save_photo, Toast.LENGTH_SHORT).show()
        }
        // The sheet opens either way. An unreadable share still leaves the user one tap from
        // typing the number, which beats bouncing them back with nothing.
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(CaptureRequest.EXTRA_OPEN_CAPTURE, true)
                copied?.let { putExtra(CaptureRequest.EXTRA_RECEIPT_PATH, it.absolutePath) }
            },
        )
        finish()
    }

    private fun incomingImage(): Uri? {
        val intent = intent ?: return null
        if (intent.type?.startsWith("image/") != true) return null
        val fromExtra = when (intent.action) {
            Intent.ACTION_SEND ->
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?.firstOrNull()
            else -> return null
        }
        // A read grant can only ride on `data` or `clipData` — never on a parcelable extra. The
        // system migrates EXTRA_STREAM into ClipData for exactly that reason, so when a sender
        // leaves the extra out, the Uri that is actually readable is the one in the clip.
        return fromExtra
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            ?: intent.data
    }

    /**
     * One shared image is kept at a time. These are frequently transfer-success screens carrying an
     * account number and a balance, so the previous one is deleted rather than left to pile up in
     * a directory nobody looks at.
     */
    private fun copyIntoCache(source: Uri): File? = try {
        val dir = File(cacheDir, "shared").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val dest = File(dir, "shared-${System.currentTimeMillis()}")
        contentResolver.openInputStream(source)?.use { input ->
            dest.outputStream().use(input::copyTo)
        }
        dest.takeIf { it.length() > 0L }
    } catch (_: Exception) {
        null
    }
}
