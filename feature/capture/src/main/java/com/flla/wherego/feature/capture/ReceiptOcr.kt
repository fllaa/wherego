package com.flla.wherego.feature.capture

import android.content.Context
import android.net.Uri
import com.flla.wherego.core.model.OcrLine
import com.flla.wherego.core.model.OcrText
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

interface ReceiptOcr {
    /** Recognized lines with their geometry, or [OcrText.EMPTY] when nothing could be read. Never throws. */
    suspend fun read(file: File): OcrText
}

@Singleton
class MlKitReceiptOcr @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReceiptOcr {
    override suspend fun read(file: File): OcrText = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext OcrText.EMPTY
        try {
            val image = InputImage.fromFilePath(context, Uri.fromFile(file))
            val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            try {
                suspendCancellableCoroutine { cont ->
                    client.process(image)
                        .addOnSuccessListener { text ->
                            if (cont.isActive) cont.resume(text.toOcrText())
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(OcrText.EMPTY)
                        }
                }
            } finally {
                client.close()
            }
        } catch (_: Exception) {
            OcrText.EMPTY
        }
    }
}

/**
 * Lines, not blocks, and their boxes.
 *
 * A block on a two-column slip is a whole column, so block order is what scrambles labels away from
 * their values. Lines are the finest granularity that still carries a box, which is what
 * [OcrText.rows] needs to lay the columns back out.
 */
private fun com.google.mlkit.vision.text.Text.toOcrText(): OcrText = OcrText(
    textBlocks.flatMap { block ->
        block.lines.map { line ->
            val box = line.boundingBox
            OcrLine(
                text = line.text.orEmpty(),
                left = box?.left ?: 0,
                top = box?.top ?: 0,
                right = box?.right ?: 0,
                bottom = box?.bottom ?: 0,
            )
        }
    },
)

/** Test double. Production binds MlKitReceiptOcr, whose model cannot run off-device. */
@Singleton
class FakeReceiptOcr @Inject constructor() : ReceiptOcr {
    /** Flat text, one line per newline and no geometry — what a single-column read looks like. */
    var text: String = ""

    /** Positioned lines, for exercising the two-column reflow. Takes precedence over [text]. */
    var lines: List<OcrLine>? = null

    override suspend fun read(file: File): OcrText =
        lines?.let(::OcrText) ?: OcrText.of(text)
}
