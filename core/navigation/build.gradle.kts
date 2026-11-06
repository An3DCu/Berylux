plugins {
    alias(libs.plugins.berylux.android.library)
    alias(libs.plugins.berylux.android.compose)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)
}
