plugins {
    alias(libs.plugins.wistride.android.library)
    alias(libs.plugins.wistride.hilt)
}

dependencies {
    api(projects.core.common)
    api(libs.kotlinx.coroutines.core)
}
