package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.example.model.ExtractedFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil

object ContactSheetGenerator {

    suspend fun generateContactSheet(
        context: Context,
        frames: List<ExtractedFrame>,
        projectTitle: String,
        columns: Int = 3
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (frames.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No frames provided for contact sheet"))
            }

            val numCols = columns.coerceIn(2, 6)
            val numRows = ceil(frames.size.toFloat() / numCols).toInt()

            val thumbWidth = 320
            val thumbHeight = 180
            val padding = 16
            val labelHeight = 44
            val headerHeight = 90
            val footerHeight = 40

            val cellWidth = thumbWidth + padding
            val cellHeight = thumbHeight + labelHeight + padding

            val sheetWidth = padding + (numCols * cellWidth)
            val sheetHeight = headerHeight + (numRows * cellHeight) + footerHeight

            val sheetBitmap = Bitmap.createBitmap(sheetWidth, sheetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(sheetBitmap)

            // 1. Dark Midnight background (#0B1020)
            val bgPaint = Paint().apply {
                color = Color.parseColor("#0B1020")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, sheetWidth.toFloat(), sheetHeight.toFloat(), bgPaint)

            // 2. Header
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#25D9F8") // Neon Cyan
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            canvas.drawText("PHOTO FRAME CONTACT SHEET", padding.toFloat(), 44f, titlePaint)
            canvas.drawText(
                "Source: $projectTitle  •  Total Frames: ${frames.size}  •  Created by MG Shankar Tech",
                padding.toFloat(),
                74f,
                subtitlePaint
            )

            // Header separator line
            val linePaint = Paint().apply {
                color = Color.parseColor("#283556")
                strokeWidth = 2f
            }
            canvas.drawLine(0f, headerHeight.toFloat(), sheetWidth.toFloat(), headerHeight.toFloat(), linePaint)

            // 3. Draw each cell
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                textSize = 16f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#34D399") // Soft green
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val frameBorderPaint = Paint().apply {
                color = Color.parseColor("#1E2842")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }

            for ((idx, frame) in frames.withIndex()) {
                val col = idx % numCols
                val row = idx / numCols

                val cellX = padding + col * cellWidth
                val cellY = headerHeight + padding + row * cellHeight

                // Draw thumbnail
                val thumbFile = File(if (File(frame.thumbPath).exists()) frame.thumbPath else frame.imagePath)
                if (thumbFile.exists()) {
                    val bmp = BitmapFactory.decodeFile(thumbFile.absolutePath)
                    if (bmp != null) {
                        val srcRect = Rect(0, 0, bmp.width, bmp.height)
                        val dstRect = Rect(cellX, cellY, cellX + thumbWidth, cellY + thumbHeight)
                        canvas.drawBitmap(bmp, srcRect, dstRect, null)
                        bmp.recycle()
                    }
                }

                // Border around thumbnail
                canvas.drawRect(
                    cellX.toFloat(),
                    cellY.toFloat(),
                    (cellX + thumbWidth).toFloat(),
                    (cellY + thumbHeight).toFloat(),
                    frameBorderPaint
                )

                // Timestamp & Frame # caption
                val captionY = cellY + thumbHeight + 24
                canvas.drawText("#${frame.index + 1}  ${frame.timestampFormatted}", cellX.toFloat() + 4, captionY.toFloat(), textPaint)

                // AI Score indicator
                if (frame.aiScore > 0) {
                    val scoreText = "AI: ${frame.aiScore}★"
                    canvas.drawText(scoreText, (cellX + thumbWidth - 75).toFloat(), captionY.toFloat(), scorePaint)
                }
            }

            // 4. Footer
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 14f
            }
            val footerText = "Generated with PHOTO FRAME — AI-Powered Video Frame Extractor by MG Shankar Tech"
            canvas.drawText(footerText, padding.toFloat(), (sheetHeight - 16).toFloat(), footerPaint)

            // Save contact sheet to cache
            val outputDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val sheetFile = File(outputDir, "CONTACT_SHEET_${System.currentTimeMillis()}.jpg")
            FileOutputStream(sheetFile).use { out ->
                sheetBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            sheetBitmap.recycle()

            Result.success(sheetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
