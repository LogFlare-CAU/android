package com.logflare.qa.image

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs

sealed class CompareResult {
    data object Match : CompareResult()

    data class Changed(
        val changedPixels: Int,
        val changedRatio: Double,
    ) : CompareResult()

    data class DimensionMismatch(
        val expectedWidth: Int,
        val expectedHeight: Int,
        val actualWidth: Int,
        val actualHeight: Int,
    ) : CompareResult()

    data class InvalidImage(val reason: String) : CompareResult()
}

class DeviceImageComparator(
    private val channelTolerance: Int = 2,
    private val maxChangedRatio: Double = 0.0001,
) {
    fun compare(expected: File, actual: File, diff: File): CompareResult {
        val expectedImage = readImage(expected) ?: return CompareResult.InvalidImage("cannot read expected: ${expected.path}")
        val actualImage = readImage(actual) ?: return CompareResult.InvalidImage("cannot read actual: ${actual.path}")

        if (expectedImage.width != actualImage.width || expectedImage.height != actualImage.height) {
            return CompareResult.DimensionMismatch(
                expectedWidth = expectedImage.width,
                expectedHeight = expectedImage.height,
                actualWidth = actualImage.width,
                actualHeight = actualImage.height,
            )
        }

        val width = expectedImage.width
        val height = expectedImage.height
        val total = width.toLong() * height.toLong()
        if (total == 0L) {
            return CompareResult.InvalidImage("empty image")
        }

        val diffImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        var changedPixels = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val e = expectedImage.getRGB(x, y)
                val a = actualImage.getRGB(x, y)
                val changed = channelDelta(e, a) > channelTolerance
                if (changed) {
                    changedPixels++
                    diffImage.setRGB(x, y, MAGENTA_OPAQUE)
                } else {
                    val r = (e ushr 16) and 0xFF
                    val g = (e ushr 8) and 0xFF
                    val b = e and 0xFF
                    diffImage.setRGB(x, y, (UNCHANGED_ALPHA shl 24) or (r shl 16) or (g shl 8) or b)
                }
            }
        }

        val ratio = changedPixels.toDouble() / total.toDouble()
        return if (ratio > maxChangedRatio) {
            diff.parentFile?.mkdirs()
            ImageIO.write(diffImage, "png", diff)
            CompareResult.Changed(changedPixels = changedPixels, changedRatio = ratio)
        } else {
            if (diff.exists()) {
                diff.delete()
            }
            CompareResult.Match
        }
    }

    private fun channelDelta(expected: Int, actual: Int): Int {
        val ea = (expected ushr 24) and 0xFF
        val er = (expected ushr 16) and 0xFF
        val eg = (expected ushr 8) and 0xFF
        val eb = expected and 0xFF
        val aa = (actual ushr 24) and 0xFF
        val ar = (actual ushr 16) and 0xFF
        val ag = (actual ushr 8) and 0xFF
        val ab = actual and 0xFF
        return maxOf(abs(ea - aa), abs(er - ar), abs(eg - ag), abs(eb - ab))
    }

    private fun readImage(file: File): BufferedImage? {
        if (!file.isFile) return null
        return runCatching { ImageIO.read(file) }.getOrNull()
    }

    companion object {
        private const val MAGENTA_OPAQUE = 0xFFFF00FF.toInt()
        private const val UNCHANGED_ALPHA = 51 // 20% of 255

        fun recordDevice(actual: File, expected: File) {
            if (!actual.isFile) {
                throw IllegalArgumentException("actual image missing: ${actual.path}")
            }
            val image = ImageIO.read(actual)
                ?: throw IllegalArgumentException("actual is not a valid PNG: ${actual.path}")
            // Ensure it is readable as an image; copy original bytes to preserve PNG payload.
            check(image.width > 0 && image.height > 0)
            expected.parentFile?.mkdirs()
            actual.copyTo(expected, overwrite = true)
        }
    }
}
