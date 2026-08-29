package app.wherego.core.sync

import app.wherego.core.model.Category
import app.wherego.core.model.PresetCategories
import app.wherego.core.model.Transaction
import app.wherego.core.model.UserProfile
import org.json.JSONObject

object CloudCodec {
    fun transaction(row: Transaction): JSONObject = JSONObject()
        .put("id", row.id)
        .put("kind", row.kind)
        .put("amountMinor", row.amountMinor)
        .put("currency", row.currency)
        .put("fxRateToBase", row.fxRateToBase)
        .put("amountBaseMinor", row.amountBaseMinor)
        .put("categoryId", row.categoryId)
        .put("note", row.note)
        .put("occurredOn", row.occurredOn)
        .put("occurredAt", row.occurredAt ?: JSONObject.NULL)
        .put("recurringId", row.recurringId ?: JSONObject.NULL)
        .put("receiptId", row.receiptId ?: JSONObject.NULL)
        .put("createdAt", row.createdAt)
        .put("updatedAt", row.updatedAt)
        .put("deletedAt", row.deletedAt ?: JSONObject.NULL)
        .put("dirty", false)

    fun transaction(json: JSONObject): Transaction = Transaction(
        id = json.getString("id"),
        kind = json.getString("kind"),
        amountMinor = json.getLong("amountMinor"),
        currency = json.getString("currency"),
        fxRateToBase = json.getString("fxRateToBase"),
        amountBaseMinor = json.getLong("amountBaseMinor"),
        categoryId = json.getString("categoryId"),
        note = json.optString("note"),
        occurredOn = json.getString("occurredOn"),
        occurredAt = json.optionalLong("occurredAt"),
        recurringId = json.optionalString("recurringId"),
        receiptId = json.optionalString("receiptId"),
        createdAt = json.getLong("createdAt"),
        updatedAt = json.getLong("updatedAt"),
        deletedAt = json.optionalLong("deletedAt"),
        dirty = false,
    )

    fun category(row: Category): JSONObject = JSONObject()
        .put("id", row.id)
        .put("name", row.name)
        .put("emoji", row.emoji)
        .put("colorHex", row.colorHex)
        .put("kind", row.kind)
        .put("isPreset", row.isPreset)
        .put("archived", row.archived)
        .put("sortOrder", row.sortOrder)
        .put("updatedAt", row.updatedAt)
        .put("deletedAt", row.deletedAt ?: JSONObject.NULL)

    fun category(json: JSONObject): Category {
        val id = json.getString("id")
        return Category(
            id = id,
            name = json.getString("name"),
            emoji = json.getString("emoji"),
            colorHex = json.getString("colorHex"),
            softColorHex = PresetCategories.softHex(id),
            kind = json.getString("kind"),
            isPreset = json.getBoolean("isPreset"),
            archived = json.getBoolean("archived"),
            sortOrder = json.getInt("sortOrder"),
            updatedAt = json.getLong("updatedAt"),
            deletedAt = json.optionalLong("deletedAt"),
        )
    }

    fun profile(row: UserProfile): JSONObject = JSONObject()
        .put("id", row.id)
        .put("googleSub", row.googleSub ?: JSONObject.NULL)
        .put("email", row.email ?: JSONObject.NULL)
        .put("displayName", row.displayName ?: JSONObject.NULL)
        .put("photoUrl", row.photoUrl ?: JSONObject.NULL)
        .put("baseCurrency", row.baseCurrency)
        .put("localeTag", row.localeTag)
        .put("timeZoneId", row.timeZoneId)
        .put("onboardingDone", row.onboardingDone)
        .put("startingBalanceMinor", row.startingBalanceMinor)
        .put("startingBalanceOn", row.startingBalanceOn ?: JSONObject.NULL)
        .put("createdAt", row.createdAt)
        .put("updatedAt", row.updatedAt)

    fun profile(json: JSONObject): UserProfile = UserProfile(
        id = json.getString("id"),
        googleSub = json.optionalString("googleSub"),
        email = json.optionalString("email"),
        displayName = json.optionalString("displayName"),
        photoUrl = json.optionalString("photoUrl"),
        baseCurrency = json.getString("baseCurrency"),
        localeTag = json.getString("localeTag"),
        timeZoneId = json.getString("timeZoneId"),
        onboardingDone = json.getBoolean("onboardingDone"),
        startingBalanceMinor = json.getLong("startingBalanceMinor"),
        startingBalanceOn = json.optionalString("startingBalanceOn"),
        createdAt = json.getLong("createdAt"),
        updatedAt = json.getLong("updatedAt"),
    )

    private fun JSONObject.optionalLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return getLong(key)
    }

    private fun JSONObject.optionalString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = getString(key)
        return value.ifBlank { null }
    }
}
