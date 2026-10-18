plugins {
    alias(libs.plugins.wistride.android.library)
    alias(libs.plugins.wistride.android.compose)
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
}
