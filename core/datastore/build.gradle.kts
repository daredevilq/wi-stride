plugins {
    alias(libs.plugins.wistride.android.library)
    alias(libs.plugins.wistride.hilt)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // PingSettings and PermissionRequestHistory are this module's public API.
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)
    implementation(projects.core.common)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
    testImplementation(libs.truth)
}
