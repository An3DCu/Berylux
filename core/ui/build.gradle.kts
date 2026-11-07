plugins {
    alias(libs.plugins.berylux.android.library)
    alias(libs.plugins.berylux.android.compose)
}

dependencies {
    api(projects.core.designsystem)
    implementation(projects.core.model)
    implementation(projects.core.domain)
}
