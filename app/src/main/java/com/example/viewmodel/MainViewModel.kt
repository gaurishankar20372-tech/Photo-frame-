package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BackendStatus
import com.example.data.PresetRepository
import com.example.data.ProjectEntity
import com.example.data.RenderBackendClient
import com.example.engine.SampleVideoProvider
import com.example.engine.VideoFrameExtractor
import com.example.model.BatchJob
import com.example.model.BatchStatus
import com.example.model.ExportConfig
import com.example.model.ExtractedFrame
import com.example.model.ExtractionConfig
import com.example.model.ExtractionMode
import com.example.model.ExtractionPreset
import com.example.model.VideoMetadata
import com.example.util.ContactSheetGenerator
import com.example.util.ImageExporter
import com.example.util.TimestampFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class AppScreen {
    HOME,
    VIDEO_DETAILS,
    PRECISION_EDITOR,
    PROCESSING,
    GALLERY,
    PHOTO_VIEWER,
    COMPARISON_STUDIO,
    EXPORT_STUDIO,
    BATCH_PROCESSING,
    SETTINGS,
    ABOUT,
    HELP
}

enum class GallerySort {
    TIMESTAMP_ASC,
    TIMESTAMP_DESC,
    AI_SCORE_DESC,
    SHARPNESS_DESC,
    FILE_SIZE_DESC,
    SITUATION_MATCH_DESC
}

enum class GalleryFilter {
    ALL,
    SITUATION_MATCH,
    TOP_PICKS,
    SHARPEST,
    WELL_EXPOSED,
    FAVORITES,
    NO_DUPLICATES
}

data class ViewerAdjustments(
    val brightness: Float = 0f,      // -100 to 100
    val contrast: Float = 1.0f,     // 0.5 to 2.0
    val saturation: Float = 1.0f,   // 0.0 to 2.0
    val rotation: Int = 0,          // 0, 90, 180, 270
    val sharpnessBoost: Boolean = false,
    val showSplitPreview: Boolean = false,
    val dslrMasterEnhanceEnabled: Boolean = true,
    val dslrProfile: com.example.util.DslrProfile = com.example.util.DslrProfile.DSLR_PRIME,
    val microClarity: Float = 0.25f,
    val shadowLift: Float = 0.20f,
    val splitDividerPosition: Float = 0.5f
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val recentProjects = db.projectDao().getAllProjects()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Navigation State
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val screenBackStack = mutableListOf(AppScreen.HOME)

    // Current Video & Extraction Setup
    private val _currentVideoUri = MutableStateFlow<Uri?>(null)
    val currentVideoUri: StateFlow<Uri?> = _currentVideoUri.asStateFlow()

    private val _videoMetadata = MutableStateFlow<VideoMetadata?>(null)
    val videoMetadata: StateFlow<VideoMetadata?> = _videoMetadata.asStateFlow()

    private val _extractionConfig = MutableStateFlow(ExtractionConfig())
    val extractionConfig: StateFlow<ExtractionConfig> = _extractionConfig.asStateFlow()

    // Extraction Engine State
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingProgress = MutableStateFlow(0f)
    val processingProgress: StateFlow<Float> = _processingProgress.asStateFlow()

    private val _processedCount = MutableStateFlow(0)
    val processedCount: StateFlow<Int> = _processedCount.asStateFlow()

    private val _totalEstimatedFrames = MutableStateFlow(0)
    val totalEstimatedFrames: StateFlow<Int> = _totalEstimatedFrames.asStateFlow()

    private val _currentProcessingTimestampUs = MutableStateFlow(0L)
    val currentProcessingTimestampUs: StateFlow<Long> = _currentProcessingTimestampUs.asStateFlow()

    private val _liveThumbnailPath = MutableStateFlow<String?>(null)
    val liveThumbnailPath: StateFlow<String?> = _liveThumbnailPath.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var extractionJob: Job? = null

    // Gallery State
    private val _extractedFrames = MutableStateFlow<List<ExtractedFrame>>(emptyList())
    val extractedFrames: StateFlow<List<ExtractedFrame>> = _extractedFrames.asStateFlow()

    private val _selectedFrameIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedFrameIds: StateFlow<Set<String>> = _selectedFrameIds.asStateFlow()

    private val _gallerySort = MutableStateFlow(GallerySort.TIMESTAMP_ASC)
    val gallerySort: StateFlow<GallerySort> = _gallerySort.asStateFlow()

    private val _galleryFilter = MutableStateFlow(GalleryFilter.ALL)
    val galleryFilter: StateFlow<GalleryFilter> = _galleryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // AI Situation Moment Search
    private val _situationQuery = MutableStateFlow("")
    val situationQuery: StateFlow<String> = _situationQuery.asStateFlow()

    private val _isAnalyzingSituation = MutableStateFlow(false)
    val isAnalyzingSituation: StateFlow<Boolean> = _isAnalyzingSituation.asStateFlow()

    private val _situationResult = MutableStateFlow<com.example.model.SituationSearchResult?>(null)
    val situationResult: StateFlow<com.example.model.SituationSearchResult?> = _situationResult.asStateFlow()

    // Active Photo Viewer
    private val _activeViewerFrame = MutableStateFlow<ExtractedFrame?>(null)
    val activeViewerFrame: StateFlow<ExtractedFrame?> = _activeViewerFrame.asStateFlow()

    private val _viewerAdjustments = MutableStateFlow(ViewerAdjustments())
    val viewerAdjustments: StateFlow<ViewerAdjustments> = _viewerAdjustments.asStateFlow()

    // Comparison Studio
    private val _comparisonFrames = MutableStateFlow<List<ExtractedFrame>>(emptyList())
    val comparisonFrames: StateFlow<List<ExtractedFrame>> = _comparisonFrames.asStateFlow()

    // Export State
    private val _exportConfig = MutableStateFlow(ExportConfig())
    val exportConfig: StateFlow<ExportConfig> = _exportConfig.asStateFlow()

    private val _exportStatusMessage = MutableStateFlow<String?>(null)
    val exportStatusMessage: StateFlow<String?> = _exportStatusMessage.asStateFlow()

    // Batch Processing State
    private val _batchQueue = MutableStateFlow<List<BatchJob>>(emptyList())
    val batchQueue: StateFlow<List<BatchJob>> = _batchQueue.asStateFlow()

    // Settings
    private val _maxFrameWarningLimit = MutableStateFlow(300)
    val maxFrameWarningLimit: StateFlow<Int> = _maxFrameWarningLimit.asStateFlow()

    // Render Cloud Backend Integration State
    private val _renderBackendUrl = MutableStateFlow("https://photoframe-backend.onrender.com")
    val renderBackendUrl: StateFlow<String> = _renderBackendUrl.asStateFlow()

    private val _backendStatus = MutableStateFlow<BackendStatus>(BackendStatus.Disconnected)
    val backendStatus: StateFlow<BackendStatus> = _backendStatus.asStateFlow()

    private val _useRenderCloudEnhance = MutableStateFlow(false)
    val useRenderCloudEnhance: StateFlow<Boolean> = _useRenderCloudEnhance.asStateFlow()

    private val _cloudEnhanceStatusMessage = MutableStateFlow<String?>(null)
    val cloudEnhanceStatusMessage: StateFlow<String?> = _cloudEnhanceStatusMessage.asStateFlow()

    fun updateRenderBackendUrl(url: String) {
        _renderBackendUrl.value = url.trim()
        _backendStatus.value = BackendStatus.Disconnected
    }

    fun setUseRenderCloudEnhance(enabled: Boolean) {
        _useRenderCloudEnhance.value = enabled
    }

    fun testBackendConnection() {
        viewModelScope.launch {
            _backendStatus.value = BackendStatus.Connecting
            val result = RenderBackendClient.checkHealth(_renderBackendUrl.value)
            result.onSuccess { (latency, service) ->
                _backendStatus.value = BackendStatus.Connected(latency, service)
            }.onFailure { err ->
                _backendStatus.value = BackendStatus.Error(err.message ?: "Could not connect to Render backend")
            }
        }
    }

    // Navigation Methods
    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            screenBackStack.add(screen)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (screenBackStack.size > 1) {
            screenBackStack.removeAt(screenBackStack.size - 1)
            _currentScreen.value = screenBackStack.last()
            return true
        }
        return false
    }

    // Video Loading
    fun loadVideo(uri: Uri) {
        viewModelScope.launch {
            _currentVideoUri.value = uri
            _errorMessage.value = null

            val result = VideoFrameExtractor.extractMetadata(getApplication(), uri)
            result.onSuccess { meta ->
                _videoMetadata.value = meta
                // Reset extraction config to full video duration
                val startUs = 0L
                val endUs = meta.durationMs * 1000L
                // Choose default interval according to detected FPS
                val defaultIntervalUs = if (meta.fps >= 50f) 16_667L else 33_333L

                _extractionConfig.value = ExtractionConfig(
                    startTimestampUs = startUs,
                    endTimestampUs = endUs,
                    intervalUs = defaultIntervalUs,
                    mode = ExtractionMode.TIME_INTERVAL,
                    maxFrameLimit = _maxFrameWarningLimit.value
                )
                updateEstimatedCount()
                navigateTo(AppScreen.VIDEO_DETAILS)
            }.onFailure { err ->
                _errorMessage.value = "Failed to load video: ${err.localizedMessage ?: "Unsupported format"}"
            }
        }
    }

    fun loadSampleDemoVideo() {
        viewModelScope.launch {
            _errorMessage.value = null
            val demoResult = SampleVideoProvider.getOrCreateSampleVideo(getApplication())
            demoResult.onSuccess { uri ->
                loadVideo(uri)
            }.onFailure { err ->
                _errorMessage.value = "Could not initialize demo video: ${err.message}"
            }
        }
    }

    // Timeline and Precision Configuration
    fun setStartTimestampUs(startUs: Long) {
        val meta = _videoMetadata.value ?: return
        val validStart = startUs.coerceIn(0L, meta.durationMs * 1000L)
        val validEnd = _extractionConfig.value.endTimestampUs.coerceAtLeast(validStart)
        _extractionConfig.value = _extractionConfig.value.copy(
            startTimestampUs = validStart,
            endTimestampUs = validEnd
        )
        updateEstimatedCount()
    }

    fun setEndTimestampUs(endUs: Long) {
        val meta = _videoMetadata.value ?: return
        val validEnd = endUs.coerceIn(0L, meta.durationMs * 1000L)
        val validStart = _extractionConfig.value.startTimestampUs.coerceAtMost(validEnd)
        _extractionConfig.value = _extractionConfig.value.copy(
            startTimestampUs = validStart,
            endTimestampUs = validEnd
        )
        updateEstimatedCount()
    }

    fun setIntervalUs(intervalUs: Long) {
        val validInterval = intervalUs.coerceAtLeast(100L) // Minimum 100 microseconds (0.1ms)
        _extractionConfig.value = _extractionConfig.value.copy(intervalUs = validInterval)
        updateEstimatedCount()
    }

    fun applyPreset(preset: ExtractionPreset) {
        _extractionConfig.value = _extractionConfig.value.copy(
            intervalUs = preset.intervalUs,
            mode = preset.mode
        )
        updateEstimatedCount()
    }

    fun toggleSkipDuplicates(skip: Boolean) {
        _extractionConfig.value = _extractionConfig.value.copy(skipDuplicates = skip)
    }

    fun toggleAiRanking(runAi: Boolean) {
        _extractionConfig.value = _extractionConfig.value.copy(runAiQualityRanking = runAi)
    }

    private fun updateEstimatedCount() {
        val cfg = _extractionConfig.value
        val count = TimestampFormatter.calculateEstimatedFrameCount(cfg.startTimestampUs, cfg.endTimestampUs, cfg.intervalUs)
        _totalEstimatedFrames.value = count
    }

    // Frame Extraction Execution
    fun startExtraction() {
        val uri = _currentVideoUri.value ?: return
        val meta = _videoMetadata.value ?: return

        _isProcessing.value = true
        _processingProgress.value = 0f
        _processedCount.value = 0
        _liveThumbnailPath.value = null
        _errorMessage.value = null
        navigateTo(AppScreen.PROCESSING)

        extractionJob?.cancel()
        extractionJob = viewModelScope.launch {
            val result = VideoFrameExtractor.extractFrames(
                context = getApplication(),
                videoUri = uri,
                config = _extractionConfig.value
            ) { current, total, timeUs, thumbPath ->
                _processedCount.value = current
                _totalEstimatedFrames.value = total
                _processingProgress.value = if (total > 0) current.toFloat() / total else 0f
                _currentProcessingTimestampUs.value = timeUs
                _liveThumbnailPath.value = thumbPath
            }

            _isProcessing.value = false

            result.onSuccess { frames ->
                _extractedFrames.value = frames
                _selectedFrameIds.value = emptySet()

                // Save project record to Room DB
                saveProjectRecord(meta, frames)
                navigateTo(AppScreen.GALLERY)
            }.onFailure { err ->
                _errorMessage.value = "Extraction stopped: ${err.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun cancelExtraction() {
        extractionJob?.cancel()
        _isProcessing.value = false
        navigateBack()
    }

    private suspend fun saveProjectRecord(meta: VideoMetadata, frames: List<ExtractedFrame>) {
        val previewPath = frames.firstOrNull()?.thumbPath
        val entity = ProjectEntity(
            id = UUID.randomUUID().toString(),
            title = meta.filename,
            videoUriString = meta.uriString,
            durationMs = meta.durationMs,
            frameCount = frames.size,
            favoriteCount = frames.count { it.isFavorite },
            createdAt = System.currentTimeMillis(),
            previewThumbPath = previewPath
        )
        db.projectDao().insertProject(entity)
    }

    fun reopenProject(project: ProjectEntity) {
        loadVideo(Uri.parse(project.videoUriString))
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            db.projectDao().deleteProject(project)
        }
    }

    // Gallery Actions
    fun toggleFrameSelection(frameId: String) {
        val current = _selectedFrameIds.value.toMutableSet()
        if (current.contains(frameId)) {
            current.remove(frameId)
        } else {
            current.add(frameId)
        }
        _selectedFrameIds.value = current
    }

    fun selectAllFrames() {
        _selectedFrameIds.value = _extractedFrames.value.map { it.id }.toSet()
    }

    fun deselectAllFrames() {
        _selectedFrameIds.value = emptySet()
    }

    fun invertFrameSelection() {
        val allIds = _extractedFrames.value.map { it.id }.toSet()
        _selectedFrameIds.value = allIds - _selectedFrameIds.value
    }

    fun toggleFavorite(frameId: String) {
        _extractedFrames.value = _extractedFrames.value.map {
            if (it.id == frameId) it.copy(isFavorite = !it.isFavorite) else it
        }
        if (_activeViewerFrame.value?.id == frameId) {
            _activeViewerFrame.value = _activeViewerFrame.value?.let { it.copy(isFavorite = !it.isFavorite) }
        }
    }

    fun deleteSelectedFrames() {
        val toDelete = _selectedFrameIds.value
        _extractedFrames.value = _extractedFrames.value.filter { it.id !in toDelete }
        _selectedFrameIds.value = emptySet()
    }

    fun setGallerySort(sort: GallerySort) {
        _gallerySort.value = sort
    }

    fun setGalleryFilter(filter: GalleryFilter) {
        _galleryFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun searchSituation(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return
        _situationQuery.value = trimmed
        viewModelScope.launch {
            _isAnalyzingSituation.value = true
            val (updatedFrames, result) = com.example.ai.SituationFrameFinder.findSituationFrames(
                trimmed,
                _extractedFrames.value
            )
            _extractedFrames.value = updatedFrames
            _situationResult.value = result
            _isAnalyzingSituation.value = false
            _gallerySort.value = GallerySort.SITUATION_MATCH_DESC
            _galleryFilter.value = GalleryFilter.SITUATION_MATCH
        }
    }

    fun clearSituationSearch() {
        _situationQuery.value = ""
        _situationResult.value = null
        _galleryFilter.value = GalleryFilter.ALL
        _gallerySort.value = GallerySort.TIMESTAMP_ASC
    }

    // Photo Viewer & Adjustments
    fun openPhotoViewer(frame: ExtractedFrame) {
        _activeViewerFrame.value = frame
        _viewerAdjustments.value = ViewerAdjustments()
        navigateTo(AppScreen.PHOTO_VIEWER)
    }

    fun navigateViewerNext() {
        val current = _activeViewerFrame.value ?: return
        val list = getFilteredFrames()
        val index = list.indexOfFirst { it.id == current.id }
        if (index != -1 && index < list.size - 1) {
            _activeViewerFrame.value = list[index + 1]
            _viewerAdjustments.value = ViewerAdjustments()
        }
    }

    fun navigateViewerPrev() {
        val current = _activeViewerFrame.value ?: return
        val list = getFilteredFrames()
        val index = list.indexOfFirst { it.id == current.id }
        if (index > 0) {
            _activeViewerFrame.value = list[index - 1]
            _viewerAdjustments.value = ViewerAdjustments()
        }
    }

    fun updateAdjustments(adjustments: ViewerAdjustments) {
        _viewerAdjustments.value = adjustments
    }

    fun resetAdjustments() {
        _viewerAdjustments.value = ViewerAdjustments()
    }

    // Comparison Studio
    fun launchComparisonStudio(frames: List<ExtractedFrame>? = null) {
        val targetFrames = frames ?: _extractedFrames.value.filter { it.id in _selectedFrameIds.value }
        if (targetFrames.size in 2..4) {
            _comparisonFrames.value = targetFrames
            navigateTo(AppScreen.COMPARISON_STUDIO)
        } else if (_extractedFrames.value.size >= 2) {
            // Default to top 2 frames
            _comparisonFrames.value = _extractedFrames.value.sortedByDescending { it.aiScore }.take(2)
            navigateTo(AppScreen.COMPARISON_STUDIO)
        }
    }

    // Export Studio
    fun updateExportConfig(config: ExportConfig) {
        _exportConfig.value = config
    }

    fun exportSingleFrame(frame: ExtractedFrame, onComplete: (Uri?) -> Unit) {
        viewModelScope.launch {
            _exportStatusMessage.value = "Saving frame..."
            val adj = _viewerAdjustments.value
            val adjustedBmp = if (hasActiveAdjustments(adj)) {
                renderAdjustedBitmap(frame.imagePath, adj)
            } else null

            val result = ImageExporter.saveFrameToGallery(
                context = getApplication(),
                frame = frame,
                config = _exportConfig.value,
                adjustedBitmap = adjustedBmp
            )
            result.onSuccess { uri ->
                _exportStatusMessage.value = "Saved successfully to Gallery!"
                onComplete(uri)
            }.onFailure { err ->
                _exportStatusMessage.value = "Export failed: ${err.message}"
                onComplete(null)
            }
        }
    }

    fun exportSelectedFramesBatch(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val selected = getSelectedOrAllFrames()
            var count = 0
            _exportStatusMessage.value = "Exporting ${selected.size} frames to Gallery..."
            for (f in selected) {
                val res = ImageExporter.saveFrameToGallery(getApplication(), f, _exportConfig.value)
                if (res.isSuccess) count++
            }
            _exportStatusMessage.value = "Exported $count frames to Gallery!"
            onComplete(count)
        }
    }

    fun exportZipArchive(onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            val frames = getSelectedOrAllFrames()
            val title = _videoMetadata.value?.filename ?: "Video"
            _exportStatusMessage.value = "Compiling ZIP archive..."
            val result = ImageExporter.createZipArchive(getApplication(), frames, title, _exportConfig.value)
            result.onSuccess { file ->
                _exportStatusMessage.value = "Archive created (${TimestampFormatter.formatFileSize(file.length())})"
                onComplete(file)
            }.onFailure { err ->
                _exportStatusMessage.value = "Failed to create archive: ${err.message}"
                onComplete(null)
            }
        }
    }

    fun exportContactSheet(onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            val frames = getSelectedOrAllFrames().take(36) // Cap contact sheet at 36 frames for clear resolution
            val title = _videoMetadata.value?.filename ?: "Video"
            _exportStatusMessage.value = "Generating Contact Sheet..."
            val result = ContactSheetGenerator.generateContactSheet(
                context = getApplication(),
                frames = frames,
                projectTitle = title,
                columns = _exportConfig.value.contactSheetColumns
            )
            result.onSuccess { file ->
                _exportStatusMessage.value = "Contact Sheet generated!"
                onComplete(file)
            }.onFailure { err ->
                _exportStatusMessage.value = "Failed: ${err.message}"
                onComplete(null)
            }
        }
    }

    // Batch Processing
    fun addToBatchQueue(uri: Uri) {
        viewModelScope.launch {
            val metaResult = VideoFrameExtractor.extractMetadata(getApplication(), uri)
            metaResult.onSuccess { meta ->
                val newJob = BatchJob(
                    id = UUID.randomUUID().toString(),
                    videoUriString = uri.toString(),
                    videoTitle = meta.filename,
                    durationMs = meta.durationMs,
                    config = ExtractionConfig(
                        startTimestampUs = 0L,
                        endTimestampUs = meta.durationMs * 1000L,
                        intervalUs = 33_333L
                    )
                )
                _batchQueue.value = _batchQueue.value + newJob
            }
        }
    }

    fun removeBatchJob(jobId: String) {
        _batchQueue.value = _batchQueue.value.filter { it.id != jobId }
    }

    // Helper functions
    fun getFilteredFrames(): List<ExtractedFrame> {
        val list = _extractedFrames.value
        val query = _searchQuery.value.trim().lowercase()

        val filtered = list.filter { f ->
            val matchesFilter = when (_galleryFilter.value) {
                GalleryFilter.ALL -> true
                GalleryFilter.SITUATION_MATCH -> f.situationMatchScore >= 50
                GalleryFilter.TOP_PICKS -> f.aiScore >= 80
                GalleryFilter.SHARPEST -> f.sharpnessScore >= 65f
                GalleryFilter.WELL_EXPOSED -> f.exposureScore >= 75f
                GalleryFilter.FAVORITES -> f.isFavorite
                GalleryFilter.NO_DUPLICATES -> !f.isDuplicate
            }
            val matchesQuery = query.isEmpty() ||
                    f.timestampFormatted.lowercase().contains(query) ||
                    "#${f.index + 1}".contains(query) ||
                    (f.situationMatchReason?.lowercase()?.contains(query) == true)
            matchesFilter && matchesQuery
        }

        return when (_gallerySort.value) {
            GallerySort.TIMESTAMP_ASC -> filtered.sortedBy { it.requestedTimestampUs }
            GallerySort.TIMESTAMP_DESC -> filtered.sortedByDescending { it.requestedTimestampUs }
            GallerySort.AI_SCORE_DESC -> filtered.sortedByDescending { it.aiScore }
            GallerySort.SHARPNESS_DESC -> filtered.sortedByDescending { it.sharpnessScore }
            GallerySort.FILE_SIZE_DESC -> filtered.sortedByDescending { it.fileSize }
            GallerySort.SITUATION_MATCH_DESC -> filtered.sortedByDescending { it.situationMatchScore }
        }
    }

    fun getSelectedOrAllFrames(): List<ExtractedFrame> {
        val selected = _extractedFrames.value.filter { it.id in _selectedFrameIds.value }
        return if (selected.isNotEmpty()) selected else _extractedFrames.value
    }

    private fun hasActiveAdjustments(adj: ViewerAdjustments): Boolean {
        return adj.brightness != 0f || adj.contrast != 1.0f || adj.saturation != 1.0f || adj.rotation != 0 ||
                (adj.dslrMasterEnhanceEnabled && adj.dslrProfile != com.example.util.DslrProfile.NONE)
    }

    private fun renderAdjustedBitmap(imagePath: String, adj: ViewerAdjustments): Bitmap? {
        val original = BitmapFactory.decodeFile(imagePath) ?: return null
        val w = original.width
        val h = original.height

        val matrix = Matrix()
        if (adj.rotation != 0) {
            matrix.postRotate(adj.rotation.toFloat())
        }

        val rotated = Bitmap.createBitmap(original, 0, 0, w, h, matrix, true)
        if (rotated !== original) original.recycle()

        // 1. Apply DSLR Master Photographic Enhancement if enabled
        val dslrStage = if (adj.dslrMasterEnhanceEnabled && adj.dslrProfile != com.example.util.DslrProfile.NONE) {
            val enhanced = com.example.util.DslrImageEnhancer.enhanceBitmap(
                rotated,
                com.example.util.DslrEnhanceParams(
                    profile = adj.dslrProfile,
                    sharpnessAmount = if (adj.sharpnessBoost) 0.65f else 0.45f,
                    microContrast = adj.microClarity,
                    shadowRecovery = adj.shadowLift,
                    colorVibrance = (adj.saturation - 1.0f).coerceIn(-0.5f, 0.5f)
                )
            )
            if (enhanced !== rotated) rotated.recycle()
            enhanced
        } else {
            rotated
        }

        // 2. Apply any secondary fine-tuning adjustments
        if (adj.brightness != 0f || adj.contrast != 1.0f) {
            val cm = ColorMatrix()
            val scale = adj.contrast
            val translate = (1f - scale) * 128f + (adj.brightness * 2.55f)
            val cmContrast = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(cmContrast)

            val output = Bitmap.createBitmap(dslrStage.width, dslrStage.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(cm)
            }
            canvas.drawBitmap(dslrStage, 0f, 0f, paint)
            dslrStage.recycle()
            return output
        }

        return dslrStage
    }
}
