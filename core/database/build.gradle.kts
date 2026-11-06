plugins {
    alias(libs.plugins.berylux.android.library)
    alias(libs.plugins.berylux.room)
    alias(libs.plugins.berylux.hilt)
}

dependencies {
    implementation(projects.core.model)
}
