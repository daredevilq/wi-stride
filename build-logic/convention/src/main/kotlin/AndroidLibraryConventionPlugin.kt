import com.android.build.api.dsl.LibraryExtension
import io.github.daredevilq.wistride.buildlogic.AndroidConfig
import io.github.daredevilq.wistride.buildlogic.addUnitTestDependencies
import io.github.daredevilq.wistride.buildlogic.configureDetekt
import io.github.daredevilq.wistride.buildlogic.configureKotlin
import io.github.daredevilq.wistride.buildlogic.derivedNamespace
import io.github.daredevilq.wistride.buildlogic.libs
import io.github.daredevilq.wistride.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply(libs.pluginId("android-library"))
            extensions.configure<LibraryExtension> {
                namespace = derivedNamespace
                compileSdk = AndroidConfig.COMPILE_SDK
                defaultConfig {
                    minSdk = AndroidConfig.MIN_SDK
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                compileOptions {
                    sourceCompatibility = AndroidConfig.JAVA_VERSION
                    targetCompatibility = AndroidConfig.JAVA_VERSION
                }
                testOptions.unitTests.isIncludeAndroidResources = true
                lint {
                    abortOnError = true
                    targetSdk = AndroidConfig.TARGET_SDK
                    lintConfig = rootProject.file("config/lint.xml")
                }
            }
            configureKotlin()
            configureDetekt()
            addUnitTestDependencies()
        }
}
