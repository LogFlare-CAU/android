package com.logflare.qa

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MaestroMockContractTest {
    private val repoRoot: File = locateRepoRoot()

    @Test
    fun maestroConfigAndFlowsExistWithRequiredCheckpoints() {
        val config = File(repoRoot, ".maestro/config.yaml")
        assertTrue("missing ${config.path}", config.isFile)
        assertTrue(config.readText().contains("appId: com.logflare.android"))

        val login = File(repoRoot, ".maestro/flows/_login.yaml")
        assertTrue(login.isFile)
        val loginText = login.readText()
        assertTrue(loginText.contains("clearState: true"))
        assertTrue(loginText.contains("\${QA_BASE_URL}"))
        assertTrue(loginText.contains("\${QA_USERNAME}"))
        assertTrue(loginText.contains("\${QA_PASSWORD}"))
        assertTrue(loginText.contains("""id: "home_screen""""))

        val expected = mapOf(
            "login-home.yaml" to listOf("home"),
            "logs-detail.yaml" to listOf("logs_detail"),
            "projects.yaml" to listOf("projects_list", "project_detail", "project_settings"),
            "mypage-members.yaml" to listOf("my_page", "add_member", "edit_member"),
            "logout.yaml" to listOf("logout_confirmation"),
        )
        expected.forEach { (name, checkpoints) ->
            val flow = File(repoRoot, ".maestro/flows/$name")
            assertTrue("missing ${flow.path}", flow.isFile)
            val text = flow.readText()
            assertTrue("$name must runFlow _login.yaml", text.contains("_login.yaml"))
            assertTrue("$name must crop on app_root", text.contains("cropOn") && text.contains("app_root"))
            checkpoints.forEach { checkpoint ->
                assertTrue(
                    "$name missing checkpoint $checkpoint",
                    text.contains("\${QA_OUTPUT_DIR}/$checkpoint\${QA_THEME}") ||
                        text.contains("\${QA_OUTPUT_DIR}/${checkpoint}_\${QA_THEME}") ||
                        text.contains("\${QA_OUTPUT_DIR}/$checkpoint"),
                )
            }
        }
    }

    @Test
    fun powerShellHelpersDeclareRequiredContracts() {
        val common = File(repoRoot, "scripts/visual-qa-common.ps1")
        val mock = File(repoRoot, "scripts/visual-qa-maestro-mock.ps1")
        assertTrue(common.isFile)
        assertTrue(mock.isFile)

        val commonText = common.readText()
        listOf(
            "Assert-Command",
            "Assert-Java17",
            "Resolve-EmulatorSerial",
            "Assert-DeviceProfile",
            "Capture-AnimationScales",
            "Disable-AnimationScales",
            "Restore-AnimationScales",
            "Get-ScreenOffTimeout",
            "Set-ScreenOffTimeout",
            "Resolve-UiModeNightState",
            "Set-UiMode",
            "Wait-HttpHealth",
        ).forEach { fn ->
            assertTrue("common missing $fn", commonText.contains("function $fn"))
        }

        assertFalse(
            "Disable-AnimationScales must not reassign Previous originals",
            Regex(
                """function Disable-AnimationScales[\s\S]*?Previous\[['\"]window_animation_scale['\"]\]\s*=""",
            ).containsMatchIn(commonText),
        )
        assertTrue(
            "Capture-AnimationScales should read animation scales into Previous",
            commonText.contains("function Capture-AnimationScales") &&
                commonText.contains("Previous['window_animation_scale']"),
        )
        assertTrue(
            "Resolve-UiModeNightState must exact-match Night mode: yes/no",
            commonText.contains("Night mode:\\s*yes") && commonText.contains("Night mode:\\s*no"),
        )
        assertFalse(
            "Set-UiMode must not use loose night|yes|true matching that accepts Night mode: no as dark",
            commonText.contains("-match 'yes|true|night'") || commonText.contains("-match \"yes|true|night\""),
        )

        val mockText = mock.readText()
        assertTrue(mockText.contains("param("))
        assertTrue(mockText.contains("Mode"))
        assertTrue(mockText.contains("Verify") && mockText.contains("Record"))
        assertTrue(mockText.contains("Serial"))
        assertTrue(mockText.contains("visual-qa-common.ps1"))
        assertTrue(mockText.contains("assembleDebug"))
        assertTrue(mockText.contains("installDist"))
        // Physical devices have no 10.0.2.2 host alias; they reach the mock server via adb reverse.
        // Port is declared once and derived everywhere, so a collision is fixed in a single place.
        assertTrue("mock must declare a single server port", mockText.contains("\$QaServerPort ="))
        assertTrue("mock must derive the base URL from that port", mockText.contains("http://localhost:\$QaServerPort"))
        assertFalse("emulator-only host alias must not come back", mockText.contains("10.0.2.2"))
        assertTrue("mock must establish adb reverse", mockText.contains("reverse") && mockText.contains("tcp:"))
        assertTrue("mock must tear down adb reverse", mockText.contains("reverse --remove"))
        assertTrue("mock must restore the original screen off timeout", mockText.contains("screenTimeoutPrevious"))

        // Maestro ignores ambient env vars (unset ones become the literal "undefined") and always
        // nests screenshots under its own takeScreenshot tree, so both must be handled explicitly.
        assertTrue("flow variables must be passed as --env pairs", mockText.contains("-e \"QA_BASE_URL="))
        assertTrue("mock must pin Maestro's artifact directory", mockText.contains("--test-output-dir"))
        assertTrue("mock must collect from Maestro's takeScreenshot tree", mockText.contains("takeScreenshot"))

        // Captures crop on the edge-to-edge app_root and therefore include the live status bar.
        // One UI ignores SystemUI demo mode, so the band is discarded; without this the device tier
        // can never pass Verify, because clock and battery differ between record and verify.
        assertTrue("mock must crop the status bar band off captures", mockText.contains("crop-top"))
        assertTrue("crop band must be pinned, not guessed", mockText.contains("\$StatusBarCropPx ="))
        assertTrue("crop must assert the uncropped source height", mockText.contains("--expect-height"))
        assertTrue(mockText.contains("device-baselines"))
        assertTrue(mockText.contains("device-diffs"))
        assertTrue(mockText.contains("record-device"))
        assertTrue(
            mockText.contains("device-baselines-staging") ||
                mockText.contains("Promote-BaselineSet") ||
                mockText.contains("BaselineSetPromoter"),
        )
        assertTrue(mockText.contains("Push-Location") && mockText.contains("RepoRoot"))
        assertTrue(
            mockText.contains("visual-qa/device-captures") ||
                mockText.contains("ConvertTo-MaestroRelativePath") ||
                mockText.contains("-replace") ||
                mockText.contains(".Replace('\\','/')"),
        )
        assertFalse("scripts must never echo password literals in docs", mockText.contains("Write-Host \$QA_PASSWORD"))
        assertFalse("notification probe must not force-deny via -or \$true", mockText.contains("-or \$true"))
        assertTrue(File(repoRoot, "visual-qa/device-baselines/.gitkeep").isFile)

        assertTrue(mockText.contains("Capture-AnimationScales"))
        assertTrue(mockText.contains("Disable-AnimationScales"))
        val flowLoop = mockText.substringAfter("foreach (\$flow in \$Flows)", "")
        assertFalse(
            "flow loop must not re-capture animation originals",
            flowLoop.contains("Capture-AnimationScales"),
        )
        assertTrue(
            "flow loop may force-zero animations",
            flowLoop.contains("Disable-AnimationScales"),
        )
    }

    private fun locateRepoRoot(): File {
        var dir = File(System.getProperty("user.dir")).canonicalFile
        repeat(8) {
            if (File(dir, "settings.gradle.kts").isFile && File(dir, "gradlew.bat").isFile) {
                return dir
            }
            dir = dir.parentFile ?: return File(System.getProperty("user.dir"))
        }
        return File(System.getProperty("user.dir"))
    }
}
