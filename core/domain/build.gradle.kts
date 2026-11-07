plugins {
    alias(libs.plugins.berylux.jvm.library)
}

dependencies {
    api(projects.core.model)
    api(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(projects.core.testing)
}
