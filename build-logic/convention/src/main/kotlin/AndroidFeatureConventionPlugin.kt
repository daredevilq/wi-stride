import io.github.daredevilq.wistride.buildlogic.lib
import io.github.daredevilq.wistride.buildlogic.libs
import io.github.daredevilq.wistride.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** A screen module: Android library + Compose + Hilt + type-safe navigation routes. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply(libs.pluginId("wistride-android-library"))
            pluginManager.apply(libs.pluginId("wistride-android-compose"))
            pluginManager.apply(libs.pluginId("wistride-hilt"))
            pluginManager.apply(libs.pluginId("kotlin-serialization"))
            dependencies {
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))
                add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.lib("androidx-navigation-compose"))
                add("implementation", libs.lib("androidx-hilt-navigation-compose"))
                add("testImplementation", project(":core:testing"))
            }
        }
}
