plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("com.logflare.qa.QaToolMainKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(project(path = ":core:model", configuration = "jvmTestElements"))
}

tasks.test {
    useJUnit()

    // MaestroMockContractTest asserts against files that live outside this module's source set.
    // Gradle cannot infer them, so without these declarations the task goes UP-TO-DATE after those
    // files change and reports a stale pass.
    inputs.files(
        rootProject.layout.projectDirectory.file("scripts/visual-qa-common.ps1"),
        rootProject.layout.projectDirectory.file("scripts/visual-qa-maestro-mock.ps1"),
        rootProject.layout.projectDirectory.file("visual-qa/device-baselines/.gitkeep"),
    )
        .withPropertyName("visualQaContractFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(rootProject.layout.projectDirectory.dir(".maestro"))
        .withPropertyName("maestroFlows")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
