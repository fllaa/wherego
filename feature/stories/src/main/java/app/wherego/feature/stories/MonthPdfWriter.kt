package app.wherego.feature.stories

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object MonthPdfWriter {
    fun write(context: Context, fileName: String, lines: List<String>): Uri {
        val dir = File(context.cacheDir, "pdf").apply { mkdirs() }
        val file = File(dir, fileName)
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val paint = Paint().apply {
            color = Color.parseColor("#1C1917")
            textSize = 11f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }
        val titlePaint = Paint(paint).apply {
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }
        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = 48f
        lines.forEachIndexed { index, line ->
            if (y > pageHeight - 48f) {
                doc.finishPage(page)
                pageNumber++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = 48f
            }
            val p = if (index == 0) titlePaint else paint
            canvas.drawText(line.take(90), 40f, y, p)
            y += if (index == 0) 28f else 16f
        }
        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }
}
