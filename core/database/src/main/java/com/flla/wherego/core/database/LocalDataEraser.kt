package com.flla.wherego.core.database

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface LocalDataEraser {
    /**
     * Drops every Room row, deletes every stored receipt JPEG, then reseeds the
     * guest profile and preset categories so the device matches a fresh install.
     */
    suspend fun resetToGuest()
}

@Singleton
class RoomLocalDataEraser @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val db: WheregoDatabase,
    private val profiles: UserProfileStore,
    private val ledger: LedgerStore,
) : LocalDataEraser {
    override suspend fun resetToGuest() {
        withContext(Dispatchers.IO) {
            db.clearAllTables()
            ReceiptFiles.dir(context).deleteRecursively()
        }
        profiles.ensureGuest()
        ledger.seedCategoriesIfEmpty()
    }
}
