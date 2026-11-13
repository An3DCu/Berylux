plugins {
    alias(libs.plugins.berylux.android.library)
    alias(libs.plugins.berylux.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        // The API on this computer: from the emulator, 10.0.2.2 is the computer it runs on.
        debug { buildConfigField("String", "API_URL", "\"http://10.0.2.2:8080/\"") }
        // No server until the API is hosted. The account card stays hidden in release builds.
        release { buildConfigField("String", "API_URL", "\"\"") }
    }
}

dependencies {
    api(projects.core.model)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.truth)
}
