package com.logflare.qa.image

import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
        val expectedImage = readImage(expected) ?: return invalid(diff, "cannot read expected: ${expected.path}")
        val actualImage = readImage(actual) ?: return invalid(diff, "cannot read actual: ${actual.path}")

        if (expectedImage.width != actualImage.width || expectedImage.height != actualImage.height) {
            deleteStaleDiff(diff)
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
            return invalid(diff, "empty image")
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
            writePngAtomically(diffImage, diff)
            CompareResult.Changed(changedPixels = changedPixels, changedRatio = ratio)
        } else {
            deleteStaleDiff(diff)
            CompareResult.Match
        }
    }

    private fun invalid(diff: File, reason: String): CompareResult.InvalidImage {
        deleteStaleDiff(diff)
        return CompareResult.InvalidImage(reason)
    }

    private fun deleteStaleDiff(diff: File) {
        if (diff.exists() && !diff.delete()) {
            throw IllegalStateException("cannot delete stale diff: ${diff.path}")
        }
    }

    private fun writePngAtomically(image: BufferedImage, destination: File) {
        val parent = destination.absoluteFile.parentFile
        parent.mkdirs()
        val temp = Files.createTempFile(parent.toPath(), "${destination.name}.tmp-", ".png")
        try {
            check(ImageIO.write(image, "png", temp.toFile())) { "PNG writer unavailable" }
            moveReplacing(temp, destination.toPath())
        } finally {
            Files.deleteIfExists(temp)
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
        private val PNG_SIGNATURE = byteArrayOf(
            0x89.toByte(),
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A,
            0x0A,
        )

        fun recordDevice(actual: File, expected: File) {
            if (!actual.isFile) {
                throw IllegalArgumentException("actual image missing: ${actual.path}")
            }
            if (!hasPngSignature(actual)) {
                throw IllegalArgumentException("actual is not a PNG: ${actual.path}")
            }
            val image = ImageIO.read(actual)
                ?: throw IllegalArgumentException("actual is not a valid PNG: ${actual.path}")
            require(image.width > 0 && image.height > 0) { "actual PNG is empty: ${actual.path}" }

            val parent = expected.absoluteFile.parentFile
            parent.mkdirs()
            val temp = Files.createTempFile(parent.toPath(), "${expected.name}.tmp-", ".png")
            try {
                Files.copy(actual.toPath(), temp, StandardCopyOption.REPLACE_EXISTING)
                moveReplacing(temp, expected.toPath())
            } finally {
                Files.deleteIfExists(temp)
            }
        }

        private fun hasPngSignature(file: File): Boolean {
            if (file.length() < PNG_SIGNATURE.size) return false
            val signature = ByteArray(PNG_SIGNATURE.size)
            file.inputStream().use { input ->
                if (input.read(signature) != signature.size) return false
            }
            return signature.contentEquals(PNG_SIGNATURE)
        }

        private fun moveReplacing(source: java.nio.file.Path, destination: java.nio.file.Path) {
            try {
                Files.move(
                    source,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}
