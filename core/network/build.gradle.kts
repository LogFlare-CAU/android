plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.example.logflare.core.network"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    targetSdk = 36
        buildConfigField(
            "String",
            "BASE_URL",
            "\"" + (providers.gradleProperty("BASE_URL").orElse("http://10.0.2.2:8000/").get()) + "\""
        )
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(project(":core:model"))

    api(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)

    // Hilt annotations are consumed in the app, but modules can declare Dagger modules
    // Without bringing the whole Hilt plugin here.
    kapt(libs.hilt.compiler)
}