package io.github.daredevilq.wistride.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStorePermissionRequestHistoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun newStore(scope: CoroutineScope): DataStore<Preferences> =
        permissionRequestsStore(File(folder.root, "permission_requests.preferences_pb"), scope)

    @Test
    fun startsEmptyAndRecordsIds() =
        runBlocking<Unit> {
            val job = Job()
            val history = DataStorePermissionRequestHistory(newStore(CoroutineScope(Dispatchers.IO + job)))
            assertThat(history.wasRequested("wifi_details")).isFalse()
            history.markRequested("wifi_details")
            history.markRequested("wifi_details")
            assertThat(history.wasRequested("wifi_details")).isTrue()
            assertThat(history.wasRequested("other")).isFalse()
            job.cancel()
        }

    @Test
    fun historyPersistsAcrossInstances() =
        runBlocking<Unit> {
            val firstJob = Job()
            val first = DataStorePermissionRequestHistory(newStore(CoroutineScope(Dispatchers.IO + firstJob)))
            first.markRequested("a")
            first.markRequested("b")
            firstJob.cancel()
            firstJob.join()

            val secondJob = Job()
            val second = DataStorePermissionRequestHistory(newStore(CoroutineScope(Dispatchers.IO + secondJob)))
            assertThat(second.wasRequested("a")).isTrue()
            assertThat(second.wasRequested("b")).isTrue()
            assertThat(second.wasRequested("c")).isFalse()
            secondJob.cancel()
        }
}
