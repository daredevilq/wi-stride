plugins {
    alias(libs.plugins.wistride.android.library)
    alias(libs.plugins.wistride.android.compose)
}

dependencies {
    implementation(projects.core.designsystem)
    // PermissionStatus is part of this module's public API (state + card parameters).
    api(projects.core.model)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.androidx.test.ext.junit)
}
