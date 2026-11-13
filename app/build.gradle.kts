plugins {
    alias(libs.plugins.berylux.android.application)
    alias(libs.plugins.berylux.android.compose)
    alias(libs.plugins.berylux.hilt)
}

android {
    namespace = "com.anadcurukovic.berylux"

    defaultConfig {
        applicationId = "com.anadcurukovic.berylux"
        versionCode = 3
        versionName = "0.3.0"

        // Instrumented tests start a Hilt test app. The orchestrator clears the app's data before
        // each one, so every test begins on a fresh install.
        testInstrumentationRunner = "com.anadcurukovic.berylux.HiltTestRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    testOptions.execution = "ANDROIDX_TEST_ORCHESTRATOR"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    lint {
        // Lint on the app also checks every module it depends on.
        checkDependencies = true
        abortOnError = true
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.navigation)
    implementation(projects.core.data)
    implementation(projects.core.ui)

    implementation(projects.feature.today)
    implementation(projects.feature.tracker)
    implementation(projects.feature.catalog)
    implementation(projects.feature.editor)
    implementation(projects.feature.progress)
    implementation(projects.feature.profile)
    implementation(projects.feature.settings)
    implementation(projects.feature.rivals)
    implementation(projects.widget)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.navigation.suite)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.androidx.work.testing)
    kspTest(libs.hilt.compiler)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // Compose testing brings Espresso 3.5, which crashes on Android 16 and newer.
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestUtil(libs.androidx.test.orchestrator)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
