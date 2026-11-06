import androidx.room.gradle.RoomExtension
import com.anadcurukovic.berylux.buildlogic.lib
import com.anadcurukovic.berylux.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("androidx.room")
        extensions.configure<RoomExtension> {
            // The exported schemas are committed, so every migration can be tested against the real old schema.
            schemaDirectory("$projectDir/schemas")
        }
        dependencies {
            add("implementation", libs.lib("room-runtime"))
            add("implementation", libs.lib("room-ktx"))
            add("ksp", libs.lib("room-compiler"))
            add("testImplementation", libs.lib("room-testing"))
        }
    }
}
