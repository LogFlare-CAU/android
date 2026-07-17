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
            "Assert-Pixel7Api35Profile",
            "Set-AnimationScales",
            "Restore-AnimationScales",
            "Set-UiMode",
            "Wait-HttpHealth",
        ).forEach { fn ->
            assertTrue("common missing $fn", commonText.contains("function $fn"))
        }

        val mockText = mock.readText()
        assertTrue(mockText.contains("param("))
        assertTrue(mockText.contains("Mode"))
        assertTrue(mockText.contains("Verify") && mockText.contains("Record"))
        assertTrue(mockText.contains("Serial"))
        assertTrue(mockText.contains("visual-qa-common.ps1"))
        assertTrue(mockText.contains("assembleDebug"))
        assertTrue(mockText.contains("installDist"))
        assertTrue(mockText.contains("10.0.2.2:8000"))
        assertTrue(mockText.contains("device-baselines"))
        assertTrue(mockText.contains("device-diffs"))
        assertTrue(mockText.contains("record-device"))
        assertFalse("scripts must never echo password literals in docs", mockText.contains("Write-Host \$QA_PASSWORD"))
        assertTrue(File(repoRoot, "visual-qa/device-baselines/.gitkeep").isFile)
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
