package com.ilaiyarasu.smartnote.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.ilaiyarasu.smartnote.data.Note
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    fun exportNoteToPdf(context: Context, note: Note): Result<Uri> {
        return try {
            val document = PdfDocument()
            val titlePaint = Paint().apply {
                textSize = 20f
                isFakeBoldText = true
            }
            val metaPaint = Paint().apply {
                textSize = 11f
                color = android.graphics.Color.DKGRAY
            }
            val bodyPaint = Paint().apply {
                textSize = 13f
            }

            var pageNumber = 1
            var page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            var canvas = page.canvas
            var y = MARGIN + 20f

            canvas.drawText(note.title.ifBlank { "Untitled" }, MARGIN, y, titlePaint)
            y += 25f

            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(Date(note.updatedAt))
            canvas.drawText("${note.category} • $dateStr", MARGIN, y, metaPaint)
            y += 30f

            val maxWidth = PAGE_WIDTH - (MARGIN * 2)
            val lineHeight = 18f
            val paragraphs = note.content.split("\n")

            for (paragraph in paragraphs) {
                val words = paragraph.split(" ")
                var line = ""

                for (word in words) {
                    val testLine = if (line.isEmpty()) word else "$line $word"
                    if (bodyPaint.measureText(testLine) > maxWidth) {
                        if (y > PAGE_HEIGHT - MARGIN) {
                            document.finishPage(page)
                            pageNumber++
                            page = document.startPage(
                                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                            )
                            canvas = page.canvas
                            y = MARGIN
                        }
                        canvas.drawText(line, MARGIN, y, bodyPaint)
                        y += lineHeight
                        line = word
                    } else {
                        line = testLine
                    }
                }

                if (line.isNotEmpty()) {
                    if (y > PAGE_HEIGHT - MARGIN) {
                        document.finishPage(page)
                        pageNumber++
                        page = document.startPage(
                            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                        )
                        canvas = page.canvas
                        y = MARGIN
                    }
                    canvas.drawText(line, MARGIN, y, bodyPaint)
                    y += lineHeight
                }
                y += 6f
            }

            document.finishPage(page)

            val exportDir = File(context.cacheDir, "pdf_exports").apply { mkdirs() }
            val safeFileName = note.title.ifBlank { "note" }
                .replace(Regex("[^a-zA-Z0-9]"), "_")
                .take(40)
            val file = File(exportDir, "${safeFileName}_${System.currentTimeMillis()}.pdf")

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareOrOpenPdf(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
        }
    }
}
