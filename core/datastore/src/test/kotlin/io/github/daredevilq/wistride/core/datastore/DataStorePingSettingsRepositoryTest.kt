package io.github.daredevilq.wistride.core.datastore

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth.assertThat
import io.github.daredevilq.wistride.core.model.ping.PingSettings
import io.github.daredevilq.wistride.core.model.ping.PingTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStorePingSettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scopes = mutableListOf<Job>()

    private val file: File by lazy { File(folder.root, "ping_settings.json") }

    // Real IO dispatcher: DataStore does file I/O, a test dispatcher would need manual pumping.
    private fun newScope(): CoroutineScope =
        CoroutineScope(Dispatchers.IO + Job()).also {
            scopes +=
                it.coroutineContext[Job]!!
        }

    private fun newStore(scope: CoroutineScope = newScope()): DataStore<PingSettingsDto> =
        pingSettingsStore(file, scope)

    private fun repository(store: DataStore<PingSettingsDto> = newStore()) = DataStorePingSettingsRepository(store)

    @After
    fun tearDown() = scopes.forEach { it.cancel() }

    @Test
    fun emptyFileYieldsDefaults() =
        runBlocking<Unit> {
            assertThat(repository().settings.first()).isEqualTo(PingSettings.DEFAULT)
        }

    @Test
    fun addTargetAppendsUserTargetWithGeneratedId() =
        runBlocking<Unit> {
            val repo = repository()
            val added = repo.addTarget("example.org", 443)
            assertThat(added.id).startsWith("user:")
            assertThat(added.host).isEqualTo("example.org")
            assertThat(added.port).isEqualTo(443)
            assertThat(repo.settings.first().targets).isEqualTo(PingSettings.DEFAULT_TARGETS + added)
        }

    @Test
    fun duplicateHostAndPortReturnsExistingTargetIgnoringCase() =
        runBlocking<Unit> {
            val repo = repository()
            val first = repo.addTarget("Example.org", 443)
            val again = repo.addTarget("example.ORG", 443)
            val otherPort = repo.addTarget("example.org", null)
            assertThat(again).isEqualTo(first)
            assertThat(otherPort.id).isNotEqualTo(first.id)
            assertThat(repo.settings.first().targets).hasSize(PingSettings.DEFAULT_TARGETS.size + 2)
        }

    @Test
    fun addingADefaultHostDoesNotDuplicateIt() =
        runBlocking<Unit> {
            val repo = repository()
            val existing = repo.addTarget("GOOGLE.com", null)
            assertThat(existing.id).isEqualTo("default:google.com")
            assertThat(repo.settings.first()).isEqualTo(PingSettings.DEFAULT)
        }

    @Test
    fun removeTargetDropsItAndIgnoresUnknownIds() =
        runBlocking<Unit> {
            val repo = repository()
            repo.removeTarget("gateway")
            repo.removeTarget("default:8.8.8.8")
            repo.removeTarget("does-not-exist")
            val ids =
                repo.settings
                    .first()
                    .targets
                    .map { it.id }
            assertThat(ids).doesNotContain("gateway")
            assertThat(ids).doesNotContain("default:8.8.8.8")
            assertThat(ids).hasSize(PingSettings.DEFAULT_TARGETS.size - 2)
        }

    @Test
    fun setIntervalPersistsAllowedValuesAndRejectsOthers() =
        runBlocking<Unit> {
            val repo = repository()
            repo.setInterval(5_000)
            assertThat(repo.settings.first().intervalMs).isEqualTo(5_000)
            assertThrows(IllegalArgumentException::class.java) { runBlocking { repo.setInterval(100) } }
            assertThat(repo.settings.first().intervalMs).isEqualTo(5_000)
        }

    @Test
    fun restoreDefaultTargetsKeepsInterval() =
        runBlocking<Unit> {
            val repo = repository()
            repo.addTarget("example.org", null)
            repo.removeTarget("gateway")
            repo.setInterval(2_000)
            repo.restoreDefaultTargets()
            assertThat(repo.settings.first())
                .isEqualTo(PingSettings(PingSettings.DEFAULT_TARGETS, intervalMs = 2_000))
        }

    @Test
    fun settingsSurviveRestart() =
        runBlocking<Unit> {
            val firstScope = newScope()
            val first = repository(newStore(firstScope))
            val added = first.addTarget("example.org", 8080)
            first.setInterval(500)
            first.removeTarget("gateway")
            firstScope.coroutineContext[Job]!!.cancel()
            firstScope.coroutineContext[Job]!!.join()

            val restored = repository().settings.first()
            assertThat(restored.intervalMs).isEqualTo(500)
            assertThat(restored.targets).contains(added)
            assertThat(restored.targets).doesNotContain(PingTarget.Gateway)
        }

    @Test
    fun corruptFileFallsBackToDefaults() =
        runBlocking<Unit> {
            file.writeText("{ this is not json")
            assertThat(repository().settings.first()).isEqualTo(PingSettings.DEFAULT)
        }

    @Test
    fun unknownJsonKeysAreIgnored() =
        runBlocking<Unit> {
            file.writeText(
                """{"schemaVersion":2,"future":true,"intervalMs":2000,
                |"targets":[{"type":"gateway"},{"type":"host","id":"user:1","host":"a.b","port":null,"extra":1}]}
                """.trimMargin(),
            )
            val loaded = repository().settings.first()
            assertThat(loaded.intervalMs).isEqualTo(2_000)
            assertThat(
                loaded.targets,
            ).containsExactly(PingTarget.Gateway, PingTarget.Host("user:1", "a.b", null)).inOrder()
        }

    @Test
    fun unknownIntervalOnDiskFallsBackToDefaultInterval() =
        runBlocking<Unit> {
            file.writeText("""{"schemaVersion":1,"intervalMs":123,"targets":[{"type":"gateway"}]}""")
            val loaded = repository().settings.first()
            assertThat(loaded.intervalMs).isEqualTo(PingSettings.DEFAULT_INTERVAL_MS)
            assertThat(loaded.targets).containsExactly(PingTarget.Gateway)
        }
}
