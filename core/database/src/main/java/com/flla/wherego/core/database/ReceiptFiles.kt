package com.flla.wherego.core.database

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.flla.wherego.core.model.ReceiptImage
import java.io.File
import java.io.FileOutputStream

object ReceiptFiles {
    fun dir(context: Context): File = File(context.filesDir, "receipts").apply { mkdirs() }

    fun dest(context: Context, id: String): File = File(dir(context), "$id.jpg")

    /**
     * `false` when the source cannot be read or decoded.
     *
     * That includes the provider throwing: `openInputStream` raises `FileNotFoundException` for a
     * path that has gone away rather than returning null, and a shared image lives in `cacheDir`,
     * which the system may evict between the share arriving and the sheet reading it. Callers map
     * `false` to "no receipt"; letting the exception out instead kills the capture coroutine.
     */
    fun compressTo(context: Context, source: Uri, dest: File): Boolean = try {
        compressOrThrow(context, source, dest)
    } catch (_: Exception) {
        false
    }

    private fun compressOrThrow(context: Context, source: Uri, dest: File): Boolean {
        dest.parentFile?.mkdirs()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // `decodeStream` returns null by contract when `inJustDecodeBounds` is set — the size comes
        // back through `bounds`, not through a bitmap. Only the stream can be null-checked here;
        // testing the decode result rejects every image ever handed in.
        val probe = context.contentResolver.openInputStream(source) ?: return false
        probe.use { BitmapFactory.decodeStream(it, null, bounds) }
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) return false
        val (dstW, dstH) = ReceiptImage.scaledSize(srcW, srcH)
        val sample = sampleSize(srcW, srcH, dstW, dstH)
        val decode = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, decode)
        } ?: return false
        val scaled = if (bitmap.width > dstW || bitmap.height > dstH) {
            Bitmap.createScaledBitmap(bitmap, dstW, dstH, true).also {
                if (it !== bitmap) bitmap.recycle()
            }
        } else {
            bitmap
        }
        return try {
            FileOutputStream(dest).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, ReceiptImage.JPEG_QUALITY, out)
            }
            true
        } finally {
            scaled.recycle()
        }
    }

    private fun sampleSize(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Int {
        var sample = 1
        var w = srcW
        var h = srcH
        while (w / 2 >= dstW && h / 2 >= dstH) {
            sample *= 2
            w /= 2
            h /= 2
        }
        return sample
    }
}
