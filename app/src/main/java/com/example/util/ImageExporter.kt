package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.ExportConfig
import com.example.model.ExportFormat
import com.example.model.ExtractedFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ImageExporter {

    /**
     * Saves an image bitmap or existing file to device MediaStore (Pictures/PhotoFrame).
     */
    suspend fun saveFrameToGallery(
        context: Context,
        frame: ExtractedFrame,
        config: ExportConfig,
        adjustedBitmap: Bitmap? = null
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val extension = when (config.format) {
                ExportFormat.JPEG -> "jpg"
                ExportFormat.PNG -> "png"
                ExportFormat.WEBP -> "webp"
            }
            val mimeType = when (config.format) {
                ExportFormat.JPEG -> "image/jpeg"
                ExportFormat.PNG -> "image/png"
                ExportFormat.WEBP -> "image/webp"
            }
            val compressFormat = when (config.format) {
                ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
                ExportFormat.PNG -> Bitmap.CompressFormat.PNG
                ExportFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }

            val filenameTs = TimestampFormatter.formatUsToFilenameTimestamp(frame.actualTimestampUs)
            val displayName = "${config.filenamePrefix}$filenameTs.$extension"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PhotoFrame")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

            resolver.openOutputStream(imageUri).use { outStream ->
                if (outStream == null) throw Exception("Cannot open MediaStore output stream")

                val bitmapToSave = adjustedBitmap ?: BitmapFactory.decodeFile(frame.imagePath)
                if (bitmapToSave != null) {
                    bitmapToSave.compress(compressFormat, config.quality, outStream)
                    if (adjustedBitmap == null) {
                        bitmapToSave.recycle()
                    }
                } else {
                    // Fallback direct copy if bitmap decode failed
                    File(frame.imagePath).inputStream().use { it.copyTo(outStream) }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            Result.success(imageUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a ZIP archive containing all selected frames and a metadata manifest.
     */
    suspend fun createZipArchive(
        context: Context,
        frames: List<ExtractedFrame>,
        videoTitle: String,
        config: ExportConfig
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val zipFile = File(exportDir, "PHOTO_FRAME_${System.currentTimeMillis()}.zip")

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // 1. Add frames
                for (frame in frames) {
                    val frameFile = File(frame.imagePath)
                    if (!frameFile.exists()) continue

                    val filenameTs = TimestampFormatter.formatUsToFilenameTimestamp(frame.actualTimestampUs)
                    val entryName = "photos/${config.filenamePrefix}$filenameTs.jpg"
                    zos.putNextEntry(ZipEntry(entryName))
                    FileInputStream(frameFile).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }

                // 2. Add JSON metadata manifest if enabled
                if (config.includeMetadataManifest) {
                    val rootJson = JSONObject().apply {
                        put("project", "PHOTO FRAME")
                        put("creator", "MG Shankar Tech")
                        put("videoTitle", videoTitle)
                        put("exportDate", System.currentTimeMillis())
                        put("frameCount", frames.size)

                        val framesArray = JSONArray()
                        for (f in frames) {
                            val fJson = JSONObject().apply {
                                put("index", f.index)
                                put("requestedTimestampUs", f.requestedTimestampUs)
                                put("actualTimestampUs", f.actualTimestampUs)
                                put("timestampFormatted", f.timestampFormatted)
                                put("width", f.width)
                                put("height", f.height)
                                put("aiScore", f.aiScore)
                                put("sharpnessScore", f.sharpnessScore)
                                put("exposureScore", f.exposureScore)
                                put("contrastScore", f.contrastScore)
                                put("isDuplicate", f.isDuplicate)
                            }
                            framesArray.put(fJson)
                        }
                        put("frames", framesArray)
                    }

                    zos.putNextEntry(ZipEntry("manifest.json"))
                    zos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    // Also write CSV format manifest
                    val csvBuilder = StringBuilder()
                    csvBuilder.append("Index,RequestedTimestampUs,ActualTimestampUs,TimestampFormatted,Width,Height,AIScore,Sharpness,Exposure\n")
                    for (f in frames) {
                        csvBuilder.append("${f.index},${f.requestedTimestampUs},${f.actualTimestampUs},${f.timestampFormatted},${f.width},${f.height},${f.aiScore},${f.sharpnessScore},${f.exposureScore}\n")
                    }
                    zos.putNextEntry(ZipEntry("manifest.csv"))
                    zos.write(csvBuilder.toString().toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }

            Result.success(zipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Builds a share intent for sharing single or multiple photos via Android share sheet.
     */
    fun buildShareIntent(context: Context, frames: List<ExtractedFrame>): Intent? {
        if (frames.isEmpty()) return null
        val authority = "${context.packageName}.fileprovider"

        return if (frames.size == 1) {
            val file = File(frames.first().imagePath)
            if (!file.exists()) return null
            val uri = FileProvider.getUriForFile(context, authority, file)
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Photo extracted by PHOTO FRAME")
                putExtra(Intent.EXTRA_TEXT, "Frame at ${frames.first().timestampFormatted} extracted with PHOTO FRAME by MG Shankar Tech")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            val uris = ArrayList<Uri>()
            for (f in frames) {
                val file = File(f.imagePath)
                if (file.exists()) {
                    uris.add(FileProvider.getUriForFile(context, authority, file))
                }
            }
            if (uris.isEmpty()) return null
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_SUBJECT, "${uris.size} Photos from PHOTO FRAME")
                putExtra(Intent.EXTRA_TEXT, "Extracted with PHOTO FRAME by MG Shankar Tech")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    /**
     * Builds a share intent for sharing a ZIP file.
     */
    fun buildShareZipIntent(context: Context, zipFile: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, zipFile)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "PHOTO FRAME Archive: ${zipFile.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
