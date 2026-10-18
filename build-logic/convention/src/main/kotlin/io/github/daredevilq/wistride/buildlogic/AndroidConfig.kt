package io.github.daredevilq.wistride.buildlogic

import org.gradle.api.JavaVersion

internal object AndroidConfig {
    const val APPLICATION_ID = "io.github.daredevilq.wistride"
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 37
    const val MIN_SDK = 29
    val JAVA_VERSION = JavaVersion.VERSION_17
}
