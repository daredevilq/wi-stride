plugins {
    alias(libs.plugins.wistride.android.library)
    alias(libs.plugins.wistride.hilt)
}

dependencies {
    // Ping targets and latency events are this module's public API.
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)
    implementation(projects.core.common)
    implementation(projects.domain.network)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.core.testing)
}
