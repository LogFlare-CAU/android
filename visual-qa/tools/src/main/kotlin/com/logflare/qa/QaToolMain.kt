package com.logflare.qa

import com.logflare.qa.image.BaselineSetPromoter
import com.logflare.qa.image.CompareResult
import com.logflare.qa.image.DeviceImageComparator
import com.logflare.qa.image.TopBandCropper
import com.logflare.qa.server.MockServer
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    exitProcess(QaToolMain().run(args))
}

class QaToolMain {
    fun run(args: Array<String>): Int {
        if (args.isEmpty() || args[0] == "--help" || args[0] == "-h") {
            printHelp()
            return 0
        }
        return when (args[0]) {
            "server" -> runServer(args.drop(1))
            "compare" -> runCompare(args.drop(1))
            "record-device" -> runRecordDevice(args.drop(1))
            "crop-top" -> runCropTop(args.drop(1))
            "promote-baselines" -> runPromoteBaselines(args.drop(1))
            else -> {
                System.err.println("Unknown command: ${args[0]}")
                printHelp()
                2
            }
        }
    }

    private fun runServer(args: List<String>): Int {
        var host = "127.0.0.1"
        var port = 8000
        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "--host" -> {
                    host = args.getOrNull(++i) ?: return usageError("server requires --host <value>")
                }
                "--port" -> {
                    port = args.getOrNull(++i)?.toIntOrNull()
                        ?: return usageError("server requires --port <int>")
                }
                "--help", "-h" -> {
                    printHelp()
                    return 0
                }
                else -> return usageError("unknown server argument: ${args[i]}")
            }
            i++
        }

        val server = MockServer(host = host, port = port)
        val latch = CountDownLatch(1)
        Runtime.getRuntime().addShutdownHook(
            Thread {
                runCatching { server.stop() }
                latch.countDown()
            },
        )
        server.start()
        println("RESULT ServerStarted host=$host port=${server.boundPort}")
        latch.await()
        return 0
    }

    private fun runCompare(args: List<String>): Int {
        var expected: File? = null
        var actual: File? = null
        var diff: File? = null
        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "--expected" -> expected = File(args.getOrNull(++i) ?: return usageError("missing --expected"))
                "--actual" -> actual = File(args.getOrNull(++i) ?: return usageError("missing --actual"))
                "--diff" -> diff = File(args.getOrNull(++i) ?: return usageError("missing --diff"))
                "--help", "-h" -> {
                    printHelp()
                    return 0
                }
                else -> return usageError("unknown compare argument: ${args[i]}")
            }
            i++
        }
        if (expected == null || actual == null || diff == null) {
            return usageError("compare requires --expected <png> --actual <png> --diff <png>")
        }
        if (!expected.isFile) return fail("RESULT InvalidImage reason=missing-expected")
        if (!actual.isFile) return fail("RESULT InvalidImage reason=missing-actual")

        val result = DeviceImageComparator().compare(expected, actual, diff)
        return when (result) {
            CompareResult.Match -> {
                println("RESULT Match")
                0
            }
            is CompareResult.Changed -> {
                println(QaResultPresenter.format(result))
                1
            }
            is CompareResult.DimensionMismatch -> {
                println(
                    "RESULT DimensionMismatch expected=${result.expectedWidth}x${result.expectedHeight} " +
                        "actual=${result.actualWidth}x${result.actualHeight}",
                )
                1
            }
            is CompareResult.InvalidImage -> {
                println("RESULT InvalidImage reason=${result.reason}")
                1
            }
        }
    }

    private fun runRecordDevice(args: List<String>): Int {
        var actual: File? = null
        var expected: File? = null
        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "--actual" -> actual = File(args.getOrNull(++i) ?: return usageError("missing --actual"))
                "--expected" -> expected = File(args.getOrNull(++i) ?: return usageError("missing --expected"))
                "--help", "-h" -> {
                    printHelp()
                    return 0
                }
                else -> return usageError("unknown record-device argument: ${args[i]}")
            }
            i++
        }
        if (actual == null || expected == null) {
            return usageError("record-device requires --actual <png> --expected <png>")
        }
        return try {
            DeviceImageComparator.recordDevice(actual = actual, expected = expected)
            println("RESULT Recorded path=${expected.path}")
            0
        } catch (e: IllegalArgumentException) {
            println("RESULT InvalidImage reason=${e.message}")
            1
        }
    }

    private fun runCropTop(args: List<String>): Int {
        var image: File? = null
        var pixels: Int? = null
        var expectHeight: Int? = null
        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "--image" -> image = File(args.getOrNull(++i) ?: return usageError("missing --image"))
                "--pixels" -> pixels = args.getOrNull(++i)?.toIntOrNull()
                    ?: return usageError("crop-top requires --pixels <int>")
                "--expect-height" -> expectHeight = args.getOrNull(++i)?.toIntOrNull()
                    ?: return usageError("crop-top requires --expect-height <int>")
                "--help", "-h" -> {
                    printHelp()
                    return 0
                }
                else -> return usageError("unknown crop-top argument: ${args[i]}")
            }
            i++
        }
        if (image == null || pixels == null || expectHeight == null) {
            return usageError("crop-top requires --image <png> --pixels <int> --expect-height <int>")
        }
        return try {
            TopBandCropper.cropTop(image = image, pixels = pixels, expectedHeight = expectHeight)
            println("RESULT Cropped path=${image.path} removedTop=$pixels height=${expectHeight - pixels}")
            0
        } catch (e: IllegalArgumentException) {
            println("RESULT CropFailed reason=${e.message}")
            1
        }
    }

    private fun runPromoteBaselines(args: List<String>): Int {
        var staging: File? = null
        var baselines: File? = null
        var expectedCsv: String? = null
        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "--staging" -> staging = File(args.getOrNull(++i) ?: return usageError("missing --staging"))
                "--baselines" -> baselines = File(args.getOrNull(++i) ?: return usageError("missing --baselines"))
                "--expected" -> expectedCsv = args.getOrNull(++i) ?: return usageError("missing --expected")
                "--help", "-h" -> {
                    printHelp()
                    return 0
                }
                else -> return usageError("unknown promote-baselines argument: ${args[i]}")
            }
            i++
        }
        if (staging == null || baselines == null || expectedCsv.isNullOrBlank()) {
            return usageError("promote-baselines requires --staging <dir> --baselines <dir> --expected <a.png,b.png>")
        }
        val expected = expectedCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return try {
            BaselineSetPromoter.promote(
                stagingDir = staging,
                baselinesDir = baselines,
                expectedNames = expected,
            )
            println("RESULT Promoted count=${expected.size} path=${baselines.path}")
            0
        } catch (e: Exception) {
            println("RESULT PromoteFailed reason=${e.message}")
            1
        }
    }

    private fun usageError(message: String): Int {
        System.err.println(message)
        printHelp()
        return 2
    }

    private fun fail(line: String): Int {
        println(line)
        return 1
    }

    private fun printHelp() {
        println(
            """
            LogFlare Visual QA Tools

            Usage:
              tools server --host 127.0.0.1 --port 8000
              tools compare --expected <png> --actual <png> --diff <png>
              tools record-device --actual <png> --expected <png>
              tools crop-top --image <png> --pixels 139 --expect-height 3120
              tools promote-baselines --staging <dir> --baselines <dir> --expected <a.png,b.png>

            Commands:
              server             Start deterministic mock API (blocks until terminated)
              compare            Compare device PNGs; exit 0 on Match, nonzero otherwise
              record-device       Copy validated actual PNG bytes into expected path
              crop-top           Drop the status bar band off a capture, in place
              promote-baselines  Atomically promote a complete staged baseline set

            Exit codes:
              0  success (server healthy start/normal stop, Match, record ok, promote ok, crop ok)
              1  compare Changed / DimensionMismatch / InvalidImage / promote failed / crop failed
              2  invalid arguments
            """.trimIndent(),
        )
    }
}

object QaResultPresenter {
    fun format(result: CompareResult.Changed): String =
        "RESULT Changed ratio=${String.format(Locale.US, "%.8f", result.changedRatio)} " +
            "count=${result.changedPixels}"
}
