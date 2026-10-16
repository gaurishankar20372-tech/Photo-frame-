package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class ImageAnalysisResult(
    val overallScore: Int,          // 0 to 100
    val sharpnessScore: Float,      // 0 to 100
    val exposureScore: Float,       // 0 to 100
    val contrastScore: Float,       // 0 to 100
    val vibrancyScore: Float,       // 0 to 100
    val perceptualHash: Long,       // 64-bit dHash
    val isClippedShadows: Boolean,
    val isBlownHighlights: Boolean
)

object ComputerVisionAnalyzer {

    /**
     * Efficiently analyzes a frame bitmap for photographic quality metrics.
     * Uses a fast downscaled representation to process frames in milliseconds.
     */
    fun analyzeFrame(bitmap: Bitmap): ImageAnalysisResult {
        // Downsample to 128x128 for rapid, consistent image signal processing
        val sampleSize = 128
        val sample = if (bitmap.width == sampleSize && bitmap.height == sampleSize) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, sampleSize, sampleSize, true)
        }

        val pixels = IntArray(sampleSize * sampleSize)
        sample.getPixels(pixels, 0, sampleSize, 0, 0, sampleSize, sampleSize)

        // If a new scaled bitmap was created, recycle it if not identical to source
        if (sample !== bitmap) {
            sample.recycle()
        }

        // 1. Grayscale luminance array & histogram
        val gray = FloatArray(pixels.size)
        val histogram = IntArray(256)
        var sumLum = 0f
        var sumSat = 0f

        val hsv = FloatArray(3)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF

            // Perceptual luminance (Rec. 601)
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            gray[i] = lum
            sumLum += lum

            val lumInt = lum.toInt().coerceIn(0, 255)
            histogram[lumInt]++

            // Color saturation
            Color.colorToHSV(c, hsv)
            sumSat += hsv[1]
        }

        val pixelCount = pixels.size
        val meanLum = sumLum / pixelCount
        val meanSat = sumSat / pixelCount

        // 2. Contrast: Standard deviation of luminance
        var varianceLum = 0f
        for (i in gray.indices) {
            val diff = gray[i] - meanLum
            varianceLum += diff * diff
        }
        val stdDevLum = sqrt(varianceLum / pixelCount)
        // Normalize contrast score (typically 20-75 std dev)
        val contrastScore = ((stdDevLum - 15f) / 50f * 100f).coerceIn(0f, 100f)

        // 3. Exposure quality
        val shadowClipped = histogram.take(15).sum().toFloat() / pixelCount
        val highlightClipped = histogram.takeLast(15).sum().toFloat() / pixelCount
        // Ideal exposure: mean luminance near 128, minimal clipping (< 10%)
        val exposureCenterPenalty = abs(meanLum - 128f) / 128f * 40f
        val clippingPenalty = (shadowClipped + highlightClipped) * 60f
        val exposureScore = (100f - exposureCenterPenalty - clippingPenalty).coerceIn(0f, 100f)

        // 4. Sharpness via modified discrete Laplacian variance
        var laplacianSum = 0f
        var laplacianSqSum = 0f
        var edgeCount = 0

        val w = sampleSize
        for (y in 1 until sampleSize - 1) {
            for (x in 1 until sampleSize - 1) {
                val center = gray[y * w + x]
                val top = gray[(y - 1) * w + x]
                val bottom = gray[(y + 1) * w + x]
                val left = gray[y * w + (x - 1)]
                val right = gray[y * w + (x + 1)]

                // Discrete 4-neighbor Laplacian kernel
                val lap = (top + bottom + left + right) - 4f * center
                laplacianSum += lap
                laplacianSqSum += lap * lap
                edgeCount++
            }
        }

        val lapMean = laplacianSum / edgeCount
        val lapVar = (laplacianSqSum / edgeCount) - (lapMean * lapMean)
        // Logarithmic scaling for human perceptual edge sharpness
        val sharpnessScore = if (lapVar > 1f) {
            (15f * kotlin.math.ln(lapVar)).coerceIn(0f, 100f)
        } else {
            0f
        }

        // 5. Color Vibrancy
        val vibrancyScore = (meanSat * 120f).coerceIn(0f, 100f)

        // 6. Perceptual 64-bit dHash (Difference Hash) using an 9x8 block
        val dHash = computeDHash(pixels, sampleSize)

        // 7. Composite AI Best-Photo Score (0 - 100)
        // Weights: Sharpness (40%), Exposure (25%), Contrast (20%), Vibrancy (15%)
        val composite = (
            sharpnessScore * 0.40f +
            exposureScore * 0.25f +
            contrastScore * 0.20f +
            vibrancyScore * 0.15f
        ).toInt().coerceIn(0, 100)

        return ImageAnalysisResult(
            overallScore = composite,
            sharpnessScore = sharpnessScore,
            exposureScore = exposureScore,
            contrastScore = contrastScore,
            vibrancyScore = vibrancyScore,
            perceptualHash = dHash,
            isClippedShadows = shadowClipped > 0.12f,
            isBlownHighlights = highlightClipped > 0.12f
        )
    }

    /**
     * Computes a 64-bit difference hash (dHash) to detect duplicate or near-duplicate frames.
     */
    private fun computeDHash(pixels: IntArray, size: Int): Long {
        // Sample down to 9 columns x 8 rows
        var hash = 0L
        val stepX = size / 9
        val stepY = size / 8

        for (row in 0 until 8) {
            val y = row * stepY
            for (col in 0 until 8) {
                val x1 = col * stepX
                val x2 = (col + 1) * stepX

                val p1 = pixels[(y * size + x1).coerceAtMost(pixels.size - 1)]
                val p2 = pixels[(y * size + x2).coerceAtMost(pixels.size - 1)]

                val lum1 = 0.299f * ((p1 shr 16) and 0xFF) + 0.587f * ((p1 shr 8) and 0xFF) + 0.114f * (p1 and 0xFF)
                val lum2 = 0.299f * ((p2 shr 16) and 0xFF) + 0.587f * ((p2 shr 8) and 0xFF) + 0.114f * (p2 and 0xFF)

                if (lum1 > lum2) {
                    hash = hash or (1L shl (row * 8 + col))
                }
            }
        }
        return hash
    }

    /**
     * Calculates the Hamming distance between two 64-bit perceptual hashes.
     * Distance <= 5 indicates virtually identical duplicate frames.
     */
    fun hammingDistance(hash1: Long, hash2: Long): Int {
        return java.lang.Long.bitCount(hash1 xor hash2)
    }
}
