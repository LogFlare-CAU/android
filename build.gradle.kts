plugins {
    // Declare plugin aliases here with apply false to centralize plugin versions and
    // avoid the Kotlin plugin being loaded multiple times across subprojects.
    // The actual subprojects will apply these plugins without repeating versions.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
}

// Optionally configure common repos or buildscript settings here if needed.
