package com.logflare.qa.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class DeviceImageComparatorTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val comparator = DeviceImageComparator(
        channelTolerance = 2,
        maxChangedRatio = 0.0001,
    )

    private fun writePng(file: File, width: Int, height: Int, paint: (BufferedImage) -> Unit) {
        file.parentFile?.mkdirs()
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        paint(image)
        ImageIO.write(image, "png", file)
    }

    private fun solid(file: File, width: Int, height: Int, argb: Int) {
        writePng(file, width, height) { img ->
            for (y in 0 until height) {
                for (x in 0 until width) {
                    img.setRGB(x, y, argb)
                }
            }
        }
    }

    @Test
    fun compareIdenticalImagesIsMatchAndDeletesStaleDiff() {
        val expected = temp.newFile("expected.png")
        val actual = temp.newFile("actual.png")
        val diff = temp.newFile("diff.png")
        solid(expected, 10, 10, 0xFF112233.toInt())
        solid(actual, 10, 10, 0xFF112233.toInt())
        diff.writeText("stale")

        val expectedBytesBefore = expected.readBytes()
        assertEquals(CompareResult.Match, comparator.compare(expected, actual, diff))
        assertFalse("stale diff must be deleted on Match", diff.exists())
        assertTrue(expectedBytesBefore.contentEquals(expected.readBytes()))
    }

    @Test
    fun compareDimensionMismatch() {
        val expected = temp.newFile("expected-dim.png")
        val actual = temp.newFile("actual-dim.png")
        val diff = File(temp.root, "nested/diff-dim.png")
        solid(expected, 8, 8, 0xFF000000.toInt())
        solid(actual, 8, 9, 0xFF000000.toInt())

        val result = comparator.compare(expected, actual, diff)
        assertTrue(result is CompareResult.DimensionMismatch)
        assertFalse(diff.exists())
    }

    @Test
    fun compareVisiblyChangedWritesMagentaDiff() {
        val expected = temp.newFile("expected-chg.png")
        val actual = temp.newFile("actual-chg.png")
        val diff = File(temp.root, "out/diff-chg.png")
        // 100x100 => threshold is 1 pixel (ratio 0.0001). Change 2 pixels to exceed.
        solid(expected, 100, 100, 0xFF101010.toInt())
        writePng(actual, 100, 100) { img ->
            for (y in 0 until 100) {
                for (x in 0 until 100) {
                    img.setRGB(x, y, 0xFF101010.toInt())
                }
            }
            img.setRGB(0, 0, 0xFFFF0000.toInt())
            img.setRGB(1, 0, 0xFFFF0000.toInt())
        }

        val expectedBytesBefore = expected.readBytes()
        val result = comparator.compare(expected, actual, diff)
        assertTrue(result is CompareResult.Changed)
        val changed = result as CompareResult.Changed
        assertTrue(changed.changedPixels >= 2)
        assertTrue(changed.changedRatio > 0.0001)
        assertTrue(diff.exists())
        assertTrue(expectedBytesBefore.contentEquals(expected.readBytes()))

        val diffImage = ImageIO.read(diff)
        assertEquals(0xFFFF00FF.toInt(), diffImage.getRGB(0, 0))
        val unchanged = diffImage.getRGB(50, 50)
        val alpha = (unchanged ushr 24) and 0xFF
        assertEquals(51, alpha)
        assertEquals(0x10, (unchanged ushr 16) and 0xFF)
        assertEquals(0x10, (unchanged ushr 8) and 0xFF)
        assertEquals(0x10, unchanged and 0xFF)
    }

    @Test
    fun channelToleranceAllowsDeltaOfTwo() {
        val expected = temp.newFile("expected-tol.png")
        val actual = temp.newFile("actual-tol.png")
        val diff = temp.newFile("diff-tol.png")
        solid(expected, 4, 4, 0xFF101010.toInt())
        solid(actual, 4, 4, 0xFF121212.toInt()) // delta 2 on RGB

        assertEquals(CompareResult.Match, comparator.compare(expected, actual, diff))
        assertFalse(diff.exists())
    }

    @Test
    fun alphaChannelIsCompared() {
        val expected = temp.newFile("expected-a.png")
        val actual = temp.newFile("actual-a.png")
        val diff = temp.newFile("diff-a.png")
        // 20x20 = 400 pixels; change 1 alpha-only pixel => ratio 0.0025 > 0.0001
        solid(expected, 20, 20, 0xFF202020.toInt())
        writePng(actual, 20, 20) { img ->
            for (y in 0 until 20) {
                for (x in 0 until 20) {
                    img.setRGB(x, y, 0xFF202020.toInt())
                }
            }
            img.setRGB(0, 0, 0x00202020)
        }

        val result = comparator.compare(expected, actual, diff)
        assertTrue(result is CompareResult.Changed)
        assertTrue(diff.exists())
    }

    @Test
    fun invalidImageReturnsInvalidResult() {
        val expected = temp.newFile("expected-bad.png")
        val actual = temp.newFile("actual-bad.png")
        val diff = temp.newFile("diff-bad.png")
        solid(expected, 2, 2, 0xFFFFFFFF.toInt())
        actual.writeText("not-a-png")

        val result = comparator.compare(expected, actual, diff)
        assertTrue(result is CompareResult.InvalidImage)
    }

    @Test
    fun recordDeviceCopiesActualBytesAndCreatesParents() {
        val actual = temp.newFile("capture.png")
        val expected = File(temp.root, "baselines/nested/capture.png")
        solid(actual, 3, 3, 0xFFABCDEF.toInt())
        val bytes = actual.readBytes()

        DeviceImageComparator.recordDevice(actual = actual, expected = expected)

        assertTrue(expected.exists())
        assertTrue(bytes.contentEquals(expected.readBytes()))
    }

    @Test
    fun recordDeviceRejectsNonPng() {
        val actual = temp.newFile("capture.txt")
        val expected = File(temp.root, "baselines/capture.png")
        actual.writeText("nope")

        try {
            DeviceImageComparator.recordDevice(actual = actual, expected = expected)
            fail("expected failure for non-png")
        } catch (_: IllegalArgumentException) {
            // expected
        }
        assertFalse(expected.exists())
    }
}
