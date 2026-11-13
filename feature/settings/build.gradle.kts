plugins {
    alias(libs.plugins.berylux.android.feature)
}

dependencies {
    // The notification permission prompt, and Android's notification switch for the app.
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
