plugins {
    alias(libs.plugins.berylux.jvm.library)
}

dependencies {
    api(projects.core.model)
    api(projects.core.domain)
    api(libs.junit4)
    api(libs.truth)
    api(libs.turbine)
    api(libs.kotlinx.coroutines.test)
}
