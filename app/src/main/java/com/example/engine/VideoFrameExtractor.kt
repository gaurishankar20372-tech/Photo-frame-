package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.ExtractedFrame
import com.example.model.ExtractionConfig
import com.example.model.RoundingStrategy
import com.example.model.VideoMetadata
import com.example.util.ComputerVisionAnalyzer
import com.example.util.TimestampFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object VideoFrameExtractor {

    /**
     * Reads metadata from a video URI using MediaMetadataRetriever and ContentResolver.
     */
    suspend fun extractMetadata(context: Context, videoUri: Uri): Result<VideoMetadata> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)

            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1920
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1080
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"

            // Attempt to read capture framerate, or default to standard 30fps
            var fps = 30.0f
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val fpsMeta = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                fps = fpsMeta?.toFloatOrNull() ?: 30.0f
            }

            // Retrieve display name and file size from content resolver
            var displayName = "video_${System.currentTimeMillis()}.mp4"
            var fileSize = 0L

            try {
                context.contentResolver.query(videoUri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }
            } catch (_: Exception) {}

            if (fileSize == 0L) {
                try {
                    context.contentResolver.openFileDescriptor(videoUri, "r")?.use { pfd ->
                        fileSize = pfd.statSize
                    }
                } catch (_: Exception) {}
            }

            // Account for video orientation swap
            val actualW = if (rotation == 90 || rotation == 270) height else width
            val actualH = if (rotation == 90 || rotation == 270) width else height

            val metadata = VideoMetadata(
                uriString = videoUri.toString(),
                filename = displayName,
                durationMs = durationMs,
                width = actualW,
                height = actualH,
                fps = fps,
                bitrate = bitrate,
                mimeType = mimeType,
                sizeBytes = fileSize,
                rotationDegrees = rotation
            )
            Result.success(metadata)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    /**
     * Extracts frames with high precision according to the configured start, end, and interval timestamps.
     */
    suspend fun extractFrames(
        context: Context,
        videoUri: Uri,
        config: ExtractionConfig,
        onProgress: (current: Int, total: Int, timestampUs: Long, lastThumbPath: String) -> Unit
    ): Result<List<ExtractedFrame>> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val extractedList = mutableListOf<ExtractedFrame>()

        val framesDir = File(context.cacheDir, "extracted_frames").apply { mkdirs() }
        // Clean previous temp frames
        framesDir.listFiles()?.forEach { it.delete() }

        try {
            retriever.setDataSource(context, videoUri)

            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0

            val retrieverOption = when (config.roundingStrategy) {
                RoundingStrategy.NEAREST_CLOSEST -> MediaMetadataRetriever.OPTION_CLOSEST
                RoundingStrategy.CLOSEST_SYNC -> MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                RoundingStrategy.PREVIOUS_SYNC -> MediaMetadataRetriever.OPTION_PREVIOUS_SYNC
                RoundingStrategy.NEXT_SYNC -> MediaMetadataRetriever.OPTION_NEXT_SYNC
            }

            // Generate requested microsecond timestamps
            val timestampsUs = TimestampFormatter.generateTimestamps(
                startUs = config.startTimestampUs,
                endUs = config.endTimestampUs,
                intervalUs = config.intervalUs,
                maxCount = config.maxFrameLimit
            )

            val total = timestampsUs.size
            var previousHash: Long? = null
            val matrix = if (rotation != 0) Matrix().apply { postRotate(rotation.toFloat()) } else null

            for ((index, reqUs) in timestampsUs.withIndex()) {
                currentCoroutineContext().ensureActive()

                var rawBitmap: Bitmap? = null
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                    try {
                        val metaW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1920
                        val metaH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1080
                        rawBitmap = retriever.getScaledFrameAtTime(
                            reqUs,
                            retrieverOption,
                            metaW,
                            metaH
                        )
                    } catch (_: Throwable) {}
                }
                if (rawBitmap == null) {
                    rawBitmap = retriever.getFrameAtTime(reqUs, retrieverOption)
                }
                if (rawBitmap == null) {
                    // Fallback to sync frame if closest returned null
                    rawBitmap = retriever.getFrameAtTime(reqUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }

                if (rawBitmap != null) {
                    // Correct rotation if needed
                    val orientedBitmap = if (matrix != null) {
                        Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true).also {
                            if (it !== rawBitmap) rawBitmap.recycle()
                        }
                    } else rawBitmap

                    // Apply DSLR Master Photographic Enhancement if enabled
                    val dslrEnhanced = if (config.dslrQualityOptimization) {
                        val enhanced = com.example.util.DslrImageEnhancer.enhanceBitmap(
                            orientedBitmap,
                            com.example.util.DslrEnhanceParams(
                                profile = com.example.util.DslrProfile.DSLR_PRIME,
                                sharpnessAmount = 0.45f,
                                microContrast = 0.22f,
                                shadowRecovery = 0.18f,
                                highlightProtection = 0.15f,
                                colorVibrance = 0.18f,
                                opticalVignette = 0.10f
                            )
                        )
                        if (enhanced !== orientedBitmap) orientedBitmap.recycle()
                        enhanced
                    } else {
                        orientedBitmap
                    }

                    val frameW = dslrEnhanced.width
                    val frameH = dslrEnhanced.height

                    // 1. Save full-resolution frame to cache at studio-grade 98% quality
                    val frameId = UUID.randomUUID().toString()
                    val frameFile = File(framesDir, "frame_${index}_$frameId.jpg")
                    FileOutputStream(frameFile).use { out ->
                        dslrEnhanced.compress(Bitmap.CompressFormat.JPEG, 98, out)
                    }

                    // 2. Save thumbnail for snappy gallery rendering
                    val thumbW = 320
                    val thumbH = (320f / frameW * frameH).toInt().coerceAtLeast(180)
                    val thumbBitmap = Bitmap.createScaledBitmap(dslrEnhanced, thumbW, thumbH, true)
                    val thumbFile = File(framesDir, "thumb_${index}_$frameId.jpg")
                    FileOutputStream(thumbFile).use { out ->
                        thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }

                    // 3. Computer Vision Quality Analysis
                    var aiScore = 75
                    var sharpness = 60f
                    var exposure = 75f
                    var contrast = 70f
                    var vibrancy = 65f
                    var isDuplicate = false
                    var hashDist = 100

                    if (config.runAiQualityRanking) {
                        val analysis = ComputerVisionAnalyzer.analyzeFrame(thumbBitmap)
                        aiScore = analysis.overallScore
                        sharpness = analysis.sharpnessScore
                        exposure = analysis.exposureScore
                        contrast = analysis.contrastScore
                        vibrancy = analysis.vibrancyScore

                        if (previousHash != null) {
                            hashDist = ComputerVisionAnalyzer.hammingDistance(previousHash!!, analysis.perceptualHash)
                            isDuplicate = hashDist <= 4
                        }
                        previousHash = analysis.perceptualHash
                    }

                    thumbBitmap.recycle()
                    dslrEnhanced.recycle()

                    val frame = ExtractedFrame(
                        id = frameId,
                        index = index,
                        requestedTimestampUs = reqUs,
                        actualTimestampUs = reqUs, // Nearest extracted presentation timestamp
                        timestampFormatted = TimestampFormatter.formatUsToHhMmSsMmm(reqUs),
                        imagePath = frameFile.absolutePath,
                        thumbPath = thumbFile.absolutePath,
                        width = frameW,
                        height = frameH,
                        fileSize = frameFile.length(),
                        aiScore = aiScore,
                        sharpnessScore = sharpness,
                        exposureScore = exposure,
                        contrastScore = contrast,
                        vibrancyScore = vibrancy,
                        isDuplicate = isDuplicate,
                        duplicateDistance = hashDist
                    )

                    if (!config.skipDuplicates || !isDuplicate) {
                        extractedList.add(frame)
                    }

                    onProgress(index + 1, total, reqUs, thumbFile.absolutePath)
                }
            }

            Result.success(extractedList)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }
}
