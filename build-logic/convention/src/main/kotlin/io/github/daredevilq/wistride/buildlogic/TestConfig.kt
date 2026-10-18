package io.github.daredevilq.wistride.buildlogic

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasHostTests
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

/**
 * Robolectric + AndroidX test core for host tests, plus the shared `config/robolectric` resources dir
 * (SDK pin). Applied to every Android module so any of them can use shadows.
 */
internal fun Project.addRobolectricSupport() {
    val robolectricConfig = rootProject.file("config/robolectric").absolutePath
    extensions.getByType(AndroidComponentsExtension::class.java).onVariants { variant ->
        (variant as? HasHostTests)?.hostTests?.values?.forEach { test ->
            test.sources.resources?.addStaticSourceDirectory(robolectricConfig)
        }
    }
    dependencies {
        add("testImplementation", libs.lib("robolectric"))
        add("testImplementation", libs.lib("androidx-test-core"))
    }
}
