package com.logflare.qa.image

import org.junit.Assert.assertArrayEquals
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

class TopBandCropperTest {
    @get:Rule
    val temp = TemporaryFolder()

    /** Paints row y entirely with colour 0xFF0000yy so every row is individually identifiable. */
    private fun rowCoded(file: File, width: Int, height: Int) {
        file.parentFile?.mkdirs()
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until height) {
            val argb = 0xFF000000.toInt() or y
            for (x in 0 until width) {
                image.setRGB(x, y, argb)
            }
        }
        ImageIO.write(image, "png", file)
    }

    private fun rowColours(file: File): IntArray {
        val image = ImageIO.read(file)
        return IntArray(image.height) { y -> image.getRGB(0, y) }
    }

    @Test
    fun cropTopRemovesBandAndKeepsEveryRemainingRowUnchanged() {
        val png = temp.newFile("capture.png")
        rowCoded(png, width = 8, height = 20)

        TopBandCropper.cropTop(image = png, pixels = 4, expectedHeight = 20)

        val cropped = ImageIO.read(png)
        assertEquals(8, cropped.width)
        assertEquals(16, cropped.height)
        // Row 0 of the result must be source row 4, row 1 source row 5, and so on.
        assertArrayEquals(
            IntArray(16) { i -> 0xFF000000.toInt() or (i + 4) },
            rowColours(png),
        )
    }

    @Test
    fun cropTopRejectsHeightMismatchSoASecondCropCannotSilentlyEatContent() {
        val png = temp.newFile("already-cropped.png")
        rowCoded(png, width = 8, height = 16)
        val before = png.readBytes()

        try {
            TopBandCropper.cropTop(image = png, pixels = 4, expectedHeight = 20)
            fail("expected height mismatch to be rejected")
        } catch (_: IllegalArgumentException) {
            // expected
        }
        assertArrayEquals(before, png.readBytes())
    }

    @Test
    fun cropTopRejectsPixelsOutsideOpenRange() {
        val png = temp.newFile("range.png")
        rowCoded(png, width = 4, height = 10)
        val before = png.readBytes()

        listOf(0, -1, 10, 11).forEach { pixels ->
            try {
                TopBandCropper.cropTop(image = png, pixels = pixels, expectedHeight = 10)
                fail("expected pixels=$pixels to be rejected")
            } catch (_: IllegalArgumentException) {
                // expected
            }
        }
        assertArrayEquals(before, png.readBytes())
    }

    @Test
    fun cropTopRejectsNonPng() {
        val notPng = temp.newFile("capture.txt")
        notPng.writeText("nope")

        try {
            TopBandCropper.cropTop(image = notPng, pixels = 1, expectedHeight = 10)
            fail("expected non-png rejection")
        } catch (_: IllegalArgumentException) {
            // expected
        }
        assertEquals("nope", notPng.readText())
    }

    @Test
    fun cropTopRejectsMissingFile() {
        val missing = File(temp.root, "absent.png")

        try {
            TopBandCropper.cropTop(image = missing, pixels = 1, expectedHeight = 10)
            fail("expected missing file rejection")
        } catch (_: IllegalArgumentException) {
            // expected
        }
        assertFalse(missing.exists())
    }

    @Test
    fun cropTopLeavesNoTempFileBehind() {
        val png = temp.newFile("atomic.png")
        rowCoded(png, width = 4, height = 12)

        TopBandCropper.cropTop(image = png, pixels = 3, expectedHeight = 12)

        assertTrue(png.parentFile.listFiles()!!.none { it.name.startsWith("${png.name}.tmp-") })
        assertEquals(9, ImageIO.read(png).height)
    }
}
