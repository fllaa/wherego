package com.flla.wherego.feature.capture

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

@Singleton
class ReceiptOcr @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun read(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext ""
        try {
            val image = InputImage.fromFilePath(context, Uri.fromFile(file))
            val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            try {
                suspendCancellableCoroutine { cont ->
                    client.process(image)
                        .addOnSuccessListener { text ->
                            if (cont.isActive) cont.resume(text.text.orEmpty())
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume("")
                        }
                }
            } finally {
                client.close()
            }
        } catch (_: Exception) {
            ""
        }
    }
}
