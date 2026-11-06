plugins {
    alias(libs.plugins.berylux.android.library)
    alias(libs.plugins.berylux.android.compose)
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
}
