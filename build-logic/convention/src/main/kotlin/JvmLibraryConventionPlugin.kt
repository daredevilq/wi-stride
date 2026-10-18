import io.github.daredevilq.wistride.buildlogic.AndroidConfig
import io.github.daredevilq.wistride.buildlogic.addUnitTestDependencies
import io.github.daredevilq.wistride.buildlogic.configureDetekt
import io.github.daredevilq.wistride.buildlogic.configureKotlin
import io.github.daredevilq.wistride.buildlogic.libs
import io.github.daredevilq.wistride.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure

/** Pure Kotlin/JVM module (domain, models): no Android dependencies allowed. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply(libs.pluginId("kotlin-jvm"))
            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = AndroidConfig.JAVA_VERSION
                targetCompatibility = AndroidConfig.JAVA_VERSION
            }
            configureKotlin()
            configureDetekt()
            addUnitTestDependencies()
        }
}
