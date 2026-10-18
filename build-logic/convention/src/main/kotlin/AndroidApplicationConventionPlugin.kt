import com.android.build.api.dsl.ApplicationExtension
import io.github.daredevilq.wistride.buildlogic.AndroidConfig
import io.github.daredevilq.wistride.buildlogic.addUnitTestDependencies
import io.github.daredevilq.wistride.buildlogic.configureDetekt
import io.github.daredevilq.wistride.buildlogic.configureKotlin
import io.github.daredevilq.wistride.buildlogic.libs
import io.github.daredevilq.wistride.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply(libs.pluginId("android-application"))
            extensions.configure<ApplicationExtension> {
                namespace = AndroidConfig.APPLICATION_ID
                compileSdk = AndroidConfig.COMPILE_SDK
                defaultConfig {
                    applicationId = AndroidConfig.APPLICATION_ID
                    minSdk = AndroidConfig.MIN_SDK
                    targetSdk = AndroidConfig.TARGET_SDK
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                compileOptions {
                    sourceCompatibility = AndroidConfig.JAVA_VERSION
                    targetCompatibility = AndroidConfig.JAVA_VERSION
                }
                testOptions.unitTests.isIncludeAndroidResources = true
                lint {
                    abortOnError = true
                    lintConfig = rootProject.file("config/lint.xml")
                }
            }
            configureKotlin()
            configureDetekt()
            addUnitTestDependencies()
        }
}
