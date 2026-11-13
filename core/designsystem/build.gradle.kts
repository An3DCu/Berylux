plugins {
    alias(libs.plugins.berylux.android.library)
    alias(libs.plugins.berylux.android.compose)
    alias(libs.plugins.roborazzi)
}

// The reference screenshots are committed next to the tests; CI compares against them.
roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    // Bitmaps and fonts for the widgets' pictures.
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // Compose testing brings Espresso 3.5, which crashes on Android 16 and newer.
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
