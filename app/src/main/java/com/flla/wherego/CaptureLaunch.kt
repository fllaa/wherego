package com.flla.wherego

import android.content.Intent

/**
 * The one way anything outside the tab bar asks for the capture sheet. The Quick Settings tile, the
 * share sheet and the last step of onboarding all hand [MainActivity] one of these instead of each
 * growing its own flag.
 */
data class CaptureRequest(
    val openSheet: Boolean = false,
    /**
     * Absolute path of an image already copied into our own storage, or `null`.
     *
     * A path, not the sender's `content://` Uri: that grant dies with the activity it was handed
     * to, and OCR can outlive it — a rotation mid-read would leave nothing to re-open.
     */
    val receiptPath: String? = null,
) {
    val isEmpty: Boolean get() = !openSheet && receiptPath == null

    companion object {
        val None = CaptureRequest()

        const val EXTRA_OPEN_CAPTURE = "com.flla.wherego.extra.OPEN_CAPTURE"
        const val EXTRA_RECEIPT_PATH = "com.flla.wherego.extra.RECEIPT_PATH"

        /** [None] for a plain launcher start, so a cold open lands on Home as it always did. */
        fun from(intent: Intent?): CaptureRequest {
            if (intent == null || !intent.getBooleanExtra(EXTRA_OPEN_CAPTURE, false)) return None
            return CaptureRequest(
                openSheet = true,
                receiptPath = intent.getStringExtra(EXTRA_RECEIPT_PATH),
            )
        }
    }
}
