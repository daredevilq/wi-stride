import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasHostTests
import io.github.daredevilq.wistride.buildlogic.lib
import io.github.daredevilq.wistride.buildlogic.libs
import io.github.daredevilq.wistride.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Adds Compose to an Android module that already applies an application/library convention. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply(libs.pluginId("kotlin-compose"))
            extensions.getByType(CommonExtension::class.java).buildFeatures.compose = true
            // One shared robolectric.properties (SDK pin) for all Compose modules' host tests.
            val robolectricConfig = rootProject.file("config/robolectric").absolutePath
            extensions.getByType(AndroidComponentsExtension::class.java).onVariants { variant ->
                (variant as? HasHostTests)?.hostTests?.values?.forEach { test ->
                    test.sources.resources?.addStaticSourceDirectory(robolectricConfig)
                }
            }
            dependencies {
                val bom = platform(libs.lib("androidx-compose-bom"))
                add("implementation", bom)
                add("testImplementation", bom)
                add("implementation", libs.lib("androidx-compose-ui"))
                add("implementation", libs.lib("androidx-compose-material3"))
                add("implementation", libs.lib("androidx-compose-ui-tooling-preview"))
                add("debugImplementation", libs.lib("androidx-compose-ui-tooling"))
                add("debugImplementation", libs.lib("androidx-compose-ui-test-manifest"))
                add("testImplementation", libs.lib("androidx-compose-ui-test-junit4"))
                add("testImplementation", libs.lib("robolectric"))
                add("testImplementation", libs.lib("androidx-test-core"))
            }
        }
}
