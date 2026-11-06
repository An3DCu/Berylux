import com.anadcurukovic.berylux.buildlogic.lib
import com.anadcurukovic.berylux.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** A feature module: screens, their ViewModels and their navigation entries. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("berylux.android.library")
        pluginManager.apply("berylux.android.compose")
        pluginManager.apply("berylux.hilt")
        dependencies {
            add("implementation", project(":core:model"))
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:data"))
            add("implementation", project(":core:navigation"))
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:ui"))

            add("implementation", libs.lib("androidx-compose-material3"))
            add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", libs.lib("androidx-navigation3-runtime"))

            add("testImplementation", project(":core:testing"))
        }
    }
}
