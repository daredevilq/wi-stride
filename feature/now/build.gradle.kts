plugins {
    alias(libs.plugins.wistride.android.feature)
}

dependencies {
    implementation(projects.core.wifi)
    implementation(projects.domain.wifi)
    implementation(libs.vico.compose.m3)

    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
}
