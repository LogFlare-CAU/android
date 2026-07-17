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

class BaselineSetPromoterTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun promoteReplacesBaselinesAtomicallyAndPreservesGitkeep() {
        val baselines = tmp.newFolder("device-baselines")
        File(baselines, ".gitkeep").writeText("")
        File(baselines, "home_light.png").writeBytes(pngBytes(color = 0xFF0000))
        File(baselines, "stale_light.png").writeBytes(pngBytes(color = 0x00FF00))

        val staging = tmp.newFolder("device-baselines-staging")
        File(staging, "home_light.png").writeBytes(pngBytes(color = 0x0000FF))
        File(staging, "logs_detail_light.png").writeBytes(pngBytes(color = 0xABCDEF))

        BaselineSetPromoter.promote(
            stagingDir = staging,
            baselinesDir = baselines,
            expectedNames = listOf("home_light.png", "logs_detail_light.png"),
        )

        assertTrue(File(baselines, ".gitkeep").isFile)
        assertEquals(
            File(staging, "home_light.png").readBytes().toList(),
            File(baselines, "home_light.png").readBytes().toList(),
        )
        assertTrue(File(baselines, "logs_detail_light.png").isFile)
        assertFalse(File(baselines, "stale_light.png").exists())
    }

    @Test
    fun promoteFailureRestoresPreviousBaselines() {
        val baselines = tmp.newFolder("device-baselines")
        File(baselines, ".gitkeep").writeText("")
        val original = pngBytes(color = 0x112233)
        File(baselines, "home_light.png").writeBytes(original)

        val staging = tmp.newFolder("device-baselines-staging")
        // Missing required checkpoint → promote must fail and restore.
        File(staging, "other_light.png").writeBytes(pngBytes(color = 0x445566))

        try {
            BaselineSetPromoter.promote(
                stagingDir = staging,
                baselinesDir = baselines,
                expectedNames = listOf("home_light.png", "logs_detail_light.png"),
            )
            fail("expected promote to fail")
        } catch (_: IllegalStateException) {
            // expected
        }

        assertEquals(original.toList(), File(baselines, "home_light.png").readBytes().toList())
        assertTrue(File(baselines, ".gitkeep").isFile)
        assertFalse(File(baselines, "other_light.png").exists())
    }

    private fun pngBytes(color: Int): ByteArray {
        val image = BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, 0xFF000000.toInt() or (color and 0xFFFFFF))
        image.setRGB(1, 0, 0xFF000000.toInt() or (color and 0xFFFFFF))
        image.setRGB(0, 1, 0xFF000000.toInt() or (color and 0xFFFFFF))
        image.setRGB(1, 1, 0xFF000000.toInt() or (color and 0xFFFFFF))
        val file = tmp.newFile("sample-${color.toString(16)}.png")
        ImageIO.write(image, "png", file)
        return file.readBytes()
    }
}
