import com.anadcurukovic.berylux.buildlogic.BeryluxBuild
import com.anadcurukovic.berylux.buildlogic.configureDetekt
import com.anadcurukovic.berylux.buildlogic.configureKotlin
import com.anadcurukovic.berylux.buildlogic.configureUnitTests
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure

/** Plain Kotlin, no Android: the models and the rules. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = BeryluxBuild.JAVA_VERSION
            targetCompatibility = BeryluxBuild.JAVA_VERSION
        }
        configureKotlin()
        configureUnitTests()
        configureDetekt()
    }
}
