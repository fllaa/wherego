package com.flla.wherego.core.model

/**
 * Where an attached image came from, which decides how far its read is trusted.
 *
 * The distinction is not cosmetic. A receipt the user photographed is a receipt: the largest money
 * number on it is the total, which is the assumption [OcrAmountParser] is built on. A screen shared
 * in from a banking app is not a receipt — it prints the remaining balance beside the amount, often
 * an order of magnitude larger, and ML Kit reads a two-column layout column-first, so the labels
 * that would have told them apart arrive detached from their values.
 *
 * Measured on a GoPay-style transfer slip: a Rp 125.000 transfer beside a Rp 1.847.300 balance
 * parsed as Rp 1.847.300, anchored, which the old rule would have written straight into the amount.
 */
enum class ReceiptSource {
    /**
     * Photographed or picked by the user. An anchored read may be filled in on their behalf, and
     * the image may be queued for backup.
     */
    OWN,

    /**
     * Shared in from another app. The read is only ever offered — never written into the amount —
     * and the image stays on this phone: a transfer-success screen carries the account number and
     * the remaining balance right beside the number we came for.
     */
    SHARED,
}
