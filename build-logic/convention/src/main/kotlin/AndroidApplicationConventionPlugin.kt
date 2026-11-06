import com.android.build.api.dsl.ApplicationExtension
import com.anadcurukovic.berylux.buildlogic.BeryluxBuild
import com.anadcurukovic.berylux.buildlogic.configureDetekt
import com.anadcurukovic.berylux.buildlogic.configureKotlin
import com.anadcurukovic.berylux.buildlogic.configureUnitTests
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        extensions.configure<ApplicationExtension> {
            compileSdk = BeryluxBuild.COMPILE_SDK
            defaultConfig {
                minSdk = BeryluxBuild.MIN_SDK
                targetSdk = BeryluxBuild.TARGET_SDK
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
