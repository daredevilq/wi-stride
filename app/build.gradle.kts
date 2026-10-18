plugins {
    alias(libs.plugins.wistride.android.application)
    alias(libs.plugins.wistride.android.compose)
    alias(libs.plugins.wistride.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    defaultConfig {
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.designsystem)
    implementation(projects.feature.map)
    implementation(projects.feature.networks)
    implementation(projects.feature.now)
    implementation(projects.feature.ping)
    implementation(projects.feature.speed)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material3.navigation.suite)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.androidx.test.ext.junit)
}
