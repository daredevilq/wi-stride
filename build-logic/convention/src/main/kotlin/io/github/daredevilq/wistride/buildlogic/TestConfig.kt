package io.github.daredevilq.wistride.buildlogic

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/** JVM unit-test stack shared by every module. */
internal fun Project.addUnitTestDependencies() {
    dependencies {
        add("testImplementation", libs.lib("junit"))
        add("testImplementation", libs.lib("truth"))
        add("testImplementation", libs.lib("kotlinx-coroutines-test"))
        add("testImplementation", libs.lib("turbine"))
    }
    tasks.withType<Test>().configureEach {
        // Hilt/KSP generate test sources even in modules without tests yet; Gradle 9 would then fail
        // the build for "no discovered tests". Missing tests are caught by review, not by this guard.
        failOnNoDiscoveredTests.set(false)
    }
}
