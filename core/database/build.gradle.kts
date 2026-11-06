plugins {
    alias(libs.plugins.berylux.android.library)
    alias(libs.plugins.berylux.room)
    alias(libs.plugins.berylux.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    sourceSets {
        // The exported schemas, so tests can open every past version of the database.
        getByName("test").assets.directories.add("$projectDir/schemas")
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.runner)
}
