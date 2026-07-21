package com.logflare.qa.image

import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.imageio.ImageIO

/**
 * Removes a fixed band from the top of a device capture, in place.
 *
 * Device journeys crop on `app_root`, which is edge-to-edge and therefore extends underneath the
 * system status bar. The captured band contains the live clock, battery level and notification
 * icons, so two captures of an unchanged screen never match. SystemUI demo mode, the usual way to
 * freeze that chrome, is not honoured by Samsung One UI, so the band is discarded instead.
 *
 * [expectedHeight] is the reference device's full screen height. Requiring it to match makes a
 * second crop of the same file fail loudly rather than silently eating real app content.
 */
object TopBandCropper {
    fun cropTop(image: File, pixels: Int, expectedHeight: Int) {
        require(image.isFile) { "image missing: ${image.path}" }
        require(hasPngSignature(image)) { "image is not a PNG: ${image.path}" }

        val source = ImageIO.read(image)
            ?: throw IllegalArgumentException("image is not a valid PNG: ${image.path}")
        require(source.height == expectedHeight) {
            "expected a ${expectedHeight}px tall capture but ${image.path} is ${source.height}px; " +
                "already cropped, or recorded on a different device"
        }
        require(pixels in 1 until expectedHeight) {
            "crop band must be within 1..${expectedHeight - 1}, got $pixels"
        }

        val cropped = BufferedImage(source.width, source.height - pixels, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until cropped.height) {
            for (x in 0 until cropped.width) {
                cropped.setRGB(x, y, source.getRGB(x, y + pixels))
            }
        }
        writePngAtomically(cropped, image)
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

    private fun hasPngSignature(file: File): Boolean {
        if (file.length() < PNG_SIGNATURE.size) return false
        val signature = ByteArray(PNG_SIGNATURE.size)
        file.inputStream().use { input ->
            if (input.read(signature) != signature.size) return false
        }
        return signature.contentEquals(PNG_SIGNATURE)
    }

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
}
