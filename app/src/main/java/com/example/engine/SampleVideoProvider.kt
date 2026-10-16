package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

object SampleVideoProvider {

    private const val VIDEO_WIDTH = 640
    private const val VIDEO_HEIGHT = 360
    private const val FRAME_RATE = 30
    private const val BIT_RATE = 2_000_000
    private const val I_FRAME_INTERVAL = 1
    private const val DURATION_SECONDS = 3

    /**
     * Creates a demo video file featuring high-precision millisecond timestamps, frame counters,
     * and dynamic geometric motion so the user can immediately test precision extraction.
     */
    suspend fun getOrCreateSampleVideo(context: Context): Result<Uri> = withContext(Dispatchers.IO) {
        val sampleFile = File(context.filesDir, "photoframe_demo_sample.mp4")
        if (sampleFile.exists() && sampleFile.length() > 10_000) {
            return@withContext Result.success(Uri.fromFile(sampleFile))
        }

        try {
            val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
            val format = MediaFormat.createVideoFormat(mimeType, VIDEO_WIDTH, VIDEO_HEIGHT).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            }

            val encoder = MediaCodec.createEncoderByType(mimeType)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            val muxer = MediaMuxer(sampleFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false

            val totalFrames = FRAME_RATE * DURATION_SECONDS
            val bufferInfo = MediaCodec.BufferInfo()

            val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val paintTimer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#25D9F8") // Neon cyan
                textSize = 42f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            val paintSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#7C5CFF") // Violet
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val paintShape = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

            for (frameNum in 0 until totalFrames) {
                val canvas: Canvas = inputSurface.lockCanvas(null)
                try {
                    // 1. Midnight background
                    canvas.drawColor(Color.parseColor("#0B1020"))

                    val timeMs = (frameNum * 1000L) / FRAME_RATE
                    val seconds = timeMs / 1000
                    val ms = timeMs % 1000

                    // 2. Animated Moving Shapes
                    val progress = frameNum.toFloat() / totalFrames
                    val ballX = 80f + progress * (VIDEO_WIDTH - 160f)
                    val ballY = 180f + (kotlin.math.sin(progress * Math.PI * 4) * 50f).toFloat()

                    paintShape.color = Color.parseColor("#7C5CFF")
                    canvas.drawCircle(ballX, ballY, 32f, paintShape)

                    paintShape.color = Color.parseColor("#34D399")
                    canvas.drawRect(VIDEO_WIDTH - ballX - 25f, ballY - 25f, VIDEO_WIDTH - ballX + 25f, ballY + 25f, paintShape)

                    // 3. High Precision Timecode Overlay
                    canvas.drawText("PHOTO FRAME DEMO", 30f, 50f, paintTitle)
                    val timecodeStr = String.format(Locale.US, "00:00:%02d.%03d", seconds, ms)
                    canvas.drawText(timecodeStr, 30f, 110f, paintTimer)
                    canvas.drawText("Frame #${frameNum + 1} of $totalFrames  (30 FPS)", 30f, 145f, paintSub)

                    // Branding tag
                    canvas.drawText("MG Shankar Tech", 30f, VIDEO_HEIGHT - 30f, paintSub)
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                drainEncoder(encoder, muxer, bufferInfo, false, { idx ->
                    trackIndex = idx
                    muxerStarted = true
                }, { trackIndex })
            }

            encoder.signalEndOfInputStream()
            drainEncoder(encoder, muxer, bufferInfo, true, { idx ->
                trackIndex = idx
                muxerStarted = true
            }, { trackIndex })

            encoder.stop()
            encoder.release()
            inputSurface.release()

            if (muxerStarted) {
                muxer.stop()
                muxer.release()
            }

            Result.success(Uri.fromFile(sampleFile))
        } catch (e: Exception) {
            // If device cannot encode MP4, return failure
            Result.failure(e)
        }
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean,
        onMuxerStart: (Int) -> Unit,
        getTrackIndex: () -> Int
    ) {
        val timeoutUs = 10_000L
        while (true) {
            val status = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (status == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break
            } else if (status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = encoder.outputFormat
                val trackIdx = muxer.addTrack(newFormat)
                muxer.start()
                onMuxerStart(trackIdx)
            } else if (status >= 0) {
                val encodedData = encoder.getOutputBuffer(status)
                if (encodedData != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    if (bufferInfo.size != 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        val track = getTrackIndex()
                        if (track != -1) {
                            muxer.writeSampleData(track, encodedData, bufferInfo)
                        }
                    }
                }
                encoder.releaseOutputBuffer(status, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }
}
