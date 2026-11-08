package com.anadcurukovic.berylux.buildlogic

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
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
        // Robolectric reaches into JDK internals for file descriptors, which JDK 17+ lock by default.
        jvmArgs(
            "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED",
        )
        // A failing test prints its message and stack trace, so CI's log says why, not just where.
        testLogging {
            events("failed")
            exceptionFormat = TestExceptionFormat.FULL
        }
        // Each test's time in the log, to tell a slow machine from a step that hangs.
        addTestListener(TestTimes)
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

/** Prints how long each test took, as `TIME  1234 ms  Class > test`. */
private object TestTimes : TestListener {
    private val logger = Logging.getLogger("TestTimes")

    override fun afterTest(test: TestDescriptor, result: TestResult) {
        val millis = result.endTime - result.startTime
        logger.lifecycle("TIME %6d ms  %s > %s".format(millis, test.className?.substringAfterLast('.'), test.name))
    }

    override fun beforeSuite(suite: TestDescriptor) = Unit

    override fun afterSuite(suite: TestDescriptor, result: TestResult) = Unit

    override fun beforeTest(test: TestDescriptor) = Unit
}
