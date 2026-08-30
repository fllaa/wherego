package com.flla.wherego.core.sync

import android.content.Context
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

@Singleton
class FakeCloudDataSource @Inject constructor(
    @ApplicationContext context: Context,
) : CloudDataSource {
    private val root = File(context.filesDir, "wherego-cloud")

    override val available: Boolean = true

    override suspend fun pushTransactions(uid: String, rows: List<Transaction>) {
        rows.forEach { write(uid, "transactions", it.id, CloudCodec.transaction(it)) }
    }

    override suspend fun pullTransactions(uid: String, sinceEpoch: Long): List<Transaction> =
        readAll(uid, "transactions").map(CloudCodec::transaction).filter { it.updatedAt > sinceEpoch }

    override suspend fun pushCategories(uid: String, rows: List<Category>) {
        rows.forEach { write(uid, "categories", it.id, CloudCodec.category(it)) }
    }

    override suspend fun pullCategories(uid: String, sinceEpoch: Long): List<Category> =
        readAll(uid, "categories").map(CloudCodec::category).filter { it.updatedAt > sinceEpoch }

    override suspend fun pushProfile(uid: String, profile: UserProfile) {
        write(uid, "profile", "profile", CloudCodec.profile(profile))
    }

    override suspend fun pullProfile(uid: String, sinceEpoch: Long): UserProfile? {
        val json = read(uid, "profile", "profile") ?: return null
        val profile = CloudCodec.profile(json)
        return profile.takeIf { it.updatedAt > sinceEpoch }
    }

    override suspend fun deleteAll(uid: String) {
        File(root, uid).deleteRecursively()
    }

    private fun dir(uid: String, collection: String): File =
        File(root, "$uid/$collection").also { it.mkdirs() }

    private fun write(uid: String, collection: String, id: String, json: JSONObject) {
        File(dir(uid, collection), "$id.json").writeText(json.toString())
    }

    private fun read(uid: String, collection: String, id: String): JSONObject? {
        val file = File(dir(uid, collection), "$id.json")
        if (!file.exists()) return null
        return JSONObject(file.readText())
    }

    private fun readAll(uid: String, collection: String): List<JSONObject> {
        val folder = dir(uid, collection)
        return folder.listFiles { f -> f.extension == "json" }
            ?.map { JSONObject(it.readText()) }
            .orEmpty()
    }
}
