package com.logflare.qa.image

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object BaselineSetPromoter {
    /**
     * Atomically promotes a complete staged PNG set into [baselinesDir].
     * Backs up existing baselines (except `.gitkeep`), swaps in staging files matching
     * [expectedNames], removes unexpected PNGs, and restores the backup on any failure.
     */
    fun promote(
        stagingDir: File,
        baselinesDir: File,
        expectedNames: List<String>,
    ) {
        require(stagingDir.isDirectory) { "staging dir missing: ${stagingDir.path}" }
        baselinesDir.mkdirs()

        val missing = expectedNames.filter { !File(stagingDir, it).isFile }
        if (missing.isNotEmpty()) {
            throw IllegalStateException("staging missing expected baselines: ${missing.joinToString()}")
        }
        val unexpected = stagingDir.listFiles()
            ?.filter { it.isFile && it.extension.equals("png", ignoreCase = true) }
            ?.map { it.name }
            ?.filter { it !in expectedNames }
            .orEmpty()
        if (unexpected.isNotEmpty()) {
            throw IllegalStateException("staging has unexpected baselines: ${unexpected.joinToString()}")
        }

        val backupDir = Files.createTempDirectory(baselinesDir.toPath().parent, "baseline-backup-").toFile()
        try {
            baselinesDir.listFiles()
                ?.filter { it.isFile && it.name != ".gitkeep" }
                ?.forEach { src ->
                    Files.copy(src.toPath(), File(backupDir, src.name).toPath(), StandardCopyOption.REPLACE_EXISTING)
                }

            // Clear previous PNG baselines (keep .gitkeep).
            baselinesDir.listFiles()
                ?.filter { it.isFile && it.extension.equals("png", ignoreCase = true) }
                ?.forEach { it.delete() }

            expectedNames.forEach { name ->
                val src = File(stagingDir, name)
                val dest = File(baselinesDir, name)
                val temp = Files.createTempFile(baselinesDir.toPath(), "${name}.tmp-", ".png")
                try {
                    Files.copy(src.toPath(), temp, StandardCopyOption.REPLACE_EXISTING)
                    moveReplacing(temp.toFile(), dest)
                } finally {
                    Files.deleteIfExists(temp)
                }
            }

            // Ensure .gitkeep survives.
            val keep = File(baselinesDir, ".gitkeep")
            if (!keep.exists()) {
                keep.writeText("")
            }
        } catch (t: Throwable) {
            restoreBackup(backupDir, baselinesDir)
            throw t
        } finally {
            backupDir.deleteRecursively()
        }
    }

    private fun restoreBackup(backupDir: File, baselinesDir: File) {
        baselinesDir.listFiles()
            ?.filter { it.isFile && it.name != ".gitkeep" }
            ?.forEach { it.delete() }
        backupDir.listFiles()?.filter { it.isFile }?.forEach { src ->
            Files.copy(src.toPath(), File(baselinesDir, src.name).toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        val keep = File(baselinesDir, ".gitkeep")
        if (!keep.exists()) keep.writeText("")
    }

    private fun moveReplacing(source: File, destination: File) {
        try {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
