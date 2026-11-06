plugins {
    alias(libs.plugins.berylux.android.library)
    alias(libs.plugins.berylux.hilt)
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.domain)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(libs.kotlinx.coroutines.android)
}
