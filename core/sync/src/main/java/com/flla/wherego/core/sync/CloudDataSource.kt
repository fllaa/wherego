package com.flla.wherego.core.sync

import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.UserProfile

interface CloudDataSource {
    val available: Boolean

    suspend fun pushTransactions(uid: String, rows: List<Transaction>)
    suspend fun pullTransactions(uid: String, sinceEpoch: Long): List<Transaction>

    suspend fun pushCategories(uid: String, rows: List<Category>)
    suspend fun pullCategories(uid: String, sinceEpoch: Long): List<Category>

    suspend fun pushProfile(uid: String, profile: UserProfile)
    suspend fun pullProfile(uid: String, sinceEpoch: Long): UserProfile?

    /** Removes every document under `users/{uid}/`. */
    suspend fun deleteAll(uid: String)
}
