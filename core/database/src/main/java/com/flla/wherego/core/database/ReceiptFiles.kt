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

    fun compressTo(context: Context, source: Uri, dest: File): Boolean {
        dest.parentFile?.mkdirs()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: return false
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
