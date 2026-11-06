import com.android.build.api.dsl.LibraryExtension
import com.anadcurukovic.berylux.buildlogic.BeryluxBuild
import com.anadcurukovic.berylux.buildlogic.configureDetekt
import com.anadcurukovic.berylux.buildlogic.configureKotlin
import com.anadcurukovic.berylux.buildlogic.configureUnitTests
import com.anadcurukovic.berylux.buildlogic.moduleNamespace
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        extensions.configure<LibraryExtension> {
            namespace = moduleNamespace()
            compileSdk = BeryluxBuild.COMPILE_SDK
            defaultConfig {
                minSdk = BeryluxBuild.MIN_SDK
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            compileOptions {
                sourceCompatibility = BeryluxBuild.JAVA_VERSION
                targetCompatibility = BeryluxBuild.JAVA_VERSION
            }
            testOptions.unitTests.isIncludeAndroidResources = true
        }
        configureKotlin()
        configureUnitTests()
        configureDetekt()
    }
}
