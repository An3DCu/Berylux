plugins {
    alias(libs.plugins.berylux.android.library)
    alias(libs.plugins.berylux.android.compose)
    alias(libs.plugins.berylux.hilt)
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.designsystem)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.glance.appwidget.testing)
    testImplementation(libs.androidx.work.testing)
}
