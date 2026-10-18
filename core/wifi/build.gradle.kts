plugins {
    alias(libs.plugins.wistride.android.library)
    alias(libs.plugins.wistride.hilt)
}

dependencies {
    // WifiSnapshot and friends are this module's public API.
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.domain.wifi)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.core.testing)
}
