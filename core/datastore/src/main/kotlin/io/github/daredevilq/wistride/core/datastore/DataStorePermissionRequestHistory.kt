package io.github.daredevilq.wistride.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.daredevilq.wistride.core.model.permission.PermissionRequestHistory
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** [PermissionRequestHistory] persisted as one string set of requirement ids. */
class DataStorePermissionRequestHistory
    @Inject
    constructor(
        private val store: DataStore<Preferences>,
    ) : PermissionRequestHistory {
        override suspend fun wasRequested(id: String): Boolean = id in store.data.first()[REQUESTED].orEmpty()

        override suspend fun markRequested(id: String) {
            store.edit { it[REQUESTED] = it[REQUESTED].orEmpty() + id }
        }

        private companion object {
            val REQUESTED = stringSetPreferencesKey("requested_ids")
        }
    }
