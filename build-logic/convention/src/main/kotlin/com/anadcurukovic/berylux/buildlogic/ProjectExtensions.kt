package com.anadcurukovic.berylux.buildlogic

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/** SDK levels and the Java version every module builds against. */
object BeryluxBuild {
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37
    const val MIN_SDK = 26
    const val BASE_PACKAGE = "com.anadcurukovic.berylux"
    val JAVA_VERSION = JavaVersion.VERSION_17
    val JVM_TARGET = JvmTarget.JVM_17
}

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.lib(alias: String) = findLibrary(alias).get()

fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

/** `:core:database` becomes `com.anadcurukovic.berylux.core.database`. */
fun Project.moduleNamespace(): String =
    BeryluxBuild.BASE_PACKAGE + "." + path.removePrefix(":").replace(':', '.')

fun Project.configureKotlin() {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(BeryluxBuild.JVM_TARGET)
        }
    }
}

fun Project.configureUnitTests() {
    // Hilt and KSP generate test sources even in modules that have no tests yet.
    tasks.withType<Test>().configureEach {
        failOnNoDiscoveredTests.set(false)
    }
    dependencies {
        add("testImplementation", libs.lib("junit4"))
        add("testImplementation", libs.lib("truth"))
        add("testImplementation", libs.lib("kotlinx-coroutines-test"))
        add("testImplementation", libs.lib("turbine"))
    }
}

fun Project.configureDetekt() {
    pluginManager.apply("io.gitlab.arturbosch.detekt")
    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        parallel = true
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    }
    // Without this, detekt takes the JDK it runs on as the target, which can be newer than it knows.
    tasks.withType<Detekt>().configureEach {
        jvmTarget = BeryluxBuild.JVM_TARGET.target
    }
    tasks.withType<DetektCreateBaselineTask>().configureEach {
        jvmTarget = BeryluxBuild.JVM_TARGET.target
    }
    dependencies {
        add("detektPlugins", libs.lib("detekt-formatting"))
    }
    // detekt 1.23 runs on the Kotlin version it was built with, not the one the app uses.
    val detektKotlin = libs.version("detektKotlin")
    configurations.matching { it.name == "detekt" }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion(detektKotlin)
            }
        }
    }
}
