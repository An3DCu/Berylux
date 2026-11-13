plugins {
    alias(libs.plugins.berylux.jvm.library)
    // Coverage for the rules; the CI log prints it.
    alias(libs.plugins.kover)
}

dependencies {
    api(projects.core.model)
    api(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(projects.core.testing)
}
