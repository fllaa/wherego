package com.flla.wherego.quicksettings

import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.flla.wherego.CaptureRequest
import com.flla.wherego.MainActivity
import com.flla.wherego.core.i18n.AppLocale
import com.flla.wherego.core.i18n.R
import java.util.function.Consumer

/**
 * Quick Settings tile that opens the capture sheet from anywhere.
 *
 * It launches the sheet; it does not read the screen. Capturing the display would mean
 * MediaProjection, whose consent cannot be cached — Android 14 and later force a fresh
 * "start capturing everything on your screen" dialog per session — so a tile that captured would
 * cost two system dialogs and a cast chip on every spend. Sharing a screenshot into
 * [com.flla.wherego.share.ShareReceiptActivity] gets the same number for fewer taps and no grant.
 */
class CaptureTileService : TileService() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.context(newBase, AppLocale.load(newBase)))
    }

    /**
     * The manifest label is resolved by the system in the system's language. Wherego's language is
     * an in-app setting, so the label is restated here from our own localized resources.
     */
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = getString(R.string.qs_tile_label)
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(CaptureRequest.EXTRA_OPEN_CAPTURE, true)
        }
        // A tile sits on the lock screen. The ledger is behind the keyguard for a reason.
        if (isSecure) unlockAndRun { open(intent) } else open(intent)
    }

    private fun open(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        } else {
            openBeforeApi34(intent)
        }
    }

    /**
     * On 26..33 this is the only call that exists: the `PendingIntent` overload was added in 34,
     * and the `Intent` overload throws `UnsupportedOperationException` from 34 onward — which is
     * why [open] gates on the version. Lint reads the deprecation without seeing that gate.
     */
    @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
    private fun openBeforeApi34(intent: Intent) {
        startActivityAndCollapse(intent)
    }
}

/**
 * A lambda that asks the system to offer adding the tile, or `null` below API 33 where no such
 * prompt exists and the user drags the tile in from the shade's own edit screen.
 *
 * Returning `null` is how the caller knows to hide the row rather than offer a button that cannot
 * do anything.
 */
fun addCaptureTilePrompt(context: Context): (() -> Unit)? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        { requestAddTile(context) }
    } else {
        null
    }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun requestAddTile(context: Context) {
    context.getSystemService(StatusBarManager::class.java)?.requestAddTileService(
        ComponentName(context, CaptureTileService::class.java),
        context.getString(R.string.qs_tile_label),
        Icon.createWithResource(context, com.flla.wherego.R.drawable.ic_qs_capture),
        context.mainExecutor,
        Consumer { result ->
            // A tile that is already there shows no dialog at all, so without this the row would
            // look dead on the second tap. A refusal stays silent: the user just said no.
            val message = when (result) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> R.string.qs_tile_added
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> R.string.qs_tile_already
                else -> return@Consumer
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
    )
}
