package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class DslrProfile {
    NONE,
    DSLR_PRIME,         // Optical sharpness, wide dynamic range, rich blacks
    STUDIO_VIVID,       // Punchy colors for sports, cars, and action shots
    CINEMATIC_TEAL_ORANGE, // Hollywood cinematic film curve
    PORTRAIT_GLOW,      // Smooth skin tones with tack-sharp eye details
    LEICA_MONOCHROME    // High-contrast fine-art black & white
}

data class DslrEnhanceParams(
    val profile: DslrProfile = DslrProfile.DSLR_PRIME,
    val sharpnessAmount: Float = 0.5f,     // 0.0 to 1.0 (Unsharp mask strength)
    val microContrast: Float = 0.25f,      // 0.0 to 1.0 (Midtone clarity boost)
    val shadowRecovery: Float = 0.20f,     // 0.0 to 1.0 (Lifting crushed shadow detail)
    val highlightProtection: Float = 0.15f,// 0.0 to 1.0 (Preventing clipped whites)
    val colorVibrance: Float = 0.20f,      // 0.0 to 1.0
    val opticalVignette: Float = 0.15f     // 0.0 to 1.0 (Subtle f/1.8 prime lens vignette)
)

object DslrImageEnhancer {

    /**
     * Applies full DSLR-grade photographic post-processing to an image bitmap.
     * Uses micro-contrast enhancement, unsharp masking, dynamic range balancing,
     * and high-end optical color grading curves.
     */
    fun enhanceBitmap(
        source: Bitmap,
        params: DslrEnhanceParams = DslrEnhanceParams()
    ): Bitmap {
        val width = source.width
        val height = source.height

        // 1. Create mutable working bitmap in ARGB_8888
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 2. Compute professional color matrix according to chosen DSLR profile
        val cm = buildDslrColorMatrix(params)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        // 3. Apply Optical Prime Lens Vignette if enabled
        if (params.opticalVignette > 0.05f) {
            applyLensVignette(canvas, width, height, params.opticalVignette)
        }

        // 4. Optical Unsharp Mask Sharpening Kernel (DSLR High-Pass Micro-Contrast)
        if (params.sharpnessAmount > 0.05f) {
            applyUnsharpMask(output, params.sharpnessAmount)
        }

        return output
    }

    private fun buildDslrColorMatrix(params: DslrEnhanceParams): ColorMatrix {
        val cm = ColorMatrix()

        // Base Vibrancy & Saturation
        val baseSat = when (params.profile) {
            DslrProfile.LEICA_MONOCHROME -> 0.0f
            DslrProfile.STUDIO_VIVID -> 1.35f + params.colorVibrance
            DslrProfile.CINEMATIC_TEAL_ORANGE -> 1.15f + params.colorVibrance
            DslrProfile.PORTRAIT_GLOW -> 1.08f + params.colorVibrance
            DslrProfile.DSLR_PRIME -> 1.12f + params.colorVibrance
            DslrProfile.NONE -> 1.0f + params.colorVibrance
        }
        cm.setSaturation(baseSat)

        // Contrast & Midtone Clarity (S-Curve)
        val contrastFactor = 1.0f + params.microContrast * 0.45f
        val shadowLift = params.shadowRecovery * 35f
        val highlightCompress = -params.highlightProtection * 25f
        val offset = (1f - contrastFactor) * 128f + shadowLift + highlightCompress

        val cmClarity = ColorMatrix(floatArrayOf(
            contrastFactor, 0f, 0f, 0f, offset,
            0f, contrastFactor, 0f, 0f, offset,
            0f, 0f, contrastFactor, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(cmClarity)

        // Profile-Specific Color Grading (Color Temperature & Tone Balance)
        when (params.profile) {
            DslrProfile.CINEMATIC_TEAL_ORANGE -> {
                // Warm skin tone highlights, cool deep shadows
                val cmFilm = ColorMatrix(floatArrayOf(
                    1.08f, 0f, 0f, 0f, 8f,    // Boost red/warm
                    0f, 1.02f, 0f, 0f, 2f,
                    0f, 0f, 1.12f, 0f, -4f,   // Teal/cyan tint in mid-shadows
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(cmFilm)
            }
            DslrProfile.DSLR_PRIME -> {
                // Neutral optical perfection (clean whites, vivid primaries)
                val cmPrime = ColorMatrix(floatArrayOf(
                    1.04f, 0f, 0f, 0f, 4f,
                    0f, 1.04f, 0f, 0f, 4f,
                    0f, 0f, 1.05f, 0f, 4f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(cmPrime)
            }
            DslrProfile.STUDIO_VIVID -> {
                // Saturated automotive & sports pop (boosts reds, blues, greens)
                val cmVivid = ColorMatrix(floatArrayOf(
                    1.12f, 0f, 0f, 0f, 5f,
                    0f, 1.10f, 0f, 0f, 5f,
                    0f, 0f, 1.15f, 0f, 8f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(cmVivid)
            }
            DslrProfile.PORTRAIT_GLOW -> {
                // Soft warm glow in skin tones
                val cmPortrait = ColorMatrix(floatArrayOf(
                    1.06f, 0f, 0f, 0f, 10f,
                    0f, 1.02f, 0f, 0f, 4f,
                    0f, 0f, 0.98f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(cmPortrait)
            }
            else -> {}
        }

        return cm
    }

    /**
     * Subtle f/1.8 optical prime lens peripheral light falloff (vignette).
     */
    private fun applyLensVignette(canvas: Canvas, w: Int, h: Int, strength: Float) {
        val cx = w / 2f
        val cy = h / 2f
        val radius = kotlin.math.hypot(cx.toDouble(), cy.toDouble()).toFloat()

        val alpha = (strength * 120f).toInt().coerceIn(10, 160)
        val colors = intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(alpha, 5, 8, 15))
        val stops = floatArrayOf(0.0f, 0.55f, 1.0f)

        val vignetteShader = RadialGradient(
            cx, cy, radius,
            colors, stops,
            Shader.TileMode.CLAMP
        )

        val vPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = vignetteShader
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), vPaint)
    }

    /**
     * 3x3 Optical High-Pass Unsharp Masking Kernel.
     * Computes micro-edge enhancement without introducing coarse halos.
     */
    private fun applyUnsharpMask(bitmap: Bitmap, strength: Float) {
        val w = bitmap.width
        val h = bitmap.height

        // Downsample slightly for ultra-fast convolution if image is very large
        val isLarge = w > 1920 || h > 1080
        val sampleW = if (isLarge) w / 2 else w
        val sampleH = if (isLarge) h / 2 else h

        val workingBmp = if (isLarge) Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true) else bitmap
        val pixels = IntArray(sampleW * sampleH)
        workingBmp.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)

        val outPixels = IntArray(pixels.size)
        System.arraycopy(pixels, 0, outPixels, 0, pixels.size)

        val factor = (strength * 1.5f).coerceIn(0.1f, 2.0f)

        for (y in 1 until sampleH - 1) {
            val rowOffset = y * sampleW
            val topOffset = (y - 1) * sampleW
            val botOffset = (y + 1) * sampleW

            for (x in 1 until sampleW - 1) {
                val center = pixels[rowOffset + x]
                val top = pixels[topOffset + x]
                val bot = pixels[botOffset + x]
                val left = pixels[rowOffset + (x - 1)]
                val right = pixels[rowOffset + (x + 1)]

                val cr = (center shr 16) and 0xFF
                val cg = (center shr 8) and 0xFF
                val cb = center and 0xFF

                // Blur average of 4-neighbors
                val avgR = (((top shr 16) and 0xFF) + ((bot shr 16) and 0xFF) + ((left shr 16) and 0xFF) + ((right shr 16) and 0xFF)) shr 2
                val avgG = (((top shr 8) and 0xFF) + ((bot shr 8) and 0xFF) + ((left shr 8) and 0xFF) + ((right shr 8) and 0xFF)) shr 2
                val avgB = ((top and 0xFF) + (bot and 0xFF) + (left and 0xFF) + (right and 0xFF)) shr 2

                val nr = (cr + (cr - avgR) * factor).toInt().coerceIn(0, 255)
                val ng = (cg + (cg - avgG) * factor).toInt().coerceIn(0, 255)
                val nb = (cb + (cb - avgB) * factor).toInt().coerceIn(0, 255)

                outPixels[rowOffset + x] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
        }

        if (isLarge) {
            val sharpenedSmall = Bitmap.createBitmap(sampleW, sampleH, Bitmap.Config.ARGB_8888)
            sharpenedSmall.setPixels(outPixels, 0, sampleW, 0, 0, sampleW, sampleH)
            val canvas = Canvas(bitmap)
            val p = Paint(Paint.FILTER_BITMAP_FLAG)
            val scaleMatrix = Matrix().apply { postScale(w.toFloat() / sampleW, h.toFloat() / sampleH) }
            canvas.drawBitmap(sharpenedSmall, scaleMatrix, p)
            sharpenedSmall.recycle()
            workingBmp.recycle()
        } else {
            bitmap.setPixels(outPixels, 0, sampleW, 0, 0, sampleW, sampleH)
        }
    }
}
