package com.example.model

enum class ExtractionMode {
    TIME_INTERVAL,
    SOURCE_FRAME
}

enum class RoundingStrategy {
    NEAREST_CLOSEST,
    CLOSEST_SYNC,
    PREVIOUS_SYNC,
    NEXT_SYNC
}

enum class ExportFormat {
    JPEG,
    PNG,
    WEBP
}

data class VideoMetadata(
    val uriString: String,
    val filename: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fps: Float,
    val bitrate: Long,
    val mimeType: String,
    val sizeBytes: Long,
    val rotationDegrees: Int = 0
) {
    val aspectRatioString: String
        get() {
            if (height == 0) return "16:9"
            val gcd = gcd(width, height)
            val w = width / gcd
            val h = height / gcd
            return if (w in 1..21 && h in 1..21) "$w:$h" else String.format("%.2f:1", width.toFloat() / height)
        }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}

data class ExtractedFrame(
    val id: String,
    val index: Int,
    val requestedTimestampUs: Long,
    val actualTimestampUs: Long,
    val timestampFormatted: String,
    val imagePath: String,
    val thumbPath: String,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val isSelected: Boolean = false,
    val isFavorite: Boolean = false,
    val isDuplicate: Boolean = false,
    val aiScore: Int = 0,
    val sharpnessScore: Float = 0f,
    val exposureScore: Float = 0f,
    val contrastScore: Float = 0f,
    val vibrancyScore: Float = 0f,
    val duplicateDistance: Int = 100,
    val situationMatchScore: Int = 0,
    val situationMatchReason: String? = null
)

data class SituationSearchResult(
    val prompt: String,
    val winningFrameId: String?,
    val explanation: String,
    val matchedCount: Int
)

data class ExtractionConfig(
    val startTimestampUs: Long = 0L,
    val endTimestampUs: Long = 0L,
    val intervalUs: Long = 33333L, // ~30 fps
    val mode: ExtractionMode = ExtractionMode.TIME_INTERVAL,
    val sourceFrameStep: Int = 1,
    val roundingStrategy: RoundingStrategy = RoundingStrategy.NEAREST_CLOSEST,
    val maxFrameLimit: Int = 200,
    val skipDuplicates: Boolean = false,
    val runAiQualityRanking: Boolean = true,
    val dslrQualityOptimization: Boolean = true
)

data class ExportConfig(
    val format: ExportFormat = ExportFormat.JPEG,
    val quality: Int = 98,
    val includeMetadataManifest: Boolean = true,
    val createContactSheet: Boolean = false,
    val contactSheetColumns: Int = 3,
    val filenamePrefix: String = "PHOTO_FRAME_"
)

data class ExtractionPreset(
    val id: String,
    val title: String,
    val description: String,
    val intervalUs: Long,
    val intervalLabel: String,
    val mode: ExtractionMode = ExtractionMode.TIME_INTERVAL
)

data class BatchJob(
    val id: String,
    val videoUriString: String,
    val videoTitle: String,
    val durationMs: Long,
    val config: ExtractionConfig,
    val status: BatchStatus = BatchStatus.PENDING,
    val progress: Float = 0f,
    val extractedFrames: List<ExtractedFrame> = emptyList(),
    val errorMessage: String? = null
)

enum class BatchStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}
