package com.example

import com.example.util.ComputerVisionAnalyzer
import com.example.util.TimestampFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTimestampFormatting_microsecondPrecision() {
        // 5 seconds and 250 milliseconds in microseconds
        val timeUs = 5_250_000L
        val formatted = TimestampFormatter.formatUsToHhMmSsMmm(timeUs)
        assertEquals("00:00:05.250", formatted)

        // 1 hour, 23 minutes, 45 seconds, 678 ms
        val hourUs = (3600L + 23 * 60L + 45L) * 1_000_000L + 678_000L
        val hourFormatted = TimestampFormatter.formatUsToHhMmSsMmm(hourUs)
        assertEquals("01:23:45.678", hourFormatted)
    }

    @Test
    fun testFilenameTimestampFormatting() {
        val timeUs = 5_250_000L
        val filenameTs = TimestampFormatter.formatUsToFilenameTimestamp(timeUs)
        assertEquals("00-00-05-250", filenameTs)
    }

    @Test
    fun testTimestampParsing() {
        val input = "00:00:05.250"
        val parsedUs = TimestampFormatter.parseTimestampToUs(input)
        assertNotNull(parsedUs)
        assertEquals(5_250_000L, parsedUs)

        val mmSs = "01:10.500"
        val parsedMmSs = TimestampFormatter.parseTimestampToUs(mmSs)
        assertNotNull(parsedMmSs)
        assertEquals(70_500_000L, parsedMmSs)
    }

    @Test
    fun testTimestampSequenceGeneration_noFloatingPointDrift() {
        // Start: 2.000s, End: 8.000s, Interval: 125ms (125,000 us)
        val startUs = 2_000_000L
        val endUs = 8_000_000L
        val intervalUs = 125_000L
        val timestamps = TimestampFormatter.generateTimestamps(startUs, endUs, intervalUs, maxCount = 100)

        assertEquals(49, timestamps.size)
        assertEquals(2_000_000L, timestamps[0])
        assertEquals(2_125_000L, timestamps[1])
        assertEquals(2_250_000L, timestamps[2])
        assertEquals(8_000_000L, timestamps.last())

        // Verify every element is an exact integer multiple without decimal drift
        for (i in timestamps.indices) {
            val expected = startUs + i * intervalUs
            assertEquals("Timestamp at index $i had drift", expected, timestamps[i])
        }
    }

    @Test
    fun testEstimatedFrameCount() {
        // 1 second (1,000,000 us) with 33,333 us interval (~30 FPS)
        val count = TimestampFormatter.calculateEstimatedFrameCount(0L, 1_000_000L, 33_333L)
        assertEquals(31, count) // 0 to 30 = 31 frames inclusive
    }

    @Test
    fun testHammingDistance() {
        val hash1 = 0b11001100L
        val hash2 = 0b11001101L // 1 bit difference
        val dist = ComputerVisionAnalyzer.hammingDistance(hash1, hash2)
        assertEquals(1, dist)

        val identicalDist = ComputerVisionAnalyzer.hammingDistance(0xCAFEBABEL, 0xCAFEBABEL)
        assertEquals(0, identicalDist)
    }

    @Test
    fun testDurationAndFileSizeFormatting() {
        val dur = TimestampFormatter.formatDurationMs(65_250L) // 1 min 5 sec 250 ms
        assertEquals("01:05.250", dur)

        val size = TimestampFormatter.formatFileSize(15_728_640L)
        assertTrue(size.contains("15.0 MB"))
    }

    @Test
    fun testSituationFrameFinder_detectsAccidentMoment() = kotlinx.coroutines.test.runTest {
        val frames = listOf(
            com.example.model.ExtractedFrame(
                id = "f0", index = 0, requestedTimestampUs = 0L, actualTimestampUs = 0L,
                timestampFormatted = "00:00:00.000", imagePath = "", thumbPath = "",
                width = 1920, height = 1080, fileSize = 1000L, sharpnessScore = 50f, exposureScore = 70f, duplicateDistance = 1
            ),
            com.example.model.ExtractedFrame(
                id = "f1", index = 1, requestedTimestampUs = 500_000L, actualTimestampUs = 500_000L,
                timestampFormatted = "00:00:00.500", imagePath = "", thumbPath = "",
                width = 1920, height = 1080, fileSize = 1000L, sharpnessScore = 52f, exposureScore = 71f, duplicateDistance = 2
            ),
            // Frame 2 has sharp impact / visual collision spike
            com.example.model.ExtractedFrame(
                id = "f2", index = 2, requestedTimestampUs = 1_000_000L, actualTimestampUs = 1_000_000L,
                timestampFormatted = "00:00:01.000", imagePath = "", thumbPath = "",
                width = 1920, height = 1080, fileSize = 1000L, sharpnessScore = 95f, exposureScore = 40f, duplicateDistance = 28
            ),
            com.example.model.ExtractedFrame(
                id = "f3", index = 3, requestedTimestampUs = 1_500_000L, actualTimestampUs = 1_500_000L,
                timestampFormatted = "00:00:01.500", imagePath = "", thumbPath = "",
                width = 1920, height = 1080, fileSize = 1000L, sharpnessScore = 55f, exposureScore = 68f, duplicateDistance = 3
            )
        )

        val (updated, result) = com.example.ai.SituationFrameFinder.findSituationFrames(
            "कार दौड़ रही है उसका एक्सीडेंट हो गया उस टाइमिंग का शॉट",
            frames
        )

        assertNotNull(result)
        assertEquals("f2", result.winningFrameId)
        assertTrue(result.explanation.contains("00:00:01.000") || result.explanation.contains("impact") || result.explanation.contains("timing"))
        val winningFrame = updated.find { it.id == "f2" }
        assertNotNull(winningFrame)
        assertTrue((winningFrame?.situationMatchScore ?: 0) >= 90)
    }
}
