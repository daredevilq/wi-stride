plugins {
    alias(libs.plugins.wistride.android.feature)
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.datastore)
    implementation(projects.core.network)
    implementation(projects.domain.network)
    implementation(libs.vico.compose.m3)

    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
}
