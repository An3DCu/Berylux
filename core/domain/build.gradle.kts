plugins {
    alias(libs.plugins.berylux.jvm.library)
}

dependencies {
    api(projects.core.model)
    implementation(libs.kotlinx.coroutines.core)
}
