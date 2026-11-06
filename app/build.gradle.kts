plugins {
    alias(libs.plugins.berylux.android.application)
    alias(libs.plugins.berylux.android.compose)
    alias(libs.plugins.berylux.hilt)
}

android {
    namespace = "com.anadcurukovic.berylux"

    defaultConfig {
        applicationId = "com.anadcurukovic.berylux"
        versionCode = 1
        versionName = "0.1.0"
    }

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

    implementation(projects.feature.today)
    implementation(projects.feature.tracker)
    implementation(projects.feature.catalog)
    implementation(projects.feature.editor)
    implementation(projects.feature.progress)
    implementation(projects.feature.profile)
    implementation(projects.feature.settings)
    implementation(projects.feature.rivals)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
