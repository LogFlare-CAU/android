import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.roborazzi)
}

// Release signing comes from local.properties (never committed):
//   RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD
// Without any of them the release build is signed with the debug key so it still installs locally
// (with a warning, only when a release package/assemble/bundle task runs). A partial or dangling
// setup (some keys set, keystore file missing) fails such a build instead of silently debug-signing.
val releaseProps = Properties()
rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { releaseProps.load(it) }
val releaseKeys = listOf("RELEASE_STORE_FILE", "RELEASE_STORE_PASSWORD", "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD")
val releaseKeystore = releaseProps.getProperty("RELEASE_STORE_FILE")?.let { rootProject.file(it) }
val hasReleaseKeystore = releaseKeystore != null && releaseKeystore.isFile &&
    releaseKeys.all { releaseProps.getProperty(it) != null }
val releaseKeysSet = releaseKeys.filter { releaseProps.getProperty(it) != null }

// Release signing is judged when the task graph is known, so debug builds, lint and tests stay quiet.
gradle.taskGraph.whenReady {
    val signsRelease = allTasks.any {
        it.project == project && it.name.matches(Regex("^(assemble|bundle|package|sign|validateSigning)[A-Za-z]*Release.*"))
    }
    if (signsRelease && !hasReleaseKeystore) {
        if (releaseKeysSet.isNotEmpty()) {
            val problem = releaseKeys.filter { it !in releaseKeysSet }.takeIf { it.isNotEmpty() }
                ?.let { "missing ${it.joinToString()}" }
                ?: "the keystore file RELEASE_STORE_FILE=${releaseProps.getProperty("RELEASE_STORE_FILE")} does not exist"
            throw GradleException(
                "Release signing is only partly configured in local.properties ($problem). " +
                    "Fix it, or remove all RELEASE_* keys to build a debug-signed release.",
            )
        }
        logger.warn(
            "WARNING: no release keystore in local.properties (RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, " +
                "RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD): the release build is signed with the DEBUG key " +
                "and must not be distributed.",
        )
    }
}

kapt {
    correctErrorTypes = true
}

android {
    namespace = "com.logflare.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.logflare.android"
        minSdk = 26
    targetSdk = 36
        versionCode = 2
        versionName = "2.0.0"

        vectorDrawables.useSupportLibrary = true
    }

    if (hasReleaseKeystore) {
        signingConfigs.create("release") {
            storeFile = releaseKeystore
            storePassword = releaseProps.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = releaseProps.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = releaseProps.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // enable as needed
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildToolsVersion = "36.1.0"

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

roborazzi {
    outputDir.set(file("src/test/snapshots/images"))
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.robolectric)
    testImplementation(libs.okhttp)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.navigation.testing)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
