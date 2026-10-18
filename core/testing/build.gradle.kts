plugins {
    alias(libs.plugins.wistride.jvm.library)
}

dependencies {
    api(projects.core.common)
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
