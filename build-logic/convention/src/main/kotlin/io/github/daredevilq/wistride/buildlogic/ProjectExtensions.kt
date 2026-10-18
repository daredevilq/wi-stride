package io.github.daredevilq.wistride.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String): Provider<MinimalExternalModuleDependency> = findLibrary(alias).get()

internal fun VersionCatalog.pluginId(alias: String): String = findPlugin(alias).get().get().pluginId

/** Namespace derived from the Gradle path, e.g. `:core:designsystem` becomes `<appId>.core.designsystem`. */
internal val Project.derivedNamespace: String
    get() = AndroidConfig.APPLICATION_ID + path.replace(':', '.').replace("-", "")
